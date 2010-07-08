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

package org.sakaiproject.nakamura.privacy;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.webdav.api.SlingItemFilterHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;


@Component(name="org.sakaiproject.nakamura.privacy.PrivacyItemFilter", immediate = true, metatype= true, label="%privacyfilter.name", description="%privacyfilter.description")
@Service(value = SlingItemFilterHelper.class)
public class PrivacyItemFilter implements SlingItemFilterHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyItemFilter.class);

  public boolean isFilteredItem(String name, Session session) {
    if (name.startsWith("/_user") || name.startsWith("/_group")) {
      return true;
    }
    return false;
  }

  public boolean isFilteredItem(Item item) {
    try {
      return isFilteredItem(item.getPath(), item.getSession());
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage());
    }
    return false;
  }

}
