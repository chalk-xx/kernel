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
package org.sakaiproject.kernel.search;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>UserSearchServlet</code> uses nodes from the
 * 
 * @scr.component immediate="true" label="UserSearchServlet"
 *                description="a user search servlet"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Looks for users"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.paths" value="/var/search/usersearch"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.extensions" value="json"
 */
public class UserSearchServlet extends SlingAllMethodsServlet {

  private static final long serialVersionUID = 1L;

  /**  
   * @scr.reference
   */
  private SlingRepository slingRepository;

  protected NameFactory NF = NameFactoryImpl.getInstance();
  protected String SECURITY_ROOT_PATH = "/rep:security";
  protected String AUTHORIZABLES_PATH = SECURITY_ROOT_PATH + "/rep:authorizables";
  protected String USERS_PATH = AUTHORIZABLES_PATH + "/rep:users";
  protected Name NT_REP_USER = NF.create(Name.NS_REP_URI, "User");
  
  private Query buildUserQuery(QueryManager queryManager, NamePathResolver nameResolver) throws RepositoryException {
    StringBuffer stmt = new StringBuffer("/jcr:root");
    stmt.append("/rep:security/rep:authorizables/rep:users");
    stmt.append("//element(*,");
    stmt.append(nameResolver.getJCRName(NT_REP_USER));
    stmt.append(")");
    return queryManager.createQuery(stmt.toString(), Query.XPATH);
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Session securitySession = slingRepository.loginAdministrative(null);
      DefaultNamePathResolver resolver = new DefaultNamePathResolver(securitySession);
      QueryManager queryManager = securitySession.getWorkspace().getQueryManager();
      Query query = buildUserQuery(queryManager, resolver);
      QueryResult result = query.execute();

      JSONWriter write = new JSONWriter(response.getWriter());
      write.object();
      write.key("results");
      write.array();
      for (NodeIterator ni = result.getNodes(); ni.hasNext();) {
        Node resultNode = ni.nextNode();
        write.value(resultNode);
      }
      write.endArray();
      write.endObject();
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  public void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  public void unbindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = null;
  }
}
