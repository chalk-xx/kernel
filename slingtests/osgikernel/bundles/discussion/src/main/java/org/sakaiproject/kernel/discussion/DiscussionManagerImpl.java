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
package org.sakaiproject.kernel.discussion;

import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.message.MessagingException;
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
 * 
 * @scr.component immediate="true" label="Sakai Discussion Manager"
 *                description="Service for doing operations with discussions." name
 *                ="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionManagerImpl implements DiscussionManager {

  public static final Logger LOG = LoggerFactory.getLogger(DiscussionManagerImpl.class);

  public String findStoreForMessage(String messageid, Session session) throws MessagingException {

    String queryString = "//*[@sling:resourceType=\"sakai/message\" and @sakai:type='discussion' and @sakai:id='"
        + messageid + "']";
    LOG.info("Trying to find message with query: {}", queryString);
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator nodeIterator = result.getNodes();

      if (nodeIterator.getSize() == 0) {
        LOG.warn("No message with ID '{}' found.", messageid);
        throw new MessagingException(404, "No message with that ID found.");
      }

      while (nodeIterator.hasNext()) {
        Node n = nodeIterator.nextNode();
        if (n.hasProperty(DiscussionConstants.PROP_SAKAI_WRITETO)) {
          // This node has a property related to the store. use this one.
          return n.getProperty(DiscussionConstants.PROP_SAKAI_WRITETO).getString();
        }
      }
      // Nothing found .. throw an exception.

    } catch (RepositoryException e) {
      LOG.warn("Unable to check for store for {}", messageid);
      e.printStackTrace();
    }

    throw new MessagingException(500, "Unable to get messagestore.");
  }
}
