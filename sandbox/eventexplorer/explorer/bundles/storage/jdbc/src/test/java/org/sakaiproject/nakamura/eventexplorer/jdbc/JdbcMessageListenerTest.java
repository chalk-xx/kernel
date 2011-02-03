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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

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
  PreparedStatement ps;

  @Mock
  DatabaseMetaData metadata;

  HashMap<String, Object> props;

  @Before
  public void setUp() throws Exception {
    when(conn.createStatement()).thenReturn(stmt);
    when(conn.prepareStatement(isA(String.class))).thenReturn(ps);
    when(conn.getMetaData()).thenReturn(metadata);
    when(metadata.getDatabaseProductName()).thenReturn("");

    listener = new JdbcMessageListener();
    props = new HashMap<String, Object>();
    props.put(JdbcMessageListener._CONNECTION, conn);
  }

  @Test
  public void doesntLoadDdl() throws Exception {
    listener.activate(props);
  }

  @Test(expected = SQLException.class)
  public void cantFindDdlToLoad() throws Exception {
    when(stmt.execute(anyString())).thenThrow(new SQLException());

    listener.activate(props);
  }

  @Test
  public void loadSqlAndDdl() throws Exception {
    final ClassLoader was = Thread.currentThread().getContextClassLoader();
    final ClassLoader is = new ClassLoader(was) {
      @Override
      public InputStream getResourceAsStream(String name) {
        if ("client.sql".equals(name)) {
          return new ByteArrayInputStream("--client.sql".getBytes());
        } else if ("client.ddl".equals(name)) {
          return new ByteArrayInputStream("--client.ddl".getBytes());
        } else {
          return super.getResourceAsStream(name);
        }
      }
    };
    Thread.currentThread().setContextClassLoader(is);

    when(stmt.execute(anyString())).thenThrow(new SQLException());

    listener.activate(props);

    try {
      // run the bit to be tested
      boolean sawSql = false;
      boolean sawDdl = false;
      ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
      verify(stmt).execute(sql.capture());
      for (String value : sql.getAllValues()) {
        if ("--client.sql".equals(value)) {
          sawSql = true;
        } else if ("--client.ddl".equals(value)) {
          sawDdl = true;
        }
      }

      verify(conn).prepareStatement(sql.capture());
      assertEquals("Expected to see a request for client.sql", true, sawSql);
      assertEquals("Expected to see a request for client.ddl", true, sawDdl);
    } finally {
      Thread.currentThread().setContextClassLoader(was);
    }
  }
}
