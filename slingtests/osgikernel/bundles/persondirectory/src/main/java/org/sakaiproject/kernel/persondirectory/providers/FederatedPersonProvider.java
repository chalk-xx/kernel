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
package org.sakaiproject.kernel.persondirectory.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;
import org.sakaiproject.kernel.persondirectory.PersonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;

/**
 * Person information provider that aggregates other person information
 * provides. The methods called on this person provider uses other person
 * providers to get information then aggregates that information, so that the
 * federated approach is transparent.
 *
 * @author Carl Hall
 */
@Component
@Service(value = FederatedPersonProvider.class)
public class FederatedPersonProvider implements PersonProvider {
  private static final Logger LOG = LoggerFactory.getLogger(FederatedPersonProvider.class);

  /** Storage of providers available for looking up person information. */
  @Reference(referenceInterface = PersonProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = "bindProvider", unbind = "unbindProvider")
  private Set<PersonProvider> providers = new HashSet<PersonProvider>();

  /**
   * Bind a provider to this component.
   *
   * @param provider
   *          The provider to bind.
   */
  protected void bindProvider(PersonProvider provider) {
    LOG.debug("Binding provider: {}", provider.getClass().getName());
    providers.add(provider);
  }

  /**
   * Unbind a provider from this component.
   *
   * @param provider
   *          The provider to unbind.
   */
  protected void unbindProvider(PersonProvider provider) {
    LOG.debug("Unbinding provider: {}", provider.getClass().getName());
    providers.remove(provider);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.persondirectory.PersonProvider#getPerson(java.lang.String)
   */
  public Person getPerson(String uid, Node profileNode) throws PersonProviderException {
    PersonImpl retPerson = null;
    for (PersonProvider provider : providers) {
      Person p = provider.getPerson(uid, profileNode);
      if (p != null) {
        if (retPerson == null) {
          retPerson = new PersonImpl(p);
        } else {
          retPerson.addAttributes(p.getAttributes());
        }
      }
    }
    return retPerson;
  }
}
