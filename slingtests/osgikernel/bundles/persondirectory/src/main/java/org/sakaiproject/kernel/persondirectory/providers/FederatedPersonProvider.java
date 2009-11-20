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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

  @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = "bindProvider", unbind = "unbindProvider")
  private Set<PersonProvider> providers = new HashSet<PersonProvider>();

  protected void bindProvider(PersonProvider provider) {
    LOG.debug("Binding provider: {}", provider.getClass().getName());
    providers.add(provider);
  }

  protected void unbindProvider(PersonProvider provider) {
    LOG.debug("Unbinding provider: {}", provider.getClass().getName());
    providers.remove(provider);
  }

  public Set<Person> getPeople(Set<String> uids) throws PersonProviderException {
    return getPeople(uids, (String) null);
  }

  public Set<Person> getPeople(Set<String> uids, String... attributes)
      throws PersonProviderException {
    HashMap<String, PersonImpl> retPeople = null;

    for (PersonProvider provider : providers) {
      Set<Person> people = provider.getPeople(uids, attributes);
      for (Person person : people) {
        if (retPeople == null) {
          retPeople = new HashMap<String, PersonImpl>();
        }
        if (retPeople.containsKey(person.getName())) {
          // concatenate attributes if user has been seen before.
          PersonImpl p = retPeople.get(person.getName());
          p.addAttributes(person.getAttributes());

        } else if (person instanceof PersonImpl) {
          // use the object directly since it is an editable type
          retPeople.put(person.getName(), (PersonImpl) person);

        } else {
          // make sure we have an editable object in case we have to concatenate
          // other attributes
          PersonImpl p = new PersonImpl(person);
          retPeople.put(p.getName(), p);

        }
      }
    }
    Set<Person> them = null;
    if (retPeople != null) {
      them = new HashSet<Person>(retPeople.values());
    }
    return them;
  }

  public Person getPerson(String uid) throws PersonProviderException {
    return getPerson(uid, (String) null);
  }

  public Person getPerson(String uid, String... attributes) throws PersonProviderException {
    PersonImpl retPerson = null;
    for (PersonProvider provider : providers) {
      Person p = provider.getPerson(uid);
      if (p != null) {
        if (retPerson == null) {
          retPerson = new PersonImpl(uid);
        }
        retPerson.addAttributes(p.getAttributes());
      }
    }
    return retPerson;
  }
}
