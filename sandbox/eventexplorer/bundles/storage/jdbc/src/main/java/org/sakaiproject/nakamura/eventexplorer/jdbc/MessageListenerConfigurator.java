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
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

/**
 * When the {@link ConfigurationAdmin} and {@link JdbcMessageListener} become active, this
 * class gets a handle to the configuration for JmsRouteBuilder in the event explorer
 * listeners bundle and sets messageListener.target = "(service.pid={@link JdbcMessageListener}
 * .class.getName()})".<br/>
 * The target value is set to its previous value when this tracker is closed.
 */
@Component
public class MessageListenerConfigurator {
  private static final String ROUTE_BUILDER_PID = "org.sakaiproject.nakamura.eventexplorer.JmsRouteBuilder";
  private static final String MESSAGE_LISTENER_TARGET = "messageListener.target";

  private Object oldMessageListenerTarget;

  @Reference
  private ConfigurationAdmin configAdmin;

  /**
   * Get this reference to make sure our message listener is active. We don't do anything
   * with it.
   */
  @SuppressWarnings("unused")
  @Reference
  private JdbcMessageListener messageListener;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Activate
  protected void activate(Map<?, ?> props) throws IOException {
    Configuration config = configAdmin.getConfiguration(ROUTE_BUILDER_PID);
    Dictionary listenerProps = config.getProperties();
    if (listenerProps == null) {
      listenerProps = new Hashtable();
    }
    oldMessageListenerTarget = listenerProps.get(MESSAGE_LISTENER_TARGET);
    listenerProps.put(MESSAGE_LISTENER_TARGET, "(service.pid="
        + JdbcMessageListener.class.getName() + ")");
    config.update(listenerProps);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Deactivate
  protected void deactivate(Map<?, ?> props) throws IOException {
    Configuration config = configAdmin.getConfiguration(ROUTE_BUILDER_PID);
    Dictionary listenerProps = config.getProperties();
    if (listenerProps != null) {
      listenerProps.put(MESSAGE_LISTENER_TARGET, oldMessageListenerTarget);
    }
    config.update(listenerProps);
  }
}
