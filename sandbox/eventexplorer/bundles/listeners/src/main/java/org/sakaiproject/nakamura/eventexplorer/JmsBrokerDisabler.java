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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

/**
 * Disables the broker that is started by the nakamura activemeq bundle.
 */
@Component(immediate = true)
public class JmsBrokerDisabler {
  private static final String BROKER_PID = "org.sakaiproject.nakamura.activemq.ActiveMQBrokerComponent";
  private static final String BROKER_ENABLED = "broker.enabled";

  @Reference
  private ConfigurationAdmin configAdmin;

  private boolean wasEnabled = true;

  @Activate
  protected void activate(Map<?, ?> props) throws Exception {
    Configuration config = configAdmin.getConfiguration(BROKER_PID, null);
    config.setBundleLocation(null);
    Dictionary brokerProps = config.getProperties();
    if (brokerProps == null) {
      brokerProps = new Hashtable();
    } else {
      wasEnabled = (Boolean) brokerProps.get("broker.enabled");
    }
    brokerProps.put(BROKER_ENABLED, "false");
    config.update(brokerProps);
  }

  @Deactivate
  protected void deactivate(Map<?, ?> props) throws Exception {
    Configuration config = configAdmin.getConfiguration(BROKER_PID, null);
    Dictionary listenerProps = config.getProperties();
    if (listenerProps == null) {
      listenerProps = new Hashtable();
    }
    listenerProps.put(BROKER_ENABLED, wasEnabled);
    config.update(listenerProps);
  }
}
