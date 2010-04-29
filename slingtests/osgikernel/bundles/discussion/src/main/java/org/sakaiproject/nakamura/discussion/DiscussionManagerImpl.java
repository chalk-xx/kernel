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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Manager for the discussions.
 */
@Component(immediate = true, label = "%discussion.manager.label", description = "%discussion.manager.desc")
@Service
public class DiscussionManagerImpl implements DiscussionManager {

  public static final Logger LOG = LoggerFactory.getLogger(DiscussionManagerImpl.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.discussion.DiscussionManager#findMessage(java.lang.String,
   *      java.lang.String, javax.jcr.Session, java.lang.String)
   */
  public Node findMessage(String messageId, String marker, Session session, String path)
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

    String queryString = "/"
        + path
        + "//*[@sling:resourceType=\"sakai/message\" and @sakai:type='discussion' and @sakai:id='"
        + messageId + "' and @sakai:marker='" + marker + "']";
    LOG.info("Trying to find message with query: {}", queryString);
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator nodeIterator = result.getNodes();

      while (nodeIterator.hasNext()) {
        Node n = nodeIterator.nextNode();
        return n;
      }

    } catch (RepositoryException e) {
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
  public Node findSettings(String marker, Session session, String type) {
    if (type == null || "".equals(type)) {
      type = "discussion";
    }
    String queryString = "//*[@sling:resourceType=\"sakai/settings\" and @sakai:type='"
        + type + "' and @sakai:marker='" + marker + "']";
    LOG.info("Trying to find settings with query: {}", queryString);
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator nodeIterator = result.getNodes();

      while (nodeIterator.hasNext()) {
        Node n = nodeIterator.nextNode();
        return n;
      }

    } catch (RepositoryException e) {
      LOG.warn("Unable to check for settings of type '{}' and marker '{}'", type, marker);
    }

    LOG.warn("No settings with type '{}' and marker '{}' found.", type, marker);

    return null;
  }
}
