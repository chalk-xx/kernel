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

import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class ProviderSettingsFactory {

  protected static final String PROVIDER_SETTINGS = "/var/profile/providersByName";

  /**
   * {@inheritDoc}
   * 
   * @throws RepositoryException
   * @throws
   * 
   * @see org.sakaiproject.nakamura.api.profile.ProviderSettingsFactory#newProviderSettings(java.lang.String,
   *      javax.jcr.Node)
   */
  public ProviderSettings newProviderSettings(String path, Content profileContent, Session session)
      throws RepositoryException {
    if (profileContent.hasProperty("sakai:source")) {
      if ("external".equals(profileContent.getProperty("sakai:source"))) {
        System.err.println("Is external " + profileContent.getProperty("sakai:source"));
        
        String providerSettings = StorageClientUtils.newPath(PROVIDER_SETTINGS, path);
        if (session.nodeExists(providerSettings)) {
          return new ProviderSettingsImpl(profileContent, session.getNode(providerSettings));
        } else {
          System.err.println("No Settings "+providerSettings);   
        }
      }
    }
    return null;
  }


}
