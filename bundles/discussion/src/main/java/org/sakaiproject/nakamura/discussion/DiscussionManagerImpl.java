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
package org.sakaiproject.nakamura.discussion;

import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.discussion.LiteDiscussionManager;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for the discussions.
 */
@Component(immediate = true, label = "%discussion.manager.label", description = "%discussion.manager.desc")
@Service
public class DiscussionManagerImpl implements LiteDiscussionManager {

  public static final Logger LOG = LoggerFactory.getLogger(DiscussionManagerImpl.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  private Pattern homePathPattern = Pattern.compile("^(.*)(~([\\w-]*?))/");

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.discussion.DiscussionManager#findMessage(java.lang.String,
   *      java.lang.String, javax.jcr.Session, java.lang.String)
   */
  public Content findMessage(String messageId, String marker, Session session, String path)
      throws MessagingException {

    if (path == null) {
      path = "/";
    }
    if (!path.startsWith("/")) {
      throw new MessagingException(500,
          "Path should be an absolute path starting with a '/'");
    }
    if (path.endsWith("/")) {
      path = path.substring(0, path.length());
    }
    try {
      ContentManager cm = session.getContentManager();
      Map<String, Object> props = Maps.newHashMap();
      props.put("sling:resourceType", "sakai/message");
      props.put("sakai:type", "discussion");
      props.put("sakai:id", messageId);
      props.put("sakai:marker", marker);

      path = expandHomeDirectory(path);

      Iterator<Content> foundContent = cm.find(props).iterator();

      while (foundContent.hasNext()) {
        Content c = foundContent.next();
        if (c.getPath().startsWith(path)) {
          return c;
        }
      }
    } catch (StorageClientException e) {
      LOG.warn("Unable to check for message with ID '{}' and marker '{}'", messageId,
          marker);
    } catch (AccessDeniedException e) {
      LOG.warn("Unable to check for message with ID '{}' and marker '{}'", messageId,
          marker);
    }

    LOG.warn("No message with ID '{}' and marker '{}' found.", messageId, marker);
    return null;
  }

  /**
   * 
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.discussion.DiscussionManager#findSettings(java.lang.String, javax.jcr.Session, java.lang.String)
   */
  public Content findSettings(String marker, Session session, String type) {
    if (type == null || "".equals(type)) {
      type = "discussion";
    }
    try {
      ContentManager cm = session.getContentManager();
      Map<String, Object> props = Maps.newHashMap();
      props.put("sling:resourceType", "sakai/settings");
      props.put("sakai:type", type);
      props.put("sakai:marker", marker);

      Iterator<Content> foundContent = cm.find(props).iterator();

      if (foundContent.hasNext()) {
        Content c = foundContent.next();
        return c;
      }
    } catch (StorageClientException e) {
      LOG.warn("Unable to check for settings of type '{}' and marker '{}'", type, marker);
    } catch (AccessDeniedException e) {
      LOG.warn("Unable to check for settings of type '{}' and marker '{}'", type, marker);
    }

    LOG.debug("No settings with type '{}' and marker '{}' found.", type, marker);
    return null;
  }

  private String expandHomeDirectory(String queryString) {
    Matcher homePathMatcher = homePathPattern.matcher(queryString);
    if (homePathMatcher.find()) {
      String username = homePathMatcher.group(3);
      String homePrefix = homePathMatcher.group(1);
      String userHome = LitePersonalUtils.getHomePath(username);
      userHome = ClientUtils.escapeQueryChars(userHome);
      String homePath = homePrefix + userHome + "/";
      String prefix = "";
      if (homePathMatcher.start() > 0) {
        prefix = queryString.substring(0, homePathMatcher.start());
      }
      String suffix = queryString.substring(homePathMatcher.end());
      queryString = prefix + homePath + suffix;
    }
    return queryString;
  }
}
