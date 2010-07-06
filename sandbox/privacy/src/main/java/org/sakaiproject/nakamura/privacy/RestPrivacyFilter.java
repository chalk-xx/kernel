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
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This filter enforces privacy based on a property (sakai:restrictions) on nodes. This
 * filter is mounted in the SlingBase servlet and filters all requests. If the property
 * contains noget and is the current resource, then listing is not allowed. If the none is
 * specified on a node, then there are no restrictions on the node or any child nodes. If
 * all, then no request are allowed on any node, unless the subtree contains a none
 * property. if useronly:userid then the node is or any subnode is only available to the
 * specified user.
 * </p>
 * <p>
 * Examples
 * </p>
 * <pre>
 *      /
 *       - sakai:restrictions = noget
 *   all operations on / are not allowed except for admin  
 *   
 *      /_user
 *          - sakai:restrictions = all
 *      /_user/i/ie/ieb
 *                    - sakai:restrictions = none
 *      All requests to /_user, /_user/i, /_user/i/ie are blocked, /_user/i/ie/ieb is allowed
 * </pre>
 * <p>
 * When the filter initializes it sets the root node to / property sakai:restrictions value to noget
 * No other properties are set on other nodes within the system.
 * </p>
 */
@Service(value = Filter.class)
@Component(immediate = true, metatype = false)
@Properties(value = { @Property(name = "service.description", value = "Privacy Filter"),
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "filter.scope", value = "request", propertyPrivate = true),
    @Property(name = "filter.order", intValue = { 10 }, propertyPrivate = true) })
public class RestPrivacyFilter implements Filter {

  private static final String ADMIN_USER = "admin";
  private static final String PROP_SAKAI_RESTRICTIONS = "sakai:restrictions";
  private static final String USERONLY_OPTION = "useronly:";
  private static final String NONE_OPTION = "none";
  private static final String ALL_OPTION = "all";
  private static final String NOGET_OPTION = "noget";
  private static final Logger LOGGER = LoggerFactory.getLogger(RestPrivacyFilter.class);

  @Reference
  public SlingRepository repository;

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    SlingHttpServletRequest srequest = (SlingHttpServletRequest) request;
    SlingHttpServletResponse sresponse = (SlingHttpServletResponse) response;
    Resource resource = srequest.getResource();
    if (resource != null) {
      Node resourceNode = resource.adaptTo(Node.class);
      if (isProtected(srequest, resourceNode)) {
        sresponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Resource is protected");
        return;
      } else {
        LOGGER.info("Not Protected" );
      }
    } else {
      LOGGER.info("No Resource");
    }
    chain.doFilter(request, response);

  }

  /**
   * Any node may have sakai restrictions applied to it. If it does, then the first one of
   * these found it will be acted on, with the following meanings
   * 
   * <pre>
   * - sakai:restrictions : all 
   * - sakai:restrictions : none 
   * - sakai:restrictions : useronly
   * - sakai:restrictions : noget
   * </pre>
   * 
   * @param srequest
   * @param resourceNode
   * @return
   * @throws RepositoryException
   */
  private boolean isProtected(SlingHttpServletRequest srequest, Node resourceNode) {
    try {
      if (resourceNode == null) {
        return false;
      }
      Node cnode = resourceNode;
      Session session = resourceNode.getSession();
      String currentUser = session.getUserID();
      if (ADMIN_USER.equals(currentUser)) {
        return false;
      }
      if (cnode.hasProperty(PROP_SAKAI_RESTRICTIONS)) {
        String restriction = cnode.getProperty(PROP_SAKAI_RESTRICTIONS).getString();
        if (NOGET_OPTION.equals(restriction)) {
          return true;
        }
      }
      Node rootNode = session.getRootNode();
      while (!cnode.isSame(rootNode)) {
        if (cnode.hasProperty(PROP_SAKAI_RESTRICTIONS)) {
          String restriction = cnode.getProperty(PROP_SAKAI_RESTRICTIONS).getString();
          if (ALL_OPTION.equals(restriction)) {
            return true;
          } else if (NONE_OPTION.equals(restriction)) {
            return false;
          } else if (restriction.startsWith(USERONLY_OPTION)) {
            String userName = restriction.substring(USERONLY_OPTION.length());
            if (currentUser.equals(userName)) {
              return false;
            } else {
              return true;
            }
          }
        }
        cnode = cnode.getParent();
      }
      return false;
    } catch (RepositoryException e) {
      LOGGER.warn("Privacy Filter hit problem, forcing filter: {} ", e.getMessage(), e);
      return true;
    }
  }

  public void init(FilterConfig filterConfig) throws ServletException {
    try {
      Session adminSession = repository.login();
      try {
        Node rootNode = adminSession.getRootNode();
        if (!rootNode.hasProperty(PROP_SAKAI_RESTRICTIONS)) {
          rootNode.addMixin("sakai:propertiesmix");
          rootNode.setProperty(PROP_SAKAI_RESTRICTIONS, NOGET_OPTION);
          adminSession.save();
        }
      } finally {
        adminSession.logout();
      }
    } catch (Exception e) {
      LOGGER.info("Setting Root node restriction {} ", e.getMessage(), e);
    }

  }

  public void destroy() {
  }

}
