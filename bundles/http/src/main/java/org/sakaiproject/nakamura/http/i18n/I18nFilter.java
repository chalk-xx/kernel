/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.http.i18n;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Filter to transform __MSG_*__ i18n message keys into i18n messages.
 */
@Component(metatype = true)
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Nakamura Cache-Control Filter"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 10, propertyPrivate = true),
    @Property(name = "sling.filter.scope", value = "REQUEST", propertyPrivate = true),
    @Property(name = I18nFilter.FILTERED_PATTERN, value = I18nFilter.DEFAULT_FILTERED_PATTERN),
    @Property(name = I18nFilter.BUNDLES_PATH, value = I18nFilter.DEFAULT_BUNDLES_PATH),
    @Property(name = I18nFilter.MESSAGE_KEY_PREFIX_PATTERN, value = I18nFilter.DEFAULT_MESSAGE_KEY_PREFIX),
    @Property(name = I18nFilter.MESSAGE_KEY_PATTERN, value = I18nFilter.DEFAULT_MESSAGE_KEY_PATTERN),
    @Property(name = I18nFilter.MESSAGE_KEY_SUFFIX_PATTERN, value = I18nFilter.DEFAULT_MESSAGE_KEY_SUFFIX),
    @Property(name = I18nFilter.SHOW_MISSING_KEYS, boolValue = I18nFilter.DEFAULT_SHOW_MISSING_KEYS)
})
public class I18nFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(I18nFilter.class);

  static final String FILTERED_PATTERN = "sakai.filter.i18n.pattern";
  static final String BUNDLES_PATH = "sakai.filter.i18n.bundles.path";
  static final String MESSAGE_KEY_PREFIX_PATTERN = "sakai.filter.i18n.message_key.prefix";
  static final String MESSAGE_KEY_PATTERN = "sakai.filter.i18n.message_key.pattern";
  static final String MESSAGE_KEY_SUFFIX_PATTERN = "sakai.filter.i18n.message_key.suffix";
  static final String SHOW_MISSING_KEYS = "sakai.filter.i18n.message_key.show_missing";

