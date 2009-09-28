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
package org.sakaiproject.kernel.chat;

import org.apache.commons.lang.time.DateUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.kernel.api.jcr.JCRConstants;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

@Component(metatype=false, immediate=true)
@Properties(value = {
    @Property(name = Scheduler.PROPERTY_SCHEDULER_CONCURRENT, boolValue = false),
    @Property(name = Scheduler.PROPERTY_SCHEDULER_PERIOD, longValue = MessageConstants.CLEAUNUP_EVERY_X_SECONDS) })
public class ChatMessageCleaner implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageCleaner.class);
  /**
   * The JCR Repository we access to update profile.
   * 
   */
  @Reference
  private SlingRepository slingRepository;

  /**
   * 
   */
  public ChatMessageCleaner() {

  }

  /**
   * {@inheritDoc}
   * @see java.lang.Runnable#run()
   */
  public void run() {

    LOGGER.info("Starting chat messages cleanup process.");
    // need to be admin when in our own thread
    Session session = null;
    QueryManager queryManager;
    try {

      session = slingRepository.loginAdministrative(null);

      // Get the current date and substract x minutes of it.
      Date d = new Date();
      d = DateUtils.addSeconds(d, MessageConstants.CLEAUNUP_EVERY_X_SECONDS);

      // Make the format for the JCR query
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat sdfMinutes = new SimpleDateFormat("kk:mm:ss");

      String timestamp = sdf.format(d) + "T" + sdfMinutes.format(d) + ".000+01:00";

      queryManager = session.getWorkspace().getQueryManager();

      String queryPath = "/jcr:root/" + ISO9075.encodePath("_user/private")
          + "//element(*)[@" + JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY + "='"
          + MessageConstants.SAKAI_MESSAGE_RT + "' and @"
          + MessageConstants.PROP_SAKAI_TYPE + "='" + MessageConstants.TYPE_CHAT
          + "' and @" + MessageConstants.PROP_SAKAI_READ + "='false' and @"
          + JCRConstants.JCR_CREATED + " < xs:dateTime('" + timestamp + "')]";

      Query query = queryManager.createQuery(queryPath, Query.XPATH);
      QueryResult qr = query.execute();

      NodeIterator nodes = qr.getNodes();

      long i = 0;
      // Loop the found nodes and delete them
      while (nodes.hasNext()) {
        Node n = nodes.nextNode();
        n.remove();
        i++;
      }

      // need to manually save
      session.save();

      LOGGER.info("Removed {} chat messages.", i);

    } catch (RepositoryException e) {
      LOGGER.warn("Got a repository exception during clean up process.");
      throw new MessagingException(e.getMessage(), e);
    } finally {
      // need to manually logout and commit
      try {
        if (session != null)
          session.logout();
      } catch (Exception e) {
        throw new RuntimeException("Failed to logout of JCR: " + e, e);
      }
    }
  }
}
