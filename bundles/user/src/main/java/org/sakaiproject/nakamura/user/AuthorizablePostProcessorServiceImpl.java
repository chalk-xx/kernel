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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.util.osgi.AbstractOrderedService;

import java.util.Comparator;
import java.util.List;

import javax.jcr.Session;

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

  
  private AuthorizablePostProcessor[] orderedServices = new AuthorizablePostProcessor[0];

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.api.SlingHttpServletRequest, java.util.List)
   */
  public void process(Authorizable authorizable, Session session, Modification change) throws Exception {
    for ( AuthorizablePostProcessor processor : orderedServices ) {
       processor.process(authorizable, session, change);
    }
  }

  /**
   * @return
   */
  protected Comparator<AuthorizablePostProcessor> getComparitor() {
    return new Comparator<AuthorizablePostProcessor>() {
      public int compare(AuthorizablePostProcessor o1, AuthorizablePostProcessor o2) {
        return o1.getSequence() - o2.getSequence();
      }
    };  
  }



  protected void bindAuthorizablePostProcessor(AuthorizablePostProcessor service) {
    addService(service);
  }
  
  protected void unbindAuthorizablePostProcessor(AuthorizablePostProcessor service) {
    removeService(service);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.util.osgi.AbstractOrderedService#saveArray(java.util.List)
   */
  @Override
  protected void saveArray(List<AuthorizablePostProcessor> serviceList) {
    orderedServices = serviceList.toArray(new AuthorizablePostProcessor[serviceList.size()]);
  }
}
