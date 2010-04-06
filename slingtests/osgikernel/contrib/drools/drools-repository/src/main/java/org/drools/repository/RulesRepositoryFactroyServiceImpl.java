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
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;

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
      Workspace ws = session.getWorkspace();

      // no need to set it up again, skip it if it has.
      boolean registered = RulesRepositoryAdministrator.isNamespaceRegistered(session);

      if (!registered) {
        ws.getNamespaceRegistry().registerNamespace("drools", RulesRepository.DROOLS_URI);

        // Note, the order in which they are registered actually does matter !
        this.registerNodeTypesFromCndFile("/node_type_definitions/tag_node_type.cnd",
            session);
        this.registerNodeTypesFromCndFile("/node_type_definitions/state_node_type.cnd",
            session);
        this.registerNodeTypesFromCndFile(
            "/node_type_definitions/versionable_node_type.cnd", session);
        this.registerNodeTypesFromCndFile(
            "/node_type_definitions/versionable_asset_folder_node_type.cnd", session);

        this.registerNodeTypesFromCndFile("/node_type_definitions/rule_node_type.cnd",
            session);
        this.registerNodeTypesFromCndFile(
            "/node_type_definitions/rulepackage_node_type.cnd", session);

      }

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

  private void registerNodeTypesFromCndFile(String cndFileName, Session session)
      throws RulesRepositoryException, InvalidNodeTypeDefException {

    LOGGER.info("Loading " + cndFileName);
    InputStream instream = this.getClass().getResourceAsStream(cndFileName);
    LOGGER.info("Got {} ", instream);
    Reader in = new InputStreamReader(instream);
    try {
      CndImporter.registerNodeTypes(in, session);
    } catch (InvalidNodeTypeDefinitionException e) {
      LOGGER.error("Caught Exception", e);
    } catch (NodeTypeExistsException e) {
      LOGGER.error("Caught Exception", e);
    } catch (UnsupportedRepositoryOperationException e) {
      LOGGER.error("Caught Exception", e);
    } catch (ParseException e) {
      LOGGER.error("Caught Exception", e);
    } catch (RepositoryException e) {
      LOGGER.error("Caught Exception", e);
    } catch (IOException e) {
      LOGGER.error("Caught Exception", e);
    }
  }
}
