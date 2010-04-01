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
package org.sakaiproject.nakamura.activemq;

import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jms.ConnectionFactory;

/**
 *
 */
public class ActiveMQConnectionFactoryServiceTest {

  @Test
  public void testActivate() {

    ActiveMQConnectionFactoryService service = new ActiveMQConnectionFactoryService();

    Dictionary<String, String> dictionary = new Hashtable<String, String>();
    dictionary.put(ActiveMQConnectionFactoryService.BROKER_URL, "vm://localhost:60303");
    ComponentContext context = mock(ComponentContext.class);
    when(context.getProperties()).thenReturn(dictionary);

    service.activateForTest(context);

    assertNotNull(service.getDefaultConnectionFactory());
    assertNotNull(service.getDefaultPooledConnectionFactory());

    service.deactivate(context);
  }

  @Test
  public void testCreate() throws URISyntaxException {
    ActiveMQConnectionFactoryService service = new ActiveMQConnectionFactoryService();
    ConnectionFactory factory = service.createFactory("vm://localhost:60304");
    assertNotNull(factory);
    factory = null;

    URI uri = new URI("vm://localhost:60305");
    ConnectionFactory urlFactory = service.createFactory(uri);
    assertNotNull(urlFactory);
    urlFactory = null;
  }

}
