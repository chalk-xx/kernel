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
package org.sakaiproject.nakamura.eventexplorer.oracle;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Map;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 *
 */
@Component
@Service
public class JdbcMessageListener implements MessageListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMessageListener.class);
  private static final String SQL_EVENT = "insert into EVENT (type, serverId, user, timestamp) value (?, ?, ?, ?)";
  private static final String SQL_EVENT_PROP = "insert into EVENT_PROP (eventId, key, value) values (?, ?, ?)";

  private Connection conn;

  @Activate
  protected void activate(Map<?, ?> props) {
    // TODO get connection to database
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
   */
  public void onMessage(Message msg) {
    try {
      // collect the common data
      String type = msg.getJMSType();
      String serverId = msg.getStringProperty("clusterServerId");
      String user = "system";
      if (msg.propertyExists("userid")) {
        user = msg.getStringProperty("userid");
      }

      // insert common data as hub record
      PreparedStatement ps = conn.prepareStatement(SQL_EVENT);
      ps.setString(1, type);
      ps.setString(2, serverId);
      ps.setString(3, user);
      ps.setTimestamp(4, new Timestamp(msg.getJMSTimestamp()));
      ps.executeUpdate();
      ResultSet rs = ps.getGeneratedKeys();
      int hubId = -1;
      if (!rs.next()) {
        LOGGER.error("Unable to get ID of inserted hub record.");
        return;
      }
      hubId = rs.getInt(1);

      // deal with the extraneous properties
      ps = conn.prepareStatement(SQL_EVENT_PROP);

      Enumeration<String> propNames = msg.getPropertyNames();
      while (propNames.hasMoreElements()) {
        String propName = propNames.nextElement();
        if ("userid".equals(propName)) {
          continue;
        }

        Object obj = msg.getObjectProperty(propName);
        ps.setInt(1, hubId);
        ps.setObject(2, obj);
        ps.executeUpdate();
      }
    } catch (Exception e) {
      LOGGER.error("Failed to insert the JMS message in the JDBC store: " + e.getMessage(), e);
    }
  }
}
