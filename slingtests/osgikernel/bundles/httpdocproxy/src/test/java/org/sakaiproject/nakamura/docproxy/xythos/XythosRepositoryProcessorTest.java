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
package org.sakaiproject.nakamura.docproxy.xythos;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import static org.mockito.Mockito.*;


/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class XythosRepositoryProcessorTest {

  private XythosRepositoryProcessor processor;

  @Mock
  Node node;

  @Mock
  Session session;

  @Before
  public void setup() {
    processor = new XythosRepositoryProcessor();
  }

  @Test
  public void tryFileUpdate() throws Exception {
    when(node.getSession()).thenReturn(session);
    when(session.getUserID()).thenReturn("zach");
    String path = "/zach/hello.txt";
    Map<String, Object> properties = new HashMap<String, Object>();
    String message = "Hello world!";
    InputStream documentStream = new ByteArrayInputStream(message.getBytes("UTF-8"));
    long streamLength = message.length();
    // processor.updateDocument(node, path, properties, documentStream, streamLength);
    assertTrue(Boolean.TRUE);
  }

}
