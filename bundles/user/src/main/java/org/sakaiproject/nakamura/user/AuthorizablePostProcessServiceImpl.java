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
package org.sakaiproject.nakamura.user;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.AbstractOrderedService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

@Component(immediate=true)
@Service(value=AuthorizablePostProcessService.class)
@References({
    @Reference(name="PostProcessors",cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy=ReferencePolicy.DYNAMIC,
        referenceInterface=AuthorizablePostProcessor.class,
        strategy=ReferenceStrategy.EVENT,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor")})
public class AuthorizablePostProcessServiceImpl extends AbstractOrderedService<AuthorizablePostProcessor> implements AuthorizablePostProcessService {

  @Reference
  protected SlingRepository repository;

  AuthorizablePostProcessor sakaiUserProcessor;
  AuthorizablePostProcessor sakaiGroupProcessor;
  DefaultAuthorizablesLoader defaultAuthorizablesLoader;
  private AuthorizablePostProcessor[] orderedServices = new AuthorizablePostProcessor[0];

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification)
   */
  public void process(Authorizable authorizable, Session session, ModificationType change) throws Exception {
    process(authorizable, session, change, new HashMap<String, Object[]>());
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session,
      ModificationType change, Map<String, Object[]> parameters) throws Exception {
    // Set up the Modification argument.
    final String pathPrefix = authorizable.isGroup() ?
        UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX :
        UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX;
    Modification modification = new Modification(change, pathPrefix + authorizable.getID(), null);

    if (change != ModificationType.DELETE) {
      doInternalProcessing(authorizable, session, modification, parameters);
    }
    for ( AuthorizablePostProcessor processor : orderedServices ) {
      processor.process(authorizable, session, modification, parameters);
      // Allowing a dirty session to pass between post-processor components
      // can trigger InvalidItemStateException after a Workspace.copy.
      // TODO Check to see if this is still a problem after we upgrade to
      // Jackrabbit 2.1.1
      if (session.hasPendingChanges()) {
        session.save();
      }
    }
    if (change == ModificationType.DELETE) {
      doInternalProcessing(authorizable, session, modification, parameters);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, org.apache.sling.api.SlingHttpServletRequest)
   */
  public void process(Authorizable authorizable, Session session,
      ModificationType change, SlingHttpServletRequest request) throws Exception {
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    if (request != null) {
      RequestParameterMap originalParameters = request.getRequestParameterMap();
      for (String originalParameterName : originalParameters.keySet()) {
        if (originalParameterName.startsWith(SlingPostConstants.RP_PREFIX)) {
          RequestParameter[] values = originalParameters.getValues(originalParameterName);
          String[] stringValues = new String[values.length];
          for (int i = 0; i < values.length; i++) {
            stringValues[i] = values[i].getString();
          }
          parameters.put(originalParameterName, stringValues);
        }
      }
    }
    process(authorizable, session, change, parameters);
  }

  /**
   * @return
   */
  protected Comparator<AuthorizablePostProcessor> getComparator(final Map<AuthorizablePostProcessor, Map<String, Object>> propertiesMap) {
    return new Comparator<AuthorizablePostProcessor>() {
      public int compare(AuthorizablePostProcessor o1, AuthorizablePostProcessor o2) {
        Map<String, Object> props1 = propertiesMap.get(o1);
        Map<String, Object> props2 = propertiesMap.get(o2);

        return OsgiUtil.getComparableForServiceRanking(props1).compareTo(props2);
      }
    };
  }

  protected void bindAuthorizablePostProcessor(AuthorizablePostProcessor service, Map<String, Object> properties) {
    addService(service, properties);
    if (defaultAuthorizablesLoader != null) {
      defaultAuthorizablesLoader.initDefaultUsers();
    }
  }

  protected void unbindAuthorizablePostProcessor(AuthorizablePostProcessor service, Map<String, Object> properties) {
    removeService(service, properties);
    if (defaultAuthorizablesLoader != null) {
      defaultAuthorizablesLoader.initDefaultUsers();
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.util.osgi.AbstractOrderedService#saveArray(java.util.List)
   */
  @Override
  protected void saveArray(List<AuthorizablePostProcessor> serviceList) {
    orderedServices = serviceList.toArray(new AuthorizablePostProcessor[serviceList.size()]);
  }

  @Activate
  protected void activate(ComponentContext componentContext) {
    this.sakaiUserProcessor = new SakaiUserProcessor();
    this.sakaiGroupProcessor = new SakaiGroupProcessor();
    this.defaultAuthorizablesLoader = new DefaultAuthorizablesLoader(this,
        componentContext, repository);
    defaultAuthorizablesLoader.initDefaultUsers();
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    this.sakaiUserProcessor = null;
    this.sakaiGroupProcessor = null;
    this.defaultAuthorizablesLoader = null;
  }

  private void doInternalProcessing(Authorizable authorizable, Session session,
      Modification change, Map<String, Object[]> parameters) throws Exception {
    if (authorizable.isGroup()) {
      sakaiGroupProcessor.process(authorizable, session, change, parameters);
    } else {
      sakaiUserProcessor.process(authorizable, session, change, parameters);
    }
  }
}
