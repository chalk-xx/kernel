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
package org.sakaiproject.nakamura.eventexplorer.jdbc;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 *
 */
@Component
@Service
public class JdbcMessageListener implements MessageListener {
  public static final String DEFAULT_CONNECTION_URL = "jdbc:derby:testdb;create=true";
  public static final String DEFAULT_JDBC_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String DEFAULT_USER = "sa";
  public static final String DEFAULT_PASS = "";

  @Property(JdbcMessageListener.DEFAULT_CONNECTION_URL)
  public static final String CONNECTION_URL = "jdbc-url";

  @Property(JdbcMessageListener.DEFAULT_JDBC_DRIVER)
  public static final String JDBC_DRIVER = "jdbc-driver";

  /** property for unit test to inject a connection */
  static final String _CONNECTION = "connection";

  /** user ID field in message */
  static final String USER_ID = "userid";

  /** cluster server ID in message */
  static final String CLUSTER_SERVER_ID = "clusterServerId";

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMessageListener.class);
  private static final String CHECK_SCHEMA_PROP = "check.schema";
  private static final String SQL_EVENT = "insert.event";
  private static final String SQL_EVENT_PROP = "insert.event_prop";

  @Property(JdbcMessageListener.DEFAULT_USER)
  private static final String USERNAME = "username";

  @Property(JdbcMessageListener.DEFAULT_PASS)
  private static final String PASSWORD = "password";

  private Connection conn;

  private Properties connectionProperties;

  private String username;

  private String password;

  private String url;

  private Properties sql;

  private String driver;

  @Activate
  protected void activate(Map<?, ?> props) throws Exception {
    // get connection to database
    driver = props.containsKey(JDBC_DRIVER) ? (String) props.get(JDBC_DRIVER)
                                           : JdbcMessageListener.DEFAULT_JDBC_DRIVER;
    username = props.containsKey(USERNAME) ? (String) props.get(USERNAME)
                                          : JdbcMessageListener.DEFAULT_USER;
    password = props.containsKey(PASSWORD) ? (String) props.get(PASSWORD)
                                          : JdbcMessageListener.DEFAULT_PASS;
    url = props.containsKey(CONNECTION_URL) ? (String) props.get(CONNECTION_URL)
                                           : JdbcMessageListener.DEFAULT_CONNECTION_URL;
    connectionProperties = new Properties();
    connectionProperties.putAll(props);

    conn = props.containsKey(_CONNECTION) ? (Connection) props.get(_CONNECTION)
                                          : getConnection();

    DatabaseMetaData metadata = conn.getMetaData();
    String dbProductName = metadata.getDatabaseProductName().replaceAll(" ", "");

    loadSql(dbProductName);

    loadDdl(dbProductName);
  }

  @Deactivate
  protected void deactivate(Map<?, ?> props) throws SQLException {
    // drop connection to database
    conn.close();
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
      String serverId = msg.getStringProperty(CLUSTER_SERVER_ID);
      String user = "system";
      if (msg.propertyExists(USER_ID)) {
        user = msg.getStringProperty(USER_ID);
      }

      // insert common data as hub record
      PreparedStatement ps = conn.prepareStatement(sql.getProperty(SQL_EVENT),
          Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, type);
      ps.setString(2, serverId);
      ps.setString(3, user);
      ps.setTimestamp(4, new Timestamp(msg.getJMSTimestamp()));
      ps.executeUpdate();
      ResultSet rs = ps.getGeneratedKeys();
      int hubId = -1;
      if (rs == null || !rs.next()) {
        String m = "Unable to get ID of inserted hub record.";
        LOGGER.error(m);
        throw new RuntimeException(m);
      }
      hubId = rs.getInt(1);

      // deal with the extraneous properties
      ps = conn.prepareStatement(sql.getProperty(SQL_EVENT_PROP));

      @SuppressWarnings("unchecked")
      Enumeration<String> propNames = msg.getPropertyNames();
      while (propNames.hasMoreElements()) {
        String propName = propNames.nextElement();
        if (USER_ID.equals(propName) || CLUSTER_SERVER_ID.equals(propName)) {
          continue;
        }

        Object obj = msg.getObjectProperty(propName);
        ps.setInt(1, hubId);
        ps.setString(2, propName);
        ps.setObject(3, obj);
        ps.executeUpdate();
      }
    } catch (Exception e) {
      LOGGER.error(
          "Failed to insert the JMS message in the JDBC store: " + e.getMessage(), e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  /**
   * Load the SQL script for the driver being used.
   *
   * @param dbProductName
   * @throws SQLException If there is a problem executing script or the script doesn't exist.
   * @throws IOException If there is a problem reading the DDL file.
   */
  private void loadSql(String dbProductName) throws SQLException, IOException {
    // try getting the generic sql file
    InputStream sqlFile = loadFile("client", "sql", dbProductName);

    if (sqlFile == null) {
      throw new SQLException("Unable to load sql file for: " + dbProductName);
    } else {
      // load up the found sql file
      sql = new Properties();
      sql.load(sqlFile);
    }
  }

  /**
   * Load the DDL script for the driver being used.
   *
   * @param dbProductName
   * @throws SQLException If there is a problem executing script or the script doesn't exist.
   * @throws IOException If there is a problem reading the DDL file.
   */
  private void loadDdl(String dbProductName) throws SQLException, IOException {
    Statement stmt = null;
    try {
      stmt = conn.createStatement();
      stmt.execute(sql.getProperty(CHECK_SCHEMA_PROP));
    } catch (SQLException e) {
      // failed the schema check; load and execute the ddl
      InputStream ddlFile = loadFile("client", "ddl", dbProductName);

      if (ddlFile == null) {
        throw new SQLException("Unable to load ddl file for: " + dbProductName);
      } else {
        // read up the whole ddl file then execute it
        StringBuilder ddl = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(ddlFile));
        String line = null;
        while ((line = reader.readLine()) != null) {
          ddl.append(line);

          if (line.endsWith(";")) {
            stmt.execute(ddl.substring(0, ddl.length() - 1));
            ddl = new StringBuilder();
          }
        }
      }
    } finally {
      stmt.close();
    }
  }

  private Connection getConnection() throws ClassNotFoundException, SQLException {
    Class.forName(driver);

    Connection conn = null;
    if (username != null && username.length() > 0) {
      conn = DriverManager.getConnection(url, username, password);
    } else {
      conn = DriverManager.getConnection(url, connectionProperties);
    }
    return conn;
  }

  private InputStream loadFile(String name, String extension, String dbProductName) {
    InputStream file = null;
    if (dbProductName != null && dbProductName.length() > 0) {
      file = getClass().getResourceAsStream(name + "." + dbProductName + "." + extension);
    }
    if (file == null) {
      file = getClass().getResourceAsStream(name + "." + extension);
    }
    return file;
  }
}
