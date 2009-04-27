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
package org.sakaiproject.kernel.messaging.email;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sakaiproject.kernel.api.messaging.email.MailSessionProvider;
import org.sakaiproject.kernel.messaging.email.MailSessionProviderImpl;

import java.util.Properties;

import javax.mail.Session;

/**
 *
 */
public class MailSessionProviderT {
  @Test
  public void getSession() {
    Properties dict = new Properties();
    dict.put(MailSessionProviderImpl.SMTP_HOST, "localhost");
    dict.put(MailSessionProviderImpl.SMTP_PORT, "25");
    MailSessionProvider prov = new MailSessionProviderImpl(dict);
    Session session = prov.getSession();
    assertNotNull(session);
  }
}
