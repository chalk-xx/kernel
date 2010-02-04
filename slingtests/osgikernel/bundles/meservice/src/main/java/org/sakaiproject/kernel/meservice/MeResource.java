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
package org.sakaiproject.kernel.meservice;

import net.sf.json.JSONObject;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.personal.PersonalConstants;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.user.UserConstants;
import org.sakaiproject.kernel.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;

public class MeResource implements Resource {

  private static final Logger LOG = LoggerFactory.getLogger(MeResource.class);
  private static final String LOCALE_FIELD = "locale";
  private static final String TIMEZONE_FIELD = "timezone";
  private ResourceResolver resourceResolver;
  private String resourceType;
  private String path;
  private ResourceMetadata resourceMetadata;
  private PrincipalManager principalManager;
  private Authorizable authorizable;
  private Session session;
  private Set<String> subjects;
  private JsonResourceWriter itemWriter;

  public MeResource(ResourceResolver resolver, String path, String resourceType) throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    this.resourceResolver = resolver;
    this.session = this.resourceResolver.adaptTo(Session.class);
    this.path = path;
    this.resourceType = resourceType;
    this.resourceMetadata = new ResourceMetadata();
    this.resourceMetadata.setResolutionPath(path);
    this.principalManager = AccessControlUtil.getPrincipalManager(session);
    if (!isAnonymous()) {
      this.authorizable = AccessControlUtil.getUserManager(session).getAuthorizable(
          session.getUserID());
    }
    this.itemWriter = new JsonResourceWriter(null);
  }

  public String getPath() {
    return path;
  }

  public ResourceMetadata getResourceMetadata() {
    return resourceMetadata;
  }

  public ResourceResolver getResourceResolver() {
    return resourceResolver;
  }

  public String getResourceSuperType() {
    return null;
  }

  public String getResourceType() {
    return resourceType;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getProperties() throws RepositoryException {
    Map<String, Object> result = new HashMap<String, Object>();
    if (authorizable != null) {
      for (Iterator<String> it = authorizable.getPropertyNames(); it.hasNext();) {
        String propName = it.next();
        if (propName.startsWith("rep:"))
          continue;
        Value[] values = authorizable.getProperty(propName);
        switch (values.length) {
        case 0:
          continue;
        case 1:
          result.put(propName, values[0].getString());
          break;
        default: {
          StringBuilder valueString = new StringBuilder("");
          for (int i = 0; i < values.length; i++) {
            valueString.append("," + values[i].getString());
          }
          result.put(propName, valueString.toString().substring(1));
        }
        }
      }
    }
    return result;
  }

  private Set<String> getSubjects() throws RepositoryException {
    if (subjects == null) {
      subjects = new HashSet<String>();
      if (authorizable != null) {
        Principal principal = authorizable.getPrincipal();
        if (principal != null) {
          PrincipalIterator it = principalManager.getGroupMembership(principal);
          while (it.hasNext()) {
            subjects.add(it.nextPrincipal().getName());
          }
        }
      }
    }
    return subjects;
  }

  private boolean isAnonymous() {
    return "anonymous".equals(session.getUserID());
  }

  private String getUserJSON() throws RepositoryException {
    Map<String, Object> json = new HashMap<String, Object>();
    if (isAnonymous()) {
      json.put("anon", true);
      json.put("subjects", new ArrayList<String>());
      json.put("superUser", false);
    } else {
      json.put("userid", session.getUserID());
      json.put("properties", getProperties());
      json.put("subjects", getSubjects());
      json.put("userStoragePrefix", PathUtils.getHashedPath(session.getUserID(), UserConstants.DEFAULT_HASH_LEVELS).substring(1));
      json.put("userProfilePath", PersonalConstants._USER_PUBLIC+"/"+session.getUserID()+"/"+PersonalConstants.AUTH_PROFILE);
      json.put("superUser", getSubjects().contains("administrators"));
      
      HashMap<String, Object> localeMap = new HashMap<String, Object>();
      
      /* Get the correct locale */
      
      Locale l = Locale.getDefault();
      if (getProperties().containsKey(LOCALE_FIELD)){
    	  String locale[] = getProperties().get(LOCALE_FIELD).toString().split("_");
    	  if (locale.length == 2) {
    	    l = new Locale(locale[0],locale[1]);
    	  }
      }
      
      /* Get the correct time zone */
      
      TimeZone tz = TimeZone.getDefault();
      if (getProperties().containsKey(TIMEZONE_FIELD)){
    	  String timezone = getProperties().get(TIMEZONE_FIELD).toString();
    	  tz = TimeZone.getTimeZone(timezone);
      }
      
      /* Add the locale information into the output */
      
      localeMap.put("country", l.getCountry());
      localeMap.put("displayCountry", l.getDisplayCountry(l));
      localeMap.put("displayLanguage", l.getDisplayLanguage(l));
      localeMap.put("displayName", l.getDisplayName(l));
      localeMap.put("displayVariant", l.getDisplayVariant(l));
      localeMap.put("ISO3Country", l.getISO3Country());
      localeMap.put("ISO3Language", l.getISO3Language());
      localeMap.put("language", l.getLanguage());
      localeMap.put("variant", l.getVariant());
      
      /* Add the time zone information into the output */
      
      int offset = tz.getRawOffset() + tz.getDSTSavings();
      Map<String, Object> timezoneMap = new HashMap<String, Object>();
      timezoneMap.put("name", tz.getID());
      timezoneMap.put("GMT", offset / 3600000);
      localeMap.put("timezone", timezoneMap);
      
      json.put("locale", localeMap);
      
    }
    return JSONObject.fromObject(json).toString();
  }

  @SuppressWarnings("unchecked")
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    LOG.info("Adapting meresource to " + type);
    if (type == InputStream.class) {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new PrintWriter(baos);
        writer.append("{\"user\": ");
        writer.append(getUserJSON());
        sendNodeAsJson("profile", PersonalUtils.getProfilePath(session.getUserID()),
            writer);
        writer.append("}");
        writer.close();

        return (AdapterType) new ByteArrayInputStream(baos.toString().getBytes("utf-8"));
      } catch (RepositoryException e) {
        LOG.error("Unable to read user details", e);
      } catch (IOException e) {
        LOG.error("IOError reading user details", e);
      }
    }
    return null;
  }

  /**
   * @param key
   * @param path
   * @param writer
   * @throws IOException
   * @throws RepositoryException
   */
  private void sendNodeAsJson(String key, String path, Writer writer) throws IOException,
      RepositoryException {
    writer.append(", \"" + key + "\" : ");
    try {
      Resource res = resourceResolver.getResource(path);
      if (res == null) {
        LOG.info("No profile found at " + path);
        writer.append("{}");
      } else {
        itemWriter.dump(res, writer, 0);
      }
    } catch (Exception ex) {
      LOG.info("Failed to load profile", ex);
      writer.append("{}");
    }
  }
}
