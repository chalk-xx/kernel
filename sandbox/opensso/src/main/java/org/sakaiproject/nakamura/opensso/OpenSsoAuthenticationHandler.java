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
package org.sakaiproject.nakamura.opensso;

import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.trusted.AbstractAuthentication;
import org.sakaiproject.nakamura.trusted.AbstractAuthenticationHandler;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public class OpenSsoAuthenticationHandler extends AbstractAuthenticationHandler {

  /**
  *
  */
 final class OpenSsoAuthentication extends AbstractAuthentication {

   /**
    * @param request
    */
   OpenSsoAuthentication(HttpServletRequest request) {
     super(request);
   }

  /**
   * {@inheritDoc}
   * @throws RepositoryException 
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthentication#createUser(java.lang.String)
   */
  @Override
  protected final void createUser(String userName) throws RepositoryException {
    OpenSsoAuthenticationHandler.this.createUser(userName);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthentication#getUserName(javax.servlet.http.HttpServletRequest)
   */
  @Override
  protected final String getUserName(HttpServletRequest request) {
    return OpenSsoAuthenticationHandler.this.getUserName(request);
  }

 }

  private static final String OPEN_SSO_AUTHTYPE = OpenSsoAuthenticationHandler.class.getName();
  
  @Reference
  private SlingRepository repository;

  /**
   * @param userName
   * @throws RepositoryException 
   */
  private void createUser(String userName) throws RepositoryException {
    super.doCreateUser(userName);
  }

  /**
   * @param request
   * @return
   */
  private String getUserName(HttpServletRequest request) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthenticationHandler#getAuthType()
   */
  @Override
  protected String getAuthType() {
    return OPEN_SSO_AUTHTYPE;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthenticationHandler#getAuthenticationObject(javax.servlet.http.HttpServletRequest)
   */
  @Override
  protected final AbstractAuthentication createAuthenticationObject(HttpServletRequest request) {
    return new OpenSsoAuthentication(request);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.opensso.AbstractAuthenticationHandler#getRespository()
   */
  @Override
  protected SlingRepository getRespository() {
    return repository;
  }

}
