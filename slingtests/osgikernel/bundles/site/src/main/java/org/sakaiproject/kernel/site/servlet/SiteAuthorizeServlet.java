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
package org.sakaiproject.kernel.site.servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.kernel.api.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceGroupServlet</code> supports add and remove groups from the site.
 * 
 * @scr.component immediate="true" label="SiteAuthorizeServlet"
 *                description="Group authorize servlet for site service"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Supports Group association with the site."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values.0="sakai/site"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="authorize"
 */
public class SiteAuthorizeServlet extends AbstractSiteServlet {

  /**
   *
   */
  private static final long serialVersionUID = 6025706807478371356L;
  /**
   *
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SiteAuthorizeServlet.class);;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Node site = request.getResource().adaptTo(Node.class);
      Session session = site.getSession();
      UserManager userManager = AccessControlUtil.getUserManager(session);
      if (site == null) {
        response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find site node");
        return;
      }
      if (!getSiteService().isSite(site)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Location does not represent site ");
        return;
      }

      String[] addGroups = request.getParameterValues(SiteService.PARAM_ADD_GROUP);
      String[] removeGroups = request.getParameterValues(SiteService.PARAM_REMOVE_GROUP);
      if ((addGroups == null || addGroups.length == 0)
          && (removeGroups == null || removeGroups.length == 0)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either at least one "
            + SiteService.PARAM_ADD_GROUP + " or at least one "
            + SiteService.PARAM_REMOVE_GROUP + " must be specified");
        return;
      }
      Value[] groupValues = new Value[0];
      if ( site.hasProperty(SiteService.AUTHORIZABLE)) {
        Property groupProperty = site.getProperty(SiteService.AUTHORIZABLE);
        groupValues = groupProperty.getValues();
      }
      Set<String> groups = new HashSet<String>();
      Map<String, Authorizable> removed = new HashMap<String, Authorizable>();
      Map<String, Authorizable> added = new HashMap<String, Authorizable>();
      for (Value v : groupValues) {
        groups.add(v.getString());
      }
      int changes = 0;
      if (removeGroups != null) {
        for (String remove : removeGroups) {
          groups.remove(remove);
          Authorizable auth = userManager.getAuthorizable(remove);
          if (auth != null) {
            removed.put(remove, auth);
          }
          changes++;
        }
      }
      if (addGroups != null) {
        for (String add : addGroups) {
          Authorizable auth = userManager.getAuthorizable(add);
          if (auth == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The authorizable "
                + add + " does not exist, nothing added ");
            return;
          }
          added.put(add, auth);
          groups.add(add);
          changes++;
        }
      }
      String path = site.getPath();
      if (changes > 0) {

        LOGGER.info("Modifying Site ");
        // set the authorizables on the site
        site.setProperty(SiteService.AUTHORIZABLE, groups.toArray(new String[0]));

        // adjst the sites on each group added or removed.
        ValueFactory vf = session.getValueFactory();
        LOGGER.info("Removing {} Site references to Site {} ",removed.size(),site.getPath());
        
        // remove old sites.
        for (Authorizable auth : removed.values()) {
          Value[] v = auth.getProperty(SiteService.SITES);
          if (v != null) {
            List<Value> vnew = new ArrayList<Value>();
            boolean r = false;
            for (int i = 0; i < v.length; i++) {
              if (!path.equals(v[i].getString())) {
                vnew.add(v[i]);
              } else {
                LOGGER.info("Removing {}",path);
                r = true;
              }
            }
            if (r) {
              Value[] vnewa = vnew.toArray(new Value[0]);
              auth.setProperty(SiteService.SITES, vnewa);
            }
          }
        }

        LOGGER.info("Adding Site {} references to Site {} ",added.size(),site.getPath());
        // add new sites
        for (Authorizable auth : added.values()) {
          Value[] v = auth.getProperty(SiteService.SITES);
          Value[] vnew = null;
          if (v == null) {
            vnew = new Value[1];
            vnew[0] = vf.createValue(path);
            LOGGER.info("Adding Site {} to Group {} ",path,auth.getID());
          } else {
            boolean a = true;
            for (int i = 0; i < v.length; i++) {
              if (path.equals(v[i].getString())) {
                a = false;
                LOGGER.info("Site {} already is present in Group {} ",path,auth.getID());
                break;
              }
            }
            if (a) {
              LOGGER.info("Appending Site {} to Group {} ",path,auth.getID());
              vnew = new Value[v.length + 1];
              System.arraycopy(v, 0, vnew, 0, v.length);
              vnew[v.length] = vf.createValue(path);
            }

          }
          if (vnew != null) {
            auth.setProperty(SiteService.SITES, vnew);
          }
        }
        LOGGER.info("Done processing  Site {} ",path);

      } else {
        LOGGER.info("No change to  Site {} ",path);

      }

      if (session.hasPendingChanges()) {
        session.save();
      }
      return;
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(),e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to service request: " + e.getMessage());
      return;
    }

  }

}