//  public static final String[] DEFAULT_FILTERED_PATTERNS = new String[] {
//      "^/dev/.+html$", "/devwidgets/.+html$", "^/dev/$" };
  public static final String DEFAULT_FILTERED_PATTERN = "^/(dev|devwidgets)(/|/.+\\.html)*$";
  public static final String DEFAULT_BUNDLES_PATH = "/dev/_bundle";
  public static final String DEFAULT_MESSAGE_KEY_PREFIX = "__MSG__";
  public static final String DEFAULT_MESSAGE_KEY_PATTERN = ".+";
  public static final String DEFAULT_MESSAGE_KEY_SUFFIX = "__";
  public static final boolean DEFAULT_SHOW_MISSING_KEYS = true;

  private Pattern filteredPattern;
  private String bundlesPath;
  private String keyPrefix;
  private String keyPattern;
  private String keySuffix;
  private Pattern messageKeyPattern;
  private boolean showMissingKeys;

  @Activate @Modified
  public void modified(Map<?, ?> props) {
    String pattern = OsgiUtil.toString(props.get(FILTERED_PATTERN),
        DEFAULT_FILTERED_PATTERN);
    filteredPattern = Pattern.compile(pattern);
//    pathPatterns = new Pattern[spatterns.length];
//    for (int i = 0; i < spatterns.length; i++) {
//      pathPatterns[i] = Pattern.compile(spatterns[i]);
//    }

    bundlesPath = OsgiUtil.toString(props.get(BUNDLES_PATH), DEFAULT_BUNDLES_PATH);

    keyPrefix = OsgiUtil.toString(props.get(MESSAGE_KEY_PREFIX_PATTERN),
        DEFAULT_MESSAGE_KEY_PREFIX);
    keyPattern = OsgiUtil.toString(props.get(MESSAGE_KEY_PATTERN),
        DEFAULT_MESSAGE_KEY_PATTERN);
    keySuffix = OsgiUtil.toString(props.get(MESSAGE_KEY_SUFFIX_PATTERN),
        DEFAULT_MESSAGE_KEY_SUFFIX);
    messageKeyPattern = Pattern.compile(keyPrefix + "(" + keyPattern + ")" + keySuffix);

    showMissingKeys = OsgiUtil.toBoolean(props.get(SHOW_MISSING_KEYS), DEFAULT_SHOW_MISSING_KEYS);
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    SlingHttpServletRequest srequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse sresponse = (SlingHttpServletResponse) response;

    String path = srequest.getPathInfo();

    // check that the path is something we should filter.
    boolean filter = false;
    if (path != null && filteredPattern.matcher(path).matches()) {
      response = new I18nFilterServletResponse(sresponse);
      filter = true;
    }

    // allow the chain to process so we can capture the response
    chain.doFilter(request, response);
    String output = response.toString();

    // if the path was set to be filtered, get the output and filter it
    if (filter && output != null && output.length() > 0) {
      writeFilteredResponse(srequest, sresponse, output);
    }
  }

  /**
   * Filter <code>output</code> of any message keys by replacing them with the matching
   * message from the language bundle associated to the user.
   *
   * @param srequest
   * @param response
   * @param output
   * @throws IOException
   */
  private void writeFilteredResponse(SlingHttpServletRequest srequest,
      SlingHttpServletResponse response, String output) throws IOException {
    StringBuilder sb = new StringBuilder(output);
    try {
      Session session = srequest.getResourceResolver().adaptTo(Session.class);
      Node bundlesNode = session.getNode(bundlesPath);

      // get user's locale
      Locale locale = getLocale(session);

      // load the language bundle
      JSONObject langJson = getJsonBundle(bundlesNode, locale.toString() + ".json");

      // load the default bundle
      JSONObject defaultJson = getJsonBundle(bundlesNode, "default.json");

      // check for message keys and replace them with the appropriate message
      Matcher m = messageKeyPattern.matcher(output);
      ArrayList<String> matchedKeys = new ArrayList<String>();
      while (m.find()) {
        String msgKey = m.group(0);
        String key = m.group(1);
        if (!matchedKeys.contains(key)) {
          String message = "";

          if (langJson.has(key)) {
            message = langJson.getString(key);
          } else if (defaultJson.has(key)) {
            message = defaultJson.getString(key);
          } else {
            String msg = "Message key not found [" + key + "]";
            logger.warn(msg);
            if (showMissingKeys) {
              message = msg;
            }
          }

          // replace all instances of msgKey with the actual message
          int keyStart = sb.indexOf(msgKey);
          while (keyStart >= 0) {
            sb.replace(keyStart, keyStart + msgKey.length(), message);
            keyStart = sb.indexOf(msgKey);
          }

          // track the group so we don't try to replace it again
          matchedKeys.add(key);
        }
      }
    } catch (RepositoryException e) {
      logger.error(e.getMessage(), e);
    } catch (JSONException e) {
      logger.error(e.getMessage(), e);
    }

    // send the output to the actual response
    try {
      response.getWriter().write(sb.toString());
    } catch (IllegalStateException e) {
      response.getOutputStream().write(sb.toString().getBytes("UTF-8"));
    }
  }

  private Locale getLocale(Session session) throws RepositoryException {
    // get locale from authorizable
    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable au = um.getAuthorizable(session.getUserID());
    Locale l = Locale.getDefault();
//    if (au.getPropertyNames().containsKey("locale")) {
//      String locale[] = au.getProperty("locale").toString().split("_");
//      if (locale.length == 2) {
//        l = new Locale(locale[0], locale[1]);
//      }
//    }
    return l;
  }

  private JSONObject getJsonBundle(Node bundlesNode, String name)
      throws PathNotFoundException, RepositoryException, ValueFormatException,
      JSONException {
    Node langNode = bundlesNode.getNode(name);
    Node content = langNode.getNode("jcr:content");
    String langData = content.getProperty("jcr:data").getString();
    JSONObject langJson = new JSONObject(langData);
    return langJson;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
  }

  /**
   * Response wrapper to filter i18n keys into language messages.
   */
  private static final class I18nFilterServletResponse extends HttpServletResponseWrapper {
    private final CharArrayWriter caw;
    private final ByteArrayOutputStream baos;

    public I18nFilterServletResponse(HttpServletResponse response) {
      super(response);
      caw = new CharArrayWriter();
      baos = new ByteArrayOutputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
      // calling super will throw an exception if that's the right thing to do
      super.getOutputStream();

      // we've passed the super call, return a stream
      return new ServletOutputStream() {

        @Override
        public void write(int b) throws IOException {
          baos.write(b);
        }
      };
    }

    @Override
    public PrintWriter getWriter() throws IOException{
      // calling super will throw an exception if that's the right thing to do
      super.getWriter();

      // we've passed the super call, return a print writer
      return new PrintWriter(caw);
    }

    @Override
    public String toString() {
      String retval = null;
      if (baos.size() > 0) {
        try {
          retval = baos.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
          // let retval be null
        }
      } else if (caw.size() > 0) {
        retval = caw.toString();
      }
      return retval;
    }
  }
}
