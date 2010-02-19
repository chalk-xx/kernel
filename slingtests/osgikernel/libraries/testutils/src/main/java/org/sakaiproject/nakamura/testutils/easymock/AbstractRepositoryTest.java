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
package org.sakaiproject.nakamura.testutils.easymock;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.fs.local.FileUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * This class allows you to start up an in-memory JCR repository. This is not entirely
 * comparable to the repository we will be using in deployment.
 * 
 * ex: The security manager we have in placed will not be used. So Group resolution is
 * iffy at best.
 */
public class AbstractRepositoryTest {

  // The repository we will be starting.
  private static TransientRepository repository;

  // We keep the session so our repository doesn't automatically shuts down.
  private Session repoSession;

  // Our config we will be using.
  private static final String REPO_CONFIG = "../../libraries/testutils/src/main/resources/repository.xml";

  // Where we will store our test repository.
  private static final String REPO_DATA = "../../libraries/testutils/target/testdata/testrepository";

  @BeforeClass
  public static void beforeClass() throws IOException {
    // Clean up the previous repository data (if any..)
    File data = new File(REPO_DATA);
    System.err.println(data.getCanonicalPath());
    if (data.exists()) {
      FileUtil.delete(data);
    }

    repository = new TransientRepository(REPO_CONFIG, REPO_DATA);
  }

  @AfterClass
  public static void afterClass() throws IOException {
    // Shut down the repo.
    repository.shutdown();

    // Clean up after ourselves.
    File data = new File(REPO_DATA);
    if (data.exists()) {
      FileUtil.delete(data);
    }
  }

  /**
   * Starts the repository.
   * 
   * @throws RepositoryException
   *           Something went wrong trying to fire up the repo.
   */
  public void startRepository() throws RepositoryException {
    if (repoSession == null) {
      repoSession = repository
          .login(new SimpleCredentials("admin", "admin".toCharArray()));
      repoSession.save();
    }
  }

  /**
   * Login as an admin. You still have to start the repository yourself!
   * 
   * @return the admin {@link Session}.
   * @throws RepositoryException
   */
  public Session loginAdministrative() throws RepositoryException {
    return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
  }

  /**
   * @return The {@link TransientRepository repository} we're using.
   */
  public static TransientRepository getRepository() {
    return repository;
  }

  /**
   * @return The session that keeps the repository open.
   */
  public Session getRepoSession() {
    return repoSession;
  }

  /**
   * Create a new user in JCR.
   * 
   * @param name
   *          The name of the user.
   * @param pw
   *          The password for this user.
   * @return The {@link User user} object.
   * @throws RepositoryException
   */
  public User createUser(String name, String pw) throws RepositoryException {
    // We use the session the repository was started with to create a new user.
    Session createSession = getRepoSession();

    // Get the user managers.
    UserManager userManager = AccessControlUtil.getUserManager(createSession);

    // Create the actual user.
    User user = userManager.createUser(name, pw);

    // Save the session
    createSession.save();

    return user;
  }

  /**
   * Create a group
   * 
   * @param name
   *          The name the new group should have
   * @return The {@link Group group}.
   * @throws RepositoryException
   */
  public Group createGroup(final String name) throws RepositoryException {
    // We use the session the repository was started with to create a new user.
    Session createSession = getRepoSession();

    // Get the user managers.
    UserManager userManager = AccessControlUtil.getUserManager(createSession);

    // Create the group
    Group group = userManager.createGroup(new Principal() {

      public String getName() {
        return name;
      }
    });

    // Save the session
    createSession.save();

    return group;
  }
}
