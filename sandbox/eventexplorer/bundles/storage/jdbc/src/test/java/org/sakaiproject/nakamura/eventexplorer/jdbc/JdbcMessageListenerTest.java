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

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import javax.jms.Message;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcMessageListenerTest {
  JdbcMessageListener listener;

  @Mock
  Connection conn;

  @Mock
  Statement stmt;

  @Mock
  DatabaseMetaData metadata;

  @Mock
  Message msg;

  HashMap<String, Object> props;

  @Before
  public void setUp() throws Exception {
    when(conn.createStatement()).thenReturn(stmt);
    when(conn.getMetaData()).thenReturn(metadata);
    when(metadata.getDatabaseProductName()).thenReturn("Apache Derby");

    listener = new JdbcMessageListener();
    props = new HashMap<String, Object>();
    props.put(JdbcMessageListener.CONNECTION_URL, "jdbc:derby:memory:testdb;create=true");
    props.put(JdbcMessageListener._CONNECTION, conn);
  }

  @Test
  public void doesntLoadDdl() throws Exception {
    listener.activate(props);
  }

  @Test(expected = SQLException.class)
  public void cantFindDdlToLoad() throws Exception {
    // throw exception here to trigger loading of ddl file
    when(stmt.execute(anyString())).thenThrow(new SQLException());

    listener.activate(props);
  }

  @Test
  public void loadDdl() throws Exception {
    // throw exception here to trigger loading of ddl file
    when(stmt.execute(anyString())).thenThrow(new SQLException()).thenReturn(Boolean.TRUE);

    listener.activate(props);

    // run the bit to be tested
    boolean sawDdl = false;
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(stmt, times(3)).execute(sql.capture());
    for (String value : sql.getAllValues()) {
      if (value != null) {
        if (value.startsWith("CREATE TABLE ")) {
          sawDdl = true;
        }
      }
    }
    assertEquals("Expected to see a request for client.ddl", true, sawDdl);
  }

  @Test
  public void onMessageMockedConn() throws Exception {
    listener.activate(props);

    when(msg.getJMSType()).thenReturn("typeOnegative");
    when(msg.getJMSTimestamp()).thenReturn(System.currentTimeMillis());
    when(msg.getStringProperty(JdbcMessageListener.USER_ID)).thenReturn("psteele");
    when(msg.getStringProperty(JdbcMessageListener.CLUSTER_SERVER_ID)).thenReturn("home");
    when(msg.getObjectProperty("something")).thenReturn("not much");
    when(msg.getObjectProperty("random")).thenReturn("totally");

    Vector<String> fields = new Vector<String>();
    fields.add(JdbcMessageListener.USER_ID);
    fields.add(JdbcMessageListener.CLUSTER_SERVER_ID);
    fields.add("something");
    fields.add("random");
    when(msg.getPropertyNames()).thenReturn(fields.elements());

    PreparedStatement eventPs = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(eventPs);
    when(eventPs.getGeneratedKeys()).thenReturn(rs);
    when(rs.next()).thenReturn(true).thenReturn(false);
    when(rs.getInt(1)).thenReturn(100);

    PreparedStatement eventPropPs = mock(PreparedStatement.class);
    when(conn.prepareStatement(anyString())).thenReturn(eventPropPs);

    listener.onMessage(msg);

    Properties sqlProps = new Properties();
    sqlProps.load(getClass().getResourceAsStream("client.sql"));

    ArgumentCaptor<String> sqls = ArgumentCaptor.forClass(String.class);
    verify(conn).prepareStatement(sqls.capture(), eq(Statement.RETURN_GENERATED_KEYS));
    verify(conn).prepareStatement(sqls.capture());
    assertEquals(sqlProps.get("insert.event"), sqls.getAllValues().get(0));
    assertEquals(sqlProps.get("insert.event_prop"), sqls.getAllValues().get(1));

    verify(eventPs).executeUpdate();
    verify(eventPropPs, times(2)).executeUpdate();
  }

  @Test
  public void onMessageLiveConn() throws Exception {
    props.remove(JdbcMessageListener._CONNECTION);

    listener.activate(props);

    when(msg.getJMSType()).thenReturn("typeOnegative");
    when(msg.getJMSTimestamp()).thenReturn(System.currentTimeMillis());
    when(msg.getStringProperty(JdbcMessageListener.USER_ID)).thenReturn("psteele");
    when(msg.getStringProperty(JdbcMessageListener.CLUSTER_SERVER_ID)).thenReturn("home");
    when(msg.getObjectProperty("something")).thenReturn("not much");
    when(msg.getObjectProperty("random")).thenReturn("totally");

    Vector<String> fields = new Vector<String>();
    fields.add(JdbcMessageListener.USER_ID);
    fields.add(JdbcMessageListener.CLUSTER_SERVER_ID);
    fields.add("something");
    fields.add("random");
    when(msg.getPropertyNames()).thenReturn(fields.elements());

    listener.onMessage(msg);

    // nothing we can verify but if we don't get any RuntimeExceptions everything should
    // have finished correctly.
  }
}
