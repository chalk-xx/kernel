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
package org.sakaiproject.nakamura.smtp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.message.LiteMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


@RunWith(MockitoJUnitRunner.class)
public class SakaiSmtpServerTest {

  private static final String TESTMESSAGE = "SomeHeaders: Header\nSubject: testing\nHere is a message body";
  private static final String TESTMESSAGE_GOOD = ""
      + "Delivered-To: ianboston@googlemail.com \n"
      + "Received: by 10.204.61.70 with SMTP id s6cs59936bkh;\n"
      + "        Thu, 11 Feb 2010 05:18:41 -0800 (PST)\n"
      + "Received: by 10.150.194.13 with SMTP id r13mr175997ybf.81.1265894320608;\n"
      + "        Thu, 11 Feb 2010 05:18:40 -0800 (PST)\n"
      + "Return-Path: <k2-jira@sakaiproject.org>\n"
      + "Received: from mailex.atlas.pipex.net (mailex.atlas.pipex.net [194.154.164.61])\n"
      + "        by mx.google.com with ESMTP id 40si5206880ywh.30.2010.02.11.05.18.40;\n"
      + "        Thu, 11 Feb 2010 05:18:40 -0800 (PST)\n"
      + "Received-SPF: neutral (google.com: 194.154.164.61 is neither permitted nor denied by best guess record for domain of k2-jira@sakaiproject.org) client-ip=194.154.164.61;\n"
      + "Authentication-Results: mx.google.com; spf=neutral (google.com: 194.154.164.61 is neither permitted nor denied by best guess record for domain of k2-jira@sakaiproject.org) smtp.mail=k2-jira@sakaiproject.org\n"
      + "Received: from sjc-mail-2.wush.net ([208.83.222.205])\n"
      + "  by mail11.atlas.pipex.net with esmtp (Exim 4.63)\n"
      + "  (envelope-from <k2-jira@sakaiproject.org>)\n" + "  id 1NfYwI-0000dY-8H\n"
      + "  for ieb@tfd.co.uk; Thu, 11 Feb 2010 13:18:38 +0000\n"
      + "Received: from sjc-app-1.wush.net (unknown [208.83.222.212])\n"
      + "  by sjc-mail-2.wush.net (Postfix) with ESMTP id E2EE4D034B\n"
      + "  for <ieb@tfd.co.uk>; Thu, 11 Feb 2010 05:18:35 -0800 (PST)\n"
      + "Received: from sjc-app-1.wush.net (localhost.localdomain [127.0.0.1])\n"
      + "  by sjc-app-1.wush.net (Postfix) with ESMTP id 46F7810001AD\n"
      + "  for <ieb@tfd.co.uk>; Thu, 11 Feb 2010 05:18:35 -0800 (PST)\n"
      + "Message-ID: <2056926400.1265894315267.JavaMail.sakai@sjc-app-1.wush.net>\n"
      + "Date: Thu, 11 Feb 2010 05:18:35 -0800 (PST)\n"
      + "From: \"Lance Speelmon (JIRA)\" <k2-jira@sakaiproject.org>\n"
      + "To: ieb@tfd.co.uk\n"
      + "Subject: [Sakai Jira] Commented: (KERN-631) Expose Sakai 2 tools in a Sakai\n"
      + " 3 environment detached from a Sakai 2 rendered site\n" + "MIME-Version: 1.0\n"
      + "Content-Type: text/plain; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n"
      + "Precedence: bulk\n" + "Auto-Submitted: auto-generated\n"
      + "X-JIRA-FingerPrint: 43079b93228ea120d4bc89f05c6f1356\n\n"
      + "Here is a message body";

