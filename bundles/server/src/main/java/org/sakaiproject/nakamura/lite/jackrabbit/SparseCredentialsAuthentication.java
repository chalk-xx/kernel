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
package org.sakaiproject.nakamura.lite.jackrabbit;

import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.util.Text;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

public class SparseCredentialsAuthentication implements Authentication {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SparseCredentialsAuthentication.class);
  private Authenticator authenticator;
  private User user;

  public SparseCredentialsAuthentication(User user, Authenticator authenticator) {
    this.user = user;
    this.authenticator = authenticator;
  }

  public boolean canHandle(Credentials credentials) {
    return (credentials instanceof SimpleCredentials);
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
      String testUserId = simpleCredentials.getUserID();
      if (testUserId != null && testUserId.equals(user.getId())) {
        String password = new String(simpleCredentials.getPassword());
        User user = authenticator.authenticate(simpleCredentials.getUserID(),
            digestPassword(password));
        if (user == null) {
          user = authenticator.authenticate(simpleCredentials.getUserID(), password);

        }
        LOGGER.debug("+++++++++++ Login to {} {}", simpleCredentials.getUserID(),
            user == null ? "FAILED Password was " : "OK " + user.getId());
        return user != null;
      }
    }
    LOGGER.info("--------- LOGIN FAILED Credentials: {} ", credentials);
    return false;
  }

  protected String digestPassword(String pwd) throws IllegalArgumentException {
    try {
      StringBuffer password = new StringBuffer();
      password.append("{sha1}");
      password.append(Text.digest("sha1", pwd.getBytes("UTF-8")));
      return password.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e.toString());
    }
  }

}
