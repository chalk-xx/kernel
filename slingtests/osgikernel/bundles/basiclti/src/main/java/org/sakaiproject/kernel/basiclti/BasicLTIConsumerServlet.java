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
package org.sakaiproject.kernel.basiclti;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.imsglobal.basiclti.BasicLTIUtil;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@SlingServlet(methods = { "GET" }, resourceTypes = { "sakai/basiclti" })
public class BasicLTIConsumerServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 5985490994324951127L;

  public static final Logger LOG = LoggerFactory
      .getLogger(BasicLTIConsumerServlet.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    LOG
        .debug("doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final Resource resource = request.getResource();
    if (resource == null) {
      sendError(HttpServletResponse.SC_NOT_FOUND,
          "Resource could not be found", new Error(
              "Resource could not be found"), response);
    }
    final Node node = resource.adaptTo(Node.class);
    final Session session = request.getResourceResolver()
        .adaptTo(Session.class);
    try {
      final String launch = node.getProperty("ltiurl").getValue().getString();
      final String secret = node.getProperty("ltisecret").getValue()
          .getString();
      final String key = node.getProperty("ltikey").getValue().getString();
      final Properties props = new Properties();
      props.setProperty("resource_link_id", "uuid");
      props.setProperty("user_id", session.getUserID()); // maybe needs to be
      // more opaque?
      props.setProperty("roles", "Learner,Mentor");
      props.setProperty("lis_person_name_given", "Jane");
      props.setProperty("lis_person_name_family", "Smith");
      props.setProperty("lis_person_name_full", "Jane Q. Smith");
      props.setProperty("lis_person_contact_email_primary",
          "jane.q.smith@school.edu");
      props.setProperty("context_id", "context_id"); // maybe should be parent
      // or site?
      props.setProperty("context_type", "CourseSection");
      props.setProperty("context_title", "Design of Personal Enviornments");
      props.setProperty("context_label", "SI182");
      props.setProperty("launch_presentation_locale", "en_US_variant");
      final Properties signedProperties = BasicLTIUtil.signProperties(props,
          launch, "POST", key, secret, "sakaiproject.org", "Sakai",
          "http://sakaiproject.org");
      final ExtendedJSONWriter writer = new ExtendedJSONWriter(response
          .getWriter());
      writer.object();
      for (final Object propkey : signedProperties.keySet()) {
        writer.key((String) propkey);
        writer.value(signedProperties.getProperty((String) propkey));
        // LOG
        // .info(propkey + "="
        // + signedProperties.getProperty((String) propkey));
      }
      writer.endObject();
    } catch (Exception e) {
      sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getLocalizedMessage(), e, response);
    }

  }

  private void sendError(int errorCode, String message, Throwable exception,
      HttpServletResponse response) {
    if (!response.isCommitted()) {
      try {
        LOG.error(errorCode + ": " + message, exception);
        response.sendError(errorCode, message);
      } catch (IOException e) {
        throw new Error(e);
      }
    } else {
      LOG.error(errorCode + ": " + message, exception);
      throw new Error(message, exception);
    }
  }
}
