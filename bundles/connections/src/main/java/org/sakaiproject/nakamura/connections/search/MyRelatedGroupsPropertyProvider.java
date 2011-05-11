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
package org.sakaiproject.nakamura.connections.search;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.solr.Query;
import org.sakaiproject.nakamura.api.search.solr.Result;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchException;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchResultSet;
import org.sakaiproject.nakamura.api.search.solr.SolrSearchServiceFactory;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides properties for myrelatedgroups.json. Added my groups and contact's groups,
 * titles, and tags.
 */
@Component
@Service
@Property(name = SearchConstants.REG_PROVIDER_NAMES, value = "MyRelatedGroupsPropertyProvider")
public class MyRelatedGroupsPropertyProvider implements SolrSearchPropertyProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(MyRelatedGroupsPropertyProvider.class);

  private static final String CONTACTS_QUERY_TMPL = "path:({0}) AND resourceType:sakai/contact AND state:(ACCEPTED -NONE)";

  @Reference
  private ConnectionSearchPropertyProvider connPropProv;

  @Reference
  private SolrSearchServiceFactory searchServiceFactory;

  @Reference
  private Repository repo;

  public MyRelatedGroupsPropertyProvider() {
  }

  MyRelatedGroupsPropertyProvider(ConnectionSearchPropertyProvider connPropProv, SolrSearchServiceFactory searchServiceFactory, Repository repo) {
    this.connPropProv = connPropProv;
    this.searchServiceFactory = searchServiceFactory;
    this.repo = repo;
  }

  /**
   * Loads properties needed for myrelatedgroups.json.
   *
   * _names == the groups I am a member of
   * _contacts == the groups my contacts are a member of; does not include groups I am already a member of
   * _titles == the titles of groups in _contacts
   * _taguuids == the tag UUIDs associated to groups in _contacts
   *
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.solr.SolrSearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest, java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      // collect the names of "my groups" for exclusion in final list of groups to
      // consider
      Session session = repo.loginAdministrative();
      String user = request.getRemoteUser();
      AuthorizableManager authMgr = session.getAuthorizableManager();
      Authorizable auth = authMgr.findAuthorizable(user);
      Iterator<Group> authGroups = auth.memberOf(authMgr);

      HashSet<String> names = Sets.newHashSet();
      HashSet<String> titles = Sets.newHashSet();
      HashSet<String> tagUuids = Sets.newHashSet();
      while (authGroups.hasNext()) {
        Group g = authGroups.next();
        if ("everyone".equals(g.getId()) || StringUtils.startsWith(g.getId(), "g-contacts-")) {
          continue;
        }

        names.add(ClientUtils.escapeQueryChars(g.getId()));
        titles.add(ClientUtils.escapeQueryChars(String.valueOf(g
            .getProperty(UserConstants.GROUP_TITLE_PROPERTY))));
        String[] groupTagUuids = (String[]) g.getProperty("sakai:tag-uuid");
        if (groupTagUuids != null) {
          for (String tagUuid : groupTagUuids) {
            tagUuids.add(ClientUtils.escapeQueryChars(tagUuid));
          }
        }
      }

      Set<String> contactsGroups = getContactsGroups(request, propertiesMap, names);

      // AND (name:(${_contacts}) OR (-name:(${_names}) AND (taguuid:(${_taguuids}) OR title:(${_titles}))))
      StringBuilder groupQuery = new StringBuilder();
      if (names.size() > 0 || titles.size() > 0 || tagUuids.size() > 0 || contactsGroups.size() > 0) {
        groupQuery.append("AND (");
        if (contactsGroups.size() > 0) {
          String _contactsGroups = StringUtils.join(contactsGroups.iterator(), " OR ");
          groupQuery.append("name:(").append(_contactsGroups).append(")");
        }

        if (names.size() > 0 || tagUuids.size() > 0 || titles.size() > 0) {
          if (contactsGroups.size() > 0) {
            groupQuery.append(" OR (");
          }

          if (names.size() > 0) {
            String _names = StringUtils.join(names.iterator(), " OR ");
            groupQuery.append("-name:(").append(_names).append(")");
          }

          if (tagUuids.size() > 0 || titles.size() > 0) {
            if (names.size() > 0) {
              groupQuery.append(" AND (");
            }

            // check for tags
            if (tagUuids.size() > 0) {
              String _tagUuids = StringUtils.join(tagUuids.iterator(), " OR ");
              groupQuery.append("taguuid:(").append(_tagUuids).append(")");
            }

            // check for titles
            if (titles.size() > 0) {
              if (tagUuids.size() > 0) {
                groupQuery.append(" OR ");
              }

              String _titles = StringUtils.join(titles.iterator(), " OR ");
              groupQuery.append("title:(").append(_titles).append(")");
            }

            if (names.size() > 0) {
              groupQuery.append(")");
            }
          }

          if (contactsGroups.size() > 0) {
            groupQuery.append(")");
          }
        }
        groupQuery.append(")");
      }
      propertiesMap.put("_groupQuery", groupQuery.toString());
    } catch (SolrSearchException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (StorageClientException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Collect the groups that my contact's are members of excluding groups I'm a member of
   * 
   * @param request
   * @param propertiesMap
   * @param myGroups
   * @throws SolrSearchException
   */
  private Set<String> getContactsGroups(SlingHttpServletRequest request,
      Map<String, String> propertiesMap, HashSet<String> myGroups)
      throws SolrSearchException {
    // set the connection store location in the query.
    connPropProv.loadUserProperties(request, propertiesMap);
    String contactsQuery = MessageFormat.format(CONTACTS_QUERY_TMPL,
        propertiesMap.get(ConnectionConstants.SEARCH_PROP_CONNECTIONSTORE));

    HashSet<String> contactsGroups = Sets.newHashSet();
    SolrSearchResultSet contactsRs = searchServiceFactory.getSearchResultSet(request,
        new Query(contactsQuery));

    Iterator<Result> contactsResults = contactsRs.getResultSetIterator();
    while (contactsResults.hasNext()) {
      Result result = contactsResults.next();
      Map<String, Collection<Object>> props = result.getProperties();

      Collection<Object> userGroups = props.get("group");
      if (userGroups != null) {
        Iterator<Object> userGroupsIter = userGroups.iterator();
        while (userGroupsIter.hasNext()) {
          String userGroup = String.valueOf(userGroupsIter.next());
          if (!myGroups.contains(userGroup) && !contactsGroups.contains(userGroup)) {
            contactsGroups.add(userGroup);
          }
        }
      }
    }

    return contactsGroups;
  }
}
