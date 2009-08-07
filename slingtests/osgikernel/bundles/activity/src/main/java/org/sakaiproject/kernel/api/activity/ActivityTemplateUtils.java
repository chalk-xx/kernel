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
package org.sakaiproject.kernel.api.activity;

//import org.apache.sling.api.SlingHttpServletRequest;
//import org.apache.sling.api.resource.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Iterator;
//import java.util.Locale;
//import java.util.Set;
//
//import javax.jcr.Node;
//import javax.jcr.PathNotFoundException;
//import javax.jcr.Property;
//import javax.jcr.RepositoryException;
//import javax.jcr.Session;

/**
 *
 */
public class ActivityTemplateUtils {
  // private static final Logger LOG =
  // LoggerFactory.getLogger(ActivityTemplateUtils.class);

  /**
   * Since no language is specified, the current language will attempt to be determined.
   * If it cannot be determined, the default template will be returned.
   * 
   * @param applicationId
   * @param templateId
   * @return
   */
  // String getTemplate(SlingHttpServletRequest request, String applicationId,
  // String templateId) {
  // return this.getTemplate(request, applicationId, templateId, null);
  // }

  /**
   * If for some reason the requested language cannot be found, the default language will
   * be returned.
   * 
   * @param applicationId
   * @param templateId
   * @param locale
   *          The language argument is a valid ISO Language Code. These codes are the
   *          lower-case, two-letter codes as defined by ISO-639. You can find a full list
   *          of these codes at a number of sites, such as: <a
   *          href="http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt"
   *          >http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt</a>
   * @param locale
   *          The country argument is a valid ISO Country Code. These codes are the
   *          upper-case, two-letter codes as defined by ISO-3166. You can find a full
   *          list of these codes at a number of sites, such as: <a
   *          href="http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html"
   *          >http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html</a>
   * @see Locale
   * @return
   */
  // String getTemplate(SlingHttpServletRequest request, String applicationId,
  // String templateId, String locale) {
  // // TODO finish this method
  // String templateRoot = ActivityConstants.TEMPLATE_ROOTFOLDER + "/" + applicationId
  // + "/" + templateId;
  // String template = null;
  // Session session = null;
  // if (locale == null || "".equals(locale)) {
  // locale = getLocale(request);
  // }
  // try {
  // String path = templateRoot + "_" + locale;
  // session = request.getResourceResolver().adaptTo(Session.class);
  // Node node = (Node) session.getItem(path);
  // } catch (PathNotFoundException e) { // could not find template; fall back to default
  // // language (i.e. no locale)
  // String path = templateRoot;
  // try {
  // Node node = (Node) session.getItem(path);
  // } catch (PathNotFoundException e1) {
  // LOG.error("Could not locate a template for " + applicationId + "/" + templateId
  // + "*", e1);
  // } catch (RepositoryException e1) {
  // LOG.error(e.getMessage(), e1);
  // }
  // } catch (RepositoryException e) {
  // LOG.error(e.getMessage(), e);
  // }
  // return template;
  // }

  // public String getLocale(SlingHttpServletRequest request) {
  // Locale locale = Locale.getDefault();
  // try {
  // Resource me = request.getResourceResolver().getResource("/system/me");
  // Node meNode = me.adaptTo(Node.class);
  // for (Iterator<Property> iterator = meNode.getProperties(); iterator.hasNext();) {
  // Property prop = iterator.next();
  // LOG.debug(prop.getName() + "=" + prop.getValue().toString());
  // }
  // Property p = meNode.getProperty("locale");
  // if (p != null) {
  // String[] s = p.getValue().getString().split("_");
  // locale = new Locale(s[0], s[1]);
  // }
  // } catch (RepositoryException e) {
  // LOG.error(e.getMessage(), e);
  // }
  // LOG.debug("locale.toString()=" + locale.toString());
  // return locale.toString();
  // }

  // public boolean validateTemplate(SlingHttpServletRequest request, String template) {
  // return false;
  // }

  /**
   * Parse a template and extract an Array of Strings which contains the macros (i.e.
   * keys) required by the template.
   * 
   * @param template
   *          The template itself (i.e. not a reference)
   * @return
   */
  // private Set<String> getMacros(String template) {
  // // TODO finish this method
  // return null;
  // }

}
