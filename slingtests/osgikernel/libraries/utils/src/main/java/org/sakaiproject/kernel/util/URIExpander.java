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
package org.sakaiproject.kernel.util;

import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;

public interface URIExpander {

  /**
   * Expands a URI to a full JCR path.
   * ex: /_user/files/abcdefgh gets transformed into /_user/files/aa/bb/cc/dd/abcdefgh
   * @param session A JCR session.
   * @param resourceResolver A resource resolver associated to a request.
   * @param uri The uri to transform.
   * @return The full JCR path associated to this URL.
   */
  public String getJCRPathFromURI(Session session, ResourceResolver resourceResolver, String uri);
  
}
