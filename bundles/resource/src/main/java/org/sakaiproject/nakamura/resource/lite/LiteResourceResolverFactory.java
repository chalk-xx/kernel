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
package org.sakaiproject.nakamura.resource.lite;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;

import java.util.Map;

/**
 * Resource resolver factory to provide sparse map content resource resolvers.<br/>
 * <br/>
 * {@link org.apache.sling.auth.core.impl.SlingAuthenticator} has a Reference(static,
 * unary) to ResourceResolverFactory. To use this factory instead of the default, the
 * authenticator needs to be configured to target this class.<br/>
 * <br/>
 * org.apache.sling.engine.impl.auth.SlingAuthenticator.resourceResolverFactory.target=(service.pid=org.sakaiproject.nakamura.resource.lite.LiteResourceResolverFactory)<br/>
 * <br/>
 * Do not enable this as a service until you are ready to deal with making sure it plays
 * well in the Sling stack.
 */
@Component(enabled = false)
@Service
public class LiteResourceResolverFactory implements ResourceResolverFactory {

  @Reference
  private Repository repository;

  /**
   * name of user name property in authentication information map. Copied from
   * {@link org.apache.sling.api.resource.ResourceResolverFactory.USER} until we're using
   * the Sling Resource bundle >= 2.1.0.
   */
  private String USER = "user.name";

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
   */
  public ResourceResolver getAdministrativeResourceResolver(Map<String, Object> authnInfo)
      throws LoginException {
    try {
      // TODO should use ResourceResolverFactory.USER once we are on Sling Resource
      // bundle >= 2.1.0
      String userId = (String) authnInfo.get(USER);
      Session session = repository.loginAdministrative();
      return new LiteResourceResolver(session, userId);
    } catch (AccessDeniedException e) {
      throw new LoginException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new LoginException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
   */
  public ResourceResolver getResourceResolver(Map<String, Object> authnInfo)
      throws LoginException {
    try {
      // TODO should use ResourceResolverFactory.USER once we are on Sling Resource
      // bundle >= 2.1.0
      String userId = (String) authnInfo.get(USER);
      Session session = repository.loginAdministrative(userId);
      return new LiteResourceResolver(session, userId);
    } catch (AccessDeniedException e) {
      throw new LoginException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new LoginException(e.getMessage(), e);
    }
  }

}
