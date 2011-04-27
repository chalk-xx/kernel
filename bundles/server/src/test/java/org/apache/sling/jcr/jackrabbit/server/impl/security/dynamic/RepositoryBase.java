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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import com.google.common.collect.Maps;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.sling.jcr.jackrabbit.server.impl.Activator;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.lite.ConfigurationImpl;
import org.sakaiproject.nakamura.lite.LoggingStorageListener;
import org.sakaiproject.nakamura.lite.authorizable.AuthorizableActivator;
import org.sakaiproject.nakamura.lite.jackrabbit.SparseRepositoryHolder;
import org.sakaiproject.nakamura.lite.storage.StorageClient;
import org.sakaiproject.nakamura.lite.storage.mem.MemoryStorageClientPool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 *
 */
public class RepositoryBase {
  private RepositoryImpl repository;
  private Activator sakaiActivator;
  private BundleContext bundleContext;
  private org.sakaiproject.nakamura.lite.RepositoryImpl sparseRepository;
  private ConfigurationImpl configuration;
  private MemoryStorageClientPool connectionPool;
  private StorageClient client;

  /**
   *
   */
  public RepositoryBase(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  public void start() throws IOException, RepositoryException {
    File home = new File("target/testrepo");
    if (home.exists()) {
      FileUtils.deleteDirectory(home);
    }
    InputStream ins = this.getClass().getClassLoader()
        .getResourceAsStream("test-repository.xml");

    try {
      setupSakaiActivator();
    } catch (ClientPoolException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (StorageClientException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (AccessDeniedException e) {
      throw new RepositoryException(e.getMessage(), e);
    } catch (ClassNotFoundException e) {
      throw new RepositoryException(e.getMessage(), e);
    }
    RepositoryConfig crc = RepositoryConfig.create(ins, home.getAbsolutePath());
    repository = RepositoryImpl.create(crc);
    Session session = repository.login(new SimpleCredentials("admin", "admin"
        .toCharArray()));
    session.getWorkspace().getNamespaceRegistry()
        .registerNamespace("sakai", "http://www.sakaiproject.org/nakamura/2.0");
    session.getWorkspace().getNamespaceRegistry()
        .registerNamespace("sling", "http://sling.apache.org/testing");
    if (session.hasPendingChanges()) {
      session.save();
    }
    session.logout();
  }

  /**
   * @throws AccessDeniedException
   * @throws StorageClientException
   * @throws ClientPoolException
   * @throws ClassNotFoundException
   * 
   */
  private void setupSakaiActivator() throws ClientPoolException, StorageClientException,
      AccessDeniedException, ClassNotFoundException {
    sakaiActivator = new Activator();
    Mockito.when(bundleContext.getProperty("sling.repository.home")).thenReturn("target/testrepo");
    Mockito.when(bundleContext.getProperty("sling.home")).thenReturn("target/testrepo");
    sakaiActivator.start(bundleContext);

    // setup the Sparse Content Repository.
    connectionPool = new MemoryStorageClientPool();
    connectionPool.activate(new HashMap<String, Object>());
    client = connectionPool.getClient();
    configuration = new ConfigurationImpl();
    Map<String, Object> properties = Maps.newHashMap();
    properties.put("keyspace", "n");
    properties.put("acl-column-family", "ac");
    properties.put("authorizable-column-family", "au");
    properties.put("content-column-family", "cn");
    configuration.activate(properties);
    AuthorizableActivator authorizableActivator = new AuthorizableActivator(client,
        configuration);
    authorizableActivator.setup();

    sparseRepository = new org.sakaiproject.nakamura.lite.RepositoryImpl();
    sparseRepository.setConfiguration(configuration);
    sparseRepository.setConnectionPool(connectionPool);
    sparseRepository.setStorageListener(new LoggingStorageListener());
    sparseRepository.activate(new HashMap<String, Object>());
    SparseRepositoryHolder.setSparseRespository(sparseRepository);
  }

  public void stop() {
    client.close();
    repository.shutdown();
    sakaiActivator.stop(bundleContext);
  }

  /**
   * @return the repository
   */
  public RepositoryImpl getRepository() {
    return repository;
  }

}
