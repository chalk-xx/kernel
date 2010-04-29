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
package org.drools.repository;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * Initializes the Drools area of the the repository.
 */
@Component
public class RulesRepositoryFactroyServiceImpl implements JCRRepositoryConfigurator, RulesRepositoryFactoryService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RulesRepositoryFactroyServiceImpl.class);

  @Reference
  protected SlingRepository slingRepository;

  protected void activate(ComponentContext context) {
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      setupRulesRepository(session);
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    } finally {
      session.logout();
    }
  }

  protected void deactivate(ComponentContext context) {

  }

  public RulesRepository getRulesRepository(Session session) {
    return new RulesRepository(session);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.repository.JCRRepositoryConfigurator#getJCRRepository(java.lang.String)
   */
  public Repository getJCRRepository(String repositoryRootDirectory) {
    return slingRepository;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.drools.repository.JCRRepositoryConfigurator#setupRulesRepository(javax.jcr.Session)
   */
  public void setupRulesRepository(Session session) throws RulesRepositoryException {
    System.out.println("Setting up the repository, registering node types etc.");
    try {
      Node root = session.getRootNode();

      // Setup the rule repository node
      Node repositoryNode = RulesRepository.addNodeIfNew(root,
          RulesRepository.RULES_REPOSITORY_NAME, "nt:folder");

      // Setup the RulePackageItem area
      RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.RULE_PACKAGE_AREA,
          "nt:folder");

      // Setup the Snapshot area
      RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.PACKAGE_SNAPSHOT_AREA,
          "nt:folder");

      // Setup the Cateogry area
      RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.TAG_AREA, "nt:folder");

      // Setup the State area
      RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.STATE_AREA,
          "nt:folder");

      // and we need the "Draft" state
      RulesRepository.addNodeIfNew(repositoryNode.getNode(RulesRepository.STATE_AREA),
          StateItem.DRAFT_STATE_NAME, StateItem.STATE_NODE_TYPE_NAME);

      session.save();
    } catch (Exception e) {
      LOGGER.error("Caught Exception", e);
      System.err.println(e.getMessage());
      throw new RulesRepositoryException(e);
    }
  }

}
