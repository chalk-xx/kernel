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
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.SakaiAuthorizableService;
import org.sakaiproject.nakamura.util.IOUtils;
import org.sakaiproject.nakamura.util.osgi.AbstractOrderedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 *
 */
@Component(immediate=true)
@Service(value=AuthorizablePostProcessService.class)
@References({
    @Reference(name="PostProcessors",cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy=ReferencePolicy.DYNAMIC,
        referenceInterface=AuthorizablePostProcessor.class,
        strategy=ReferenceStrategy.EVENT,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor")})
public class AuthorizablePostProcessorServiceImpl extends AbstractOrderedService<AuthorizablePostProcessor> implements AuthorizablePostProcessService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizablePostProcessorServiceImpl.class);

  @Reference
  protected transient SlingRepository repository;

  private AuthorizablePostProcessor[] orderedServices = new AuthorizablePostProcessor[0];
  private ComponentContext componentContext;
  private Collection<String> defaultUserIds;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.api.SlingHttpServletRequest, java.util.List)
   */
  public void process(Authorizable authorizable, Session session, Modification change) throws Exception {
    for ( AuthorizablePostProcessor processor : orderedServices ) {
       processor.process(authorizable, session, change);
       // Allowing a dirty session to pass between post-processor components
       // can trigger InvalidItemStateException after a Workspace.copy.
       // TODO Check to see if this is still a problem after we upgrade to
       // Jackrabbit 2.1.1
       if (session.hasPendingChanges()) {
         session.save();
       }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#bindSakaiAuthorizableService(org.sakaiproject.nakamura.api.user.SakaiAuthorizableService)
   */
  public void bindSakaiAuthorizableService(SakaiAuthorizableService sakaiAuthorizableService) {
    try {
      defaultUserIds = initDefaultUsers(sakaiAuthorizableService);
    } catch (IOException e) {
      LOGGER.error("Could not initialize default users", e);
    } catch (RepositoryException e) {
      LOGGER.error("Could not initialize default users", e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#unbindSakaiAuthorizableService(org.sakaiproject.nakamura.api.user.SakaiAuthorizableService)
   */
  public void unbindSakaiAuthorizableService(SakaiAuthorizableService sakaiAuthorizableService) {
    defaultUserIds = null;
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
    applyPostProcessorsToDefaultUsers();
  }

  protected void unbindAuthorizablePostProcessor(AuthorizablePostProcessor service, Map<String, Object> properties) {
    removeService(service, properties);
    applyPostProcessorsToDefaultUsers();
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
    this.componentContext = componentContext;
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
  }

  private Collection<String> initDefaultUsers(SakaiAuthorizableService sakaiAuthorizableService) throws IOException, RepositoryException {
    List<String> userIds = new ArrayList<String>();
    Session session = null;
    try {
      session = repository.loginAdministrative(null);
      UserManager userManager = AccessControlUtil.getUserManager(session);

      // Apply default user properties from JSON files.
      Pattern fileNamePattern = Pattern.compile("/users/|\\.json");
      @SuppressWarnings("rawtypes")
      Enumeration entriesEnum = componentContext.getBundleContext().getBundle().findEntries("users", "*.json", true);
      while (entriesEnum.hasMoreElements()) {
        Object entry = entriesEnum.nextElement();
        URL jsonUrl = new URL(entry.toString());
        String jsonFileName = jsonUrl.getFile();
        String userId = fileNamePattern.matcher(jsonFileName).replaceAll("");
        Authorizable authorizable = userManager.getAuthorizable(userId);
        if (authorizable != null) {
          userIds.add(userId);
          applyJsonToAuthorizable(jsonUrl, authorizable, session);
          sakaiAuthorizableService.postprocess(authorizable, session);
          LOGGER.info("Initialized default user {}", userId);
        } else {
          LOGGER.warn("Configured default user {} not found", userId);
        }
      }
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return userIds;
  }

  private void applyJsonToAuthorizable(URL url, Authorizable authorizable, Session session) throws IOException, RepositoryException {
    String jsonString = IOUtils.readFully(url.openStream(), "UTF-8");
    if (jsonString != null) {
      try {
        JSONObject jsonObject = new JSONObject(jsonString);
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
          String key = keys.next();
          Value value = JcrResourceUtil.createValue(jsonObject.get(key), session);
          authorizable.setProperty(key, value);
        }
      } catch (JSONException e) {
        LOGGER.error("Faulty JSON at " + url, e);
      }
    }
  }

  private void applyPostProcessorsToDefaultUsers() {
    if ((defaultUserIds != null) &&
        (defaultUserIds.size() > 0) &&
        (orderedServices.length > 0)) {
      Session session = null;
      try {
        session = repository.loginAdministrative(null);
        UserManager userManager = AccessControlUtil.getUserManager(session);
        for (String userId : defaultUserIds) {
          Authorizable authorizable = userManager.getAuthorizable(userId);
          if (authorizable != null) {
            process(authorizable, session, Modification.onCreated(userId));
          }
        }
      } catch (Exception e) {
        LOGGER.error("Could not apply post-processors to default users", e);
      } finally {
        if (session != null) {
          session.logout();
        }
      }
    }
  }
}
