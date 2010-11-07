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
package org.sakaiproject.nakamura.webapp.filter;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
    @Property(name = "filter.scope", value = "request", propertyPrivate = true),
    @Property(name = "filter.order", intValue = 10, propertyPrivate = true),
//    @Property(name = I18nFilter.FILTERED_PATTERNS, value = { "^/dev/.+\\.html$", "/devwidgets/.+\\.html$" }),
    @Property(name = I18nFilter.BUNDLES_PATH, value = I18nFilter.DEFAULT_BUNDLES_PATH),
    @Property(name = I18nFilter.MESSAGE_KEY_PATTERN, value = I18nFilter.DEFAULT_MESSAGE_KEY_PATTERN)
})
public class I18nFilter implements Filter {
  private static final Logger logger = LoggerFactory.getLogger(I18nFilter.class);

  static final String FILTERED_PATTERNS = "sakai.filter.i18n.paths";
  static final String BUNDLES_PATH = "sakai.filter.i18n.bundles.path";
  static final String MESSAGE_KEY_PATTERN = "sakai.filter.i18n.message_key.pattern";

  public static final String DEFAULT_BUNDLES_PATH = "/dev/_bundle";
  public static final String DEFAULT_MESSAGE_KEY_PATTERN = "__MSG__(.+)__";

  private Pattern[] pathPatterns;
  private String bundlesPath;
  private Pattern messageKeyPattern;

  @Activate @Modified
  public void modified(Map<?, ?> props) {
    String[] spatterns = OsgiUtil.toStringArray(props.get(FILTERED_PATTERNS),
        new String[] { "^/dev/.+\\.html$", "/devwidgets/.+\\.html$" });
    pathPatterns = new Pattern[spatterns.length];
    for (int i = 0; i < spatterns.length; i++) {
      pathPatterns[i] = Pattern.compile(spatterns[i]);
    }

    bundlesPath = OsgiUtil.toString(props.get(BUNDLES_PATH), DEFAULT_BUNDLES_PATH);

    messageKeyPattern = Pattern.compile(OsgiUtil.toString(props.get(MESSAGE_KEY_PATTERN),
        DEFAULT_MESSAGE_KEY_PATTERN));
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
    if (path != null) {
      for (Pattern pattern : pathPatterns) {
        if (pattern.matcher(path).matches()) {
          response = new I18nFilterServletResponse(sresponse);
          filter = true;
          break;
        }
      }
    }

    // allow the chain to process so we can capture the response
    chain.doFilter(request, response);
    String output = response.toString();

    // if the path was set to be filtered, get the output and filter it
    if (filter) {
      writeFilteredResposne(srequest, sresponse, output);
    }
  }

  private void writeFilteredResposne(SlingHttpServletRequest srequest,
      SlingHttpServletResponse response, String output) throws IOException {
    try {
      Session session = srequest.getResourceResolver().adaptTo(Session.class);
      Node bundlesNode = session.getNode(bundlesPath);

      // get user's locale
      String locale = getLocale(srequest);

      // load the language bundle
      JSONObject langJson = getJsonBundle(bundlesNode, locale);

      // load the default bundle
      JSONObject defaultJson = getJsonBundle(bundlesNode, "default.json");

      // check for message keys and replace them with the appropriate message
      Matcher m = messageKeyPattern.matcher(output);
      ArrayList<String> matchedKeys = new ArrayList<String>();
      while (m.find()) {
        String group = m.group();
//        for (int i = 0; i < m.groupCount(); i++) {
//          String group = m.group(i);
          if (!matchedKeys.contains(group)) {
            String message = "Message key not found [" + group + "]";

            if (langJson.has(group)) {
              message = langJson.getString(group);
            } else if (defaultJson.has(group)) {
              message = defaultJson.getString(group);
            }
            output.replace("__MSG__" + group + "__", message);

            // track the group so we don't try to replace it again
            matchedKeys.add(group);
          }
//        }
      }
    } catch (RepositoryException e) {
      logger.error(e.getMessage(), e);
    } catch (JSONException e) {
      logger.error(e.getMessage(), e);
    }

    // send the output to the actual response
    response.getWriter().write(output);
  }

  private String getLocale(SlingHttpServletRequest request) {
    // TODO get locale from authorizable
    return "en_US.json";
  }

  private JSONObject getJsonBundle(Node bundlesNode, String locale)
      throws PathNotFoundException, RepositoryException, ValueFormatException,
      JSONException {
    Node langNode = bundlesNode.getNode(locale);
    String langData = langNode.getProperty("jcr:data").getString();
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
    private CharArrayWriter caw;
    public I18nFilterServletResponse(HttpServletResponse response) {
      super(response);
      caw = new CharArrayWriter();
    }

    @Override
    public PrintWriter getWriter() {
      return new PrintWriter(caw);
    }

    @Override
    public String toString() {
      return caw.toString();
    }
  }
}