  private static final String SUBJECT_TEST = "[Sakai Jira] Commented: (KERN-631) Expose Sakai 2 tools in a Sakai\r\n"
  + " 3 environment detached from a Sakai 2 rendered site";
  private static final String MULTIPART_SUBJECT_TEST = "Breaking News Extra: Husband of Accused Huntsville Killer Says She Was Bitter Over Tenure";
  private static final String MULTIPART_SUBJECT_TEST2 = "TestBinary Mesage";
  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSmtpServerTest.class);
  @Captor
  ArgumentCaptor<Map<String, Object>> mapProperties;
  @Mock
  ContentManager contentManager;
  @Mock
  ComponentContext componentContext;
  @Mock
  Repository slingRepository;
  @Mock
  Session adminSession;
  @Mock
  LiteMessagingService messagingService;
  @Mock
  Content myMessageNode;
  @Mock
  Content part0node;
  @Mock
  Content part1node;
  List<String> recipents;
  List<String> senders;

  @Before
  public void setUp() throws ClientPoolException, StorageClientException,
      AccessDeniedException {
    when(slingRepository.loginAdministrative()).thenReturn(adminSession);
    when(adminSession.getContentManager()).thenReturn(contentManager);

    final Dictionary<String, Object> properties = new Hashtable<String, Object>();
    final int port = getSafePort(8025);
    properties.put("smtp.port", Integer.valueOf(port));
    when(componentContext.getProperties()).thenReturn(properties);

    recipents = new ArrayList<String>();
    recipents.add("alice");
    when(messagingService.expandAliases("alice")).thenReturn(recipents);
    when(messagingService.getFullPathToStore(eq("alice"), any(Session.class)))
        .thenReturn("a:alice/message");
    senders = new ArrayList<String>();
    senders.add("bob");
    when(messagingService.expandAliases("bob")).thenReturn(senders);
    when(messagingService.getFullPathToStore(eq("bob"), any(Session.class))).thenReturn(
        "a:bob/message");

    when(myMessageNode.getPath()).thenReturn("a:bob/message/messagenode");
    when(myMessageNode.getProperty("message-id")).thenReturn("messageid");

    when(part0node.getPath()).thenReturn("a:bob/message/messagenode/part000");
    when(part1node.getPath()).thenReturn("a:bob/message/messagenode/part001");
    when(contentManager.get("a:bob/message/messagenode/part000")).thenReturn(part0node);
    when(contentManager.get("a:bob/message/messagenode/part001")).thenReturn(part1node);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBadFormatMessage() throws Exception {

    InputStream dataStream = new ByteArrayInputStream(TESTMESSAGE.getBytes("UTF-8"));
    assertNotNull(dataStream);

    when(
        messagingService.create(any(Session.class), any(Map.class))).thenReturn(myMessageNode);

    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.contentRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;

    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);

    // call to messageService.create
    verify(messagingService).create(any(Session.class), mapProperties.capture());
    verify(contentManager).writeBody(eq("a:bob/message/messagenode"), eq(dataStream));
    verify(myMessageNode, times(2)).getPath();
    verify(myMessageNode).getProperty("message-id");
    
    Map<String,Object> headers = mapProperties.getValue();
    // check multi line parsing of headers
    assertEquals("testing", headers.get("sakai:subject"));

    // check multi header parsing.
    String[] recieved = (String[]) headers.get("sakai:received");
    assertNull(recieved);

    sakaiSmtpServer.deactivate(componentContext);
  }

  @Test
  public void testSafePort() throws IOException {
    ServerSocket s1 = null;
    try {
      s1 = new ServerSocket(8025);
    } catch (IOException e1) {
    }
    ServerSocket s2 = null;
    try {
      s2 = new ServerSocket(8026);
    } catch (IOException e2) {
    }
    ServerSocket s3 = null;
    try {
      s3 = new ServerSocket(8027);
    } catch (IOException e1) {
    }
    ServerSocket s4 = null;
    try {
      s4 = new ServerSocket(8028);
    } catch (IOException e1) {
    }

    int port = getSafePort(8025);
    assertTrue(port>8028);
    ServerSocket ss = new ServerSocket(port);
    assertTrue(ss.isBound());
    ss.close();

    try {

    } finally {
      try {
        s1.close();
      } catch (Exception e) {
      }
      try {
        s2.close();
      } catch (Exception e) {
      }
      try {
        s3.close();
      } catch (Exception e) {
      }
      try {
        s4.close();
      } catch (Exception e) {
      }
    }

  }

  /**
   * @param i
   * @return
   */
  private int getSafePort(int i) {
    for ( int p = i; p < i+500; p++ ) {
      ServerSocket serverSocket = null;
      try {
        serverSocket = new ServerSocket(p);
        LOGGER.info("Got socket at {} ", p);
        return p;
      } catch (IOException e) {
        LOGGER.info("Failed to get socket at {} ", p);
      } finally {
        try {
          serverSocket.close();
        } catch (Exception e) {
          LOGGER.debug("Failed to close socket at {}, safe to ignore ", p);
        }
      }
    }
    return 0;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGoodFormatMessage() throws Exception {
    InputStream dataStream = new ByteArrayInputStream(
        TESTMESSAGE_GOOD.getBytes("UTF-8"));
    assertNotNull(dataStream);

    when(messagingService.create(any(Session.class), any(Map.class))).thenReturn(
        myMessageNode);

    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.contentRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;

    sakaiSmtpServer.activate(componentContext);

    assertTrue(sakaiSmtpServer.accept("bob@localhost", "alice@localhost"));
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);

    // call to messageService.create
    verify(messagingService).create(any(Session.class), mapProperties.capture());
    verify(contentManager).writeBody(eq("a:bob/message/messagenode"), eq(dataStream));
    verify(myMessageNode, times(2)).getPath();
    verify(myMessageNode).getProperty("message-id");
    
    Map<String,Object> headers = mapProperties.getValue();
    // check multi line parsing of headers
    assertEquals(SUBJECT_TEST, headers.get("sakai:subject"));

    // check multi header parsing.
    String[] recieved = (String[]) headers.get("sakai:received");
    assertNotNull(recieved);
    assertEquals(recieved.length, 6);

    sakaiSmtpServer.deactivate(componentContext);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGoodFormatMultipartMessage() throws Exception {

    InputStream dataStream = this.getClass().getResourceAsStream("testmultipartgood.txt");
    assertNotNull(dataStream);

    when(
        messagingService.create(any(Session.class), any(Map.class), any(String.class),
            any(String.class))).thenReturn(myMessageNode);

    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.contentRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;

    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);


    // call to messageService.create
    verify(messagingService).create(eq(adminSession), mapProperties.capture(),
        any(String.class), eq("a:alice/message"));

    Map<String,Object> headers = mapProperties.getValue();
    // check multi line parsing of headers
    assertEquals(MULTIPART_SUBJECT_TEST, headers.get("sakai:subject"));

    // check multi header parsing.
    String[] recieved = (String[]) headers.get("sakai:received");
    assertNotNull(recieved);
    assertEquals(recieved.length, 6);

    sakaiSmtpServer.deactivate(componentContext);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGoodFormatMultipartBinaryMessage() throws Exception {

    InputStream dataStream = this.getClass().getResourceAsStream("testmultipartbinarygood.txt");
    assertNotNull(dataStream);

    when(
        messagingService.create(any(Session.class), any(Map.class), any(String.class),
            any(String.class))).thenReturn(myMessageNode);

    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.contentRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;

    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);


    // call to messageService.create
    verify(messagingService).create(eq(adminSession), mapProperties.capture(),
        any(String.class), eq("a:alice/message"));

    Map<String,Object> headers = mapProperties.getValue();
    // check multi line parsing of headers
    assertEquals(MULTIPART_SUBJECT_TEST2, headers.get("sakai:subject"));

    // check multi header parsing.
    String recieved =  (String) headers.get("sakai:received");
    assertNotNull(recieved);

    sakaiSmtpServer.deactivate(componentContext);
  }

}
