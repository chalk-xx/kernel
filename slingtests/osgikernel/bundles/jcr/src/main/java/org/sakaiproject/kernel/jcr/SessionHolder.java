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

package org.sakaiproject.kernel.jcr;

import org.apache.sling.engine.EngineConstants;
import org.sakaiproject.kernel.api.memory.ThreadBound;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

public class SessionHolder implements ThreadBound {

  private Session session = null;

  private boolean keepLoggedIn = false;

  public SessionHolder(Repository repository,
      Credentials repositoryCredentials, String workspace)
      throws LoginException, RepositoryException {
    session = repository.login(repositoryCredentials); // , workspace);
  }
  /**
   * @param currentSession
   */
  public SessionHolder(HttpSession currentSession) {
    session = (Session) currentSession.getAttribute(EngineConstants.SESSION);
  }

  /**
   * @param session2
   */
  public SessionHolder(Session session) {
    this.session = session;
  }


  public Session getSession() {
    return session;
  }

  public void unbind() {
    if (keepLoggedIn) {
      keepLoggedIn = false;
    } else if (session != null) {
      session.logout();
      session = null;
    }
  }


  public void keepLoggedIn() {
    keepLoggedIn = true;
  }



}
