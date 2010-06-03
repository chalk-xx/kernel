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
package org.sakaiproject.nakamura.profile;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;
import org.sakaiproject.nakamura.api.profile.ProviderSettingsFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component(immediate=true)
@Service(value=ProviderSettingsFactory.class)
public class ProviderSettingsFactoryImpl implements ProviderSettingsFactory {

  
  private static final String PROVIDER_SETTINGS = "/var/profile/providersByName";

  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   * @throws  
   * 
   * @see org.sakaiproject.nakamura.api.profile.ProviderSettingsFactory#newProviderSettings(java.lang.String,
   *      javax.jcr.Node)
   */
  public ProviderSettings newProviderSettings(String path, Node node) throws RepositoryException {
    if (node.hasProperty("sakai:source")
        && "external".equals(node.getProperty("sakai:source").getString())) {
      Session session = node.getSession();
      String providerSettings = appendPath(PROVIDER_SETTINGS, appendPath(path, node.getName()));
      if (session.nodeExists(providerSettings) && session.nodeExists(providerSettings)) {
        return new ProviderSettingsImpl(node, session.getNode(providerSettings));
      }
    }
    return null;
  }

  /**
   * @param string
   * @param name
   * @return
   */
  private String appendPath(String path, String name) {
    if (path.endsWith("/")) {
      return path + name;
    }
    return path + "/" + name;
  }

}
