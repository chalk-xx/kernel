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
package org.sakaiproject.nakamura.eventexplorer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

public class SakaiConsumer {

  private static String brokerURL = "tcp://hostname:61616";
  private static transient ConnectionFactory factory;
  private transient Connection connection;
  private transient Session session;
  private static int id;

  public SakaiConsumer() throws JMSException {
    factory = new ActiveMQConnectionFactory(brokerURL);
    connection = factory.createConnection();
    connection.start();
    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    id = 0;
  }

  public void close() throws JMSException {
    if (connection != null) {
      connection.close();
    }
  }

  public static void main(String[] args) throws JMSException {
    SakaiConsumer consumer = new SakaiConsumer();

    Destination destination = consumer.getSession().createQueue("SAMPLE_QUEUE");
    MessageConsumer messageConsumer = consumer.getSession().createConsumer(destination);
    messageConsumer.setMessageListener(new Listener());
  }

  public Session getSession() {
    return session;
  }

}
