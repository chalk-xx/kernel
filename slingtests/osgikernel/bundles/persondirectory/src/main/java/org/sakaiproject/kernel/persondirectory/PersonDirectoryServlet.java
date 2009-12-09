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
package org.sakaiproject.kernel.persondirectory;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Servlet for looking up person information from various federated sources.
 * This servlet is triggered by accessing a node that has
 * resourceType="sakai/user-profile". This node should be a user's space in JCR.
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
@SlingServlet(methods = "GET", selectors = "profile", extensions = "json")
@ServiceDocumentation(name = "Person Directory Servlet", description = "Servlet for looking up user "
    + "information from various federated sources. This servlet is triggered by accessing a node "
    + "that has resourceType=\"sakai/user-profile\". This node should be a user's space in JCR.", methods = { @ServiceMethod(name = "GET", description = "GETs to nodes of type "
    + "\"sakai/user-profile\" will trigger this servlet to produce person information.") })
public class PersonDirectoryServlet extends SlingSafeMethodsServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersonDirectoryServlet.class);
  private static final long serialVersionUID = 6707040084319189872L;

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
    LOGGER.debug("Binding provider: {}", provider.getClass().getName());
    providers.add(provider);
  }

  /**
   * Unbind a provider from this component.
   *
   * @param provider
   *          The provider to unbind.
   */
  protected void unbindProvider(PersonProvider provider) {
    LOGGER.debug("Unbinding provider: {}", provider.getClass().getName());
    providers.remove(provider);
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    Node node = (Node) resource.adaptTo(Node.class);

    try {
      String uid = node.getName();
      LOGGER.info("Getting information for [" + uid + "]");

      Person person = getPerson(uid, node);
      if (person != null) {
        response.setStatus(HttpServletResponse.SC_OK);
        Map<String, String[]> attrs = person.getAttributes();
        Writer writer = response.getWriter();
        JSONWriter jsonWriter = new JSONWriter(writer);
        jsonWriter.object();
        for (Map.Entry<String, String[]> attr : attrs.entrySet()) {
          jsonWriter.key(attr.getKey());
          jsonWriter.array();
          for (String val : attr.getValue()) {
            jsonWriter.value(val);
          }
          jsonWriter.endArray();
        }
        jsonWriter.endObject();
      } else {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      }
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
    } catch (PersonProviderException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Get a person from providers.
   *
   * @param uid
   * @param node
   * @throws PersonProviderException
   */
  protected Person getPerson(String uid, Node node) throws PersonProviderException {
    PersonImpl retPerson = null;
    for (PersonProvider provider : providers) {
      Person p = provider.getPerson(uid, node);
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
