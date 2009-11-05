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
package org.sakaiproject.kernel.userdirectory;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jasig.services.persondir.support.ldap.LdapPersonAttributeDao;
import org.sakaiproject.kernel.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletException;

/**
 * <p>
 * Servlet for looking up user information from various federated sources. This
 * servlet is triggered by accessing a node that has
 * resourceType="sakai/userdirectory". This node should be a user's space in
 * JCR.
 * </p>
 * <p>
 * <em>Example structure:</em><br/>
 * /_user/public/ad/80/11/98/dsf/authprofile<br/>
 * Where 'authprofile' has a property of resourceType = "sakai/user-profile" and
 * the node 'dsf' is the username.
 * </p>
 * <p>
 * Having the requested node as a first level subnode of a node that is named
 * after the username to be looked up is required.
 * </p>
 */
@SlingServlet(methods = "GET", resourceTypes = "sakai/user-profile")
public class UserDirectoryServlet extends SlingSafeMethodsServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserDirectoryServlet.class);
  private static final long serialVersionUID = 6707040084319189872L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    String path = resource.getPath();
    String[] splitPath = StringUtils.split(path, '/');
    String username = splitPath[splitPath.length - 2];

    String msg = "Getting information for [" + username + "]\n";
    LOGGER.info(msg);

    Writer writer = response.getWriter();
    writer.append(msg);

    printNode(resource, writer);

    try {
      printLdap(username);
    } catch (Exception e) {
      throw new ServletException(e.getMessage(), e);
    }
  }

  /**
   * @param resource
   * @param writer
   * @throws IOException
   */
  private void printNode(Resource resource, Writer writer) throws IOException {
    writer.append(resource.toString() + "\n");

    // get the node's properties
    Node node = (Node) resource.adaptTo(Node.class);

    try {
      if (node.hasProperties()) {
        writer.append("Properties:\n");
        PropertyIterator props = node.getProperties();
        while (props.hasNext()) {
          Property prop = props.nextProperty();
          Value[] values = JcrUtils.getValues(node, prop.getName());
          writer.append("\n" + prop.getName() + ":\n");
          for (Value value : values) {
            writer.append(value.getString() + "\n");
          }
        }
      }
    } catch (ValueFormatException e) {
      LOGGER.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private void printLdap(String username) throws Exception {
    String baseDN = "uid=sakai2-gted,ou=Local Accounts,dc=gted,dc=gatech,dc=edu";
    String name = "username";
    String password = "cta&HWR!";

    LdapTemplate ldapTemplate = new LdapTemplate();
    LdapContextSource contextSource = new LdapContextSource();
    contextSource.setBase(baseDN);
    contextSource.setUrl("r.gted.gatech.edu");
    contextSource.setUserDn(baseDN);
    contextSource.setPassword(password);

    LinkedList<String> queryAttributes = new LinkedList<String>();
    HashMap<String, Object> queryAttributeMapping = new HashMap<String, Object>();

    LdapPersonAttributeDao dao = new LdapPersonAttributeDao();
    dao.setBaseDN(baseDN);
    dao.setContextSource(contextSource);
    // dao.setDefaultAttributeName(name);
    dao.setQueryAttributeMapping(queryAttributeMapping);
    // dao.setQuery(uidQuery);
    // dao.setQueryAttributes(queryAttributes);
    // dao.setSearchControls(searchControls);
    dao.afterPropertiesSet();
  }
}
