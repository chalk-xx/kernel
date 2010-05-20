package org.drools.repository;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
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
 * This contains code to initialise the repository for jackrabbit.
 * This is mostly a collection of utilities. 
 * Any jackrabbit specific code needs to go in here.
 */
public class JackrabbitRepositoryConfigurator implements JCRRepositoryConfigurator {

    private static final Logger log = LoggerFactory.getLogger(JackrabbitRepositoryConfigurator.class);        
    
    /* (non-Javadoc)
     * @see org.drools.repository.RepositoryConfigurator#getJCRRepository()
     */
    public Repository getJCRRepository(String repoRootDir) {
            if (repoRootDir == null) {
                return new TransientRepository();
            } else { 
                return new TransientRepository(repoRootDir + "/repository.xml", repoRootDir);
            }
    }
    
  
    
    /* (non-Javadoc)
     * @see org.drools.repository.RepositoryConfigurator#setupRulesRepository(javax.jcr.Session)
     */
    public void setupRulesRepository(Session session) throws RulesRepositoryException {
        System.out.println("Setting up the repository, registering node types etc.");
        try {
            Node root = session.getRootNode();
            Workspace ws = session.getWorkspace();

            //no need to set it up again, skip it if it has.
            boolean registered = RulesRepositoryAdministrator.isNamespaceRegistered( session );

            if (!registered) {
                ws.getNamespaceRegistry().registerNamespace("drools", RulesRepository.DROOLS_URI);
                
                //Note, the order in which they are registered actually does matter !
                this.registerNodeTypesFromCndFile("/SLING-INF/nodetypes/tag_node_type.cnd", session);
                this.registerNodeTypesFromCndFile("/SLING-INF/nodetypes/state_node_type.cnd", session);
                this.registerNodeTypesFromCndFile("/SLING-INF/nodetypes/versionable_node_type.cnd", session);
                this.registerNodeTypesFromCndFile("/SLING-INF/nodetypes/versionable_asset_folder_node_type.cnd", session);
                
                this.registerNodeTypesFromCndFile("/SLING-INF/nodetypes/rule_node_type.cnd", session);
                this.registerNodeTypesFromCndFile("/SLING-INF/nodetypes/rulepackage_node_type.cnd", session);
             
            }
            
            // Setup the rule repository node
            Node repositoryNode = RulesRepository.addNodeIfNew(root, RulesRepository.RULES_REPOSITORY_NAME, "nt:folder");
                    

            
            // Setup the RulePackageItem area        
            RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.RULE_PACKAGE_AREA, "nt:folder");
            
            // Setup the Snapshot area        
            RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.PACKAGE_SNAPSHOT_AREA, "nt:folder");
                        
            //Setup the Cateogry area                
            RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.TAG_AREA, "nt:folder");
            
            //Setup the State area                
            RulesRepository.addNodeIfNew(repositoryNode, RulesRepository.STATE_AREA, "nt:folder");
            
            //and we need the "Draft" state
            RulesRepository.addNodeIfNew( repositoryNode.getNode( RulesRepository.STATE_AREA ), StateItem.DRAFT_STATE_NAME, StateItem.STATE_NODE_TYPE_NAME );
            
            session.save();                        
        }
        catch(Exception e) {
            log.error("Caught Exception", e);
            System.err.println(e.getMessage());
            throw new RulesRepositoryException(e);
        }
    }
    
    private void registerNodeTypesFromCndFile(String cndFileName, Session session) throws RulesRepositoryException, InvalidNodeTypeDefException {
          
          log.info("Loading "+cndFileName);
          InputStream instream = this.getClass().getResourceAsStream(cndFileName);
          log.info("Got {} ",instream);
          Reader in = new InputStreamReader(instream);
          try {
            CndImporter.registerNodeTypes(in, session);
          } catch (InvalidNodeTypeDefinitionException e) {
            log.error("Caught Exception", e);
          } catch (NodeTypeExistsException e) {
            log.error("Caught Exception", e);
          } catch (UnsupportedRepositoryOperationException e) {
            log.error("Caught Exception", e);
          } catch (ParseException e) {
            log.error("Caught Exception", e);
          } catch (RepositoryException e) {
            log.error("Caught Exception", e);
          } catch (IOException e) {
            log.error("Caught Exception", e);
          }
    }    
    
    
}
