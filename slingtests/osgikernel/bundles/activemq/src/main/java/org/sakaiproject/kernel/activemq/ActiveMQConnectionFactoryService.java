/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.kernel.api.activemq.ConnectionFactoryService;

import java.net.URI;

import javax.jms.ConnectionFactory;

@Component
@Service
public class ActiveMQConnectionFactoryService implements ConnectionFactoryService {
  public ConnectionFactory createFactory() {
    return new ActiveMQConnectionFactory();
  }

  public ConnectionFactory createFactory(String brokerURL) {
    return new ActiveMQConnectionFactory(brokerURL);
  }

  public ConnectionFactory createFactory(URI brokerURL) {
    return new ActiveMQConnectionFactory(brokerURL);
  }
}
