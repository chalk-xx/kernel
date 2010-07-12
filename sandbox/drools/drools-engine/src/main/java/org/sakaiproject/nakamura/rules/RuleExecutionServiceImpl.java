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
package org.sakaiproject.nakamura.rules;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.drools.KnowledgeBase;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.StatelessKnowledgeSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.rules.RuleConstants;
import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionPreProcessor;
import org.sakaiproject.nakamura.api.rules.RuleExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * A Drools implementation of the Rule Execution Service.
 */
@Component(label = "Drools Rule Execution Service", description = "Provides Rule Execution using Drools Knowledgebases")
@Service(value = RuleExecutionService.class)
@Reference(name = "processor", bind = "bindProcesor", unbind = "unbindProcessor", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, referenceInterface = RuleExecutionPreProcessor.class)
public class RuleExecutionServiceImpl implements RuleExecutionService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RuleExecutionServiceImpl.class);

  private KnowledgeBaseFactory knowledgeBaseFactory;

  private BundleContext bundleContext;

  private Map<String, ServiceReference> processorReferences = new HashMap<String, ServiceReference>();

  private Map<String, RuleExecutionPreProcessor> processors  = new ConcurrentHashMap<String, RuleExecutionPreProcessor>();
  

  public void activate(ComponentContext context) {
    knowledgeBaseFactory = new KnowledgeBaseFactory();
    BundleContext bundleContext = context.getBundleContext();
    synchronized (processorReferences ) {
      processors.clear();
      for ( Entry<String, ServiceReference> e : processorReferences.entrySet() ) {
        RuleExecutionPreProcessor repp = (RuleExecutionPreProcessor) bundleContext.getService(e.getValue());
        if ( repp != null ) {
          processors.put(e.getKey(),repp);
        }
      }
      processorReferences.clear();
      this.bundleContext = bundleContext;
    }
  }
  
  public void deactivate(ComponentContext componentContext) {
    synchronized (processorReferences ) {
      processors.clear();
      processorReferences.clear();
      this.bundleContext = null;
    }
    knowledgeBaseFactory = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @param ruleContext
   * @see org.sakaiproject.nakamura.api.rules.RuleExecutionService#executeRuleSet(java.lang.String,
   *      org.apache.sling.api.SlingHttpServletRequest)
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> executeRuleSet(String pathToRuleSet,
      SlingHttpServletRequest request, RuleContext ruleContext) {
    ResourceResolver resourceResolver = request.getResourceResolver();
    Resource ruleSet = resourceResolver.getResource(pathToRuleSet);
    if (ruleSet != null && "sakai/rule-set".equals(ruleSet.getResourceType())) {
      try {
        Node ruleSetNode = ruleSet.adaptTo(Node.class);
        KnowledgeBase knowledgeBase = knowledgeBaseFactory.getKnowledgeBase(ruleSetNode);
        StatelessKnowledgeSession ksession = knowledgeBase.newStatelessKnowledgeSession();
        Session session = resourceResolver.adaptTo(Session.class);

        List<Command<?>> cmds = new ArrayList<Command<?>>();
        cmds.add(CommandFactory.newSetGlobal("session", session, false));
        cmds.add(CommandFactory.newSetGlobal("request", request, false));
        cmds.add(CommandFactory.newSetGlobal("resource", ruleSet, false));
        cmds.add(CommandFactory.newSetGlobal("resourceResolver", resourceResolver, false));
        cmds.add(CommandFactory.newSetGlobal("currentUser", session.getUserID(), false));
        cmds.add(CommandFactory.newSetGlobal("results", new HashMap<String, Object>(),
            true)); // add an out parameter

        // add other globals and input instances with the RuleExecutionPreProcessor ....
        RuleExecutionPreProcessor preProcessor = getProcessor(ruleSetNode);
        if (preProcessor != null) {

          Map<RulesObjectIdentifier, Object> globals = preProcessor
              .getAdditonalGlobals(ruleContext);
          for (Entry<RulesObjectIdentifier, Object> g : globals.entrySet()) {
            String in = g.getKey().getInIdentifier();
            String out = g.getKey().getOutIdentifier();
            if (out != null) {
              cmds.add(CommandFactory.newSetGlobal(in, g.getValue(), out));
            } else {
              cmds.add(CommandFactory.newSetGlobal(in, g.getValue(), false));
            }
          }

          Map<RulesObjectIdentifier, Object> inputs = preProcessor
              .getAdditonalInputs(ruleContext);
          for (Entry<RulesObjectIdentifier, Object> g : inputs.entrySet()) {
            String out = g.getKey().getOutIdentifier();
            if (out != null) {
              cmds.add(CommandFactory.newInsert(g.getValue(), out));
            } else {
              cmds.add(CommandFactory.newInsert(g.getValue()));
            }
          }
        }
        // Fire all the rules
        ExecutionResults results = ksession.execute(CommandFactory
            .newBatchExecution(cmds));

        Map<String, Object> resultsMap = (Map<String, Object>) results
            .getValue("results"); // returns the Map containing the results.
        return resultsMap;
      } catch (IllegalStateException e) {

      } catch (RepositoryException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
      } catch (IOException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
      }

    }
    return null;
  }

  /**
   * @param ruleSetNode
   * @return
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public RuleExecutionPreProcessor getProcessor(Node ruleSetNode)
      throws RepositoryException {
    if (ruleSetNode.hasProperty(RuleConstants.SAKAI_RULE_EXECUTION_PREPROCESSOR)) {
      return processors.get(ruleSetNode.getProperty(
          RuleConstants.SAKAI_RULE_EXECUTION_PREPROCESSOR).getString());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
   */
  public void bindProcessor(ServiceReference reference) {
    String name = (String) reference.getProperty(RuleConstants.PROCESSOR_NAME);
    synchronized (processorReferences ) {
      if (bundleContext == null) {
        processorReferences.put(name, reference);
      } else {
        RuleExecutionPreProcessor o = (RuleExecutionPreProcessor) bundleContext
            .getService(reference);
        processors.put(name, o);
      }
    }
  }

  public void unbindProcessor(ServiceReference reference) {
    String name = (String) reference.getProperty(RuleConstants.PROCESSOR_NAME);
    synchronized (processorReferences) {
      if (bundleContext == null) {
        processorReferences.remove(name);
      } else {
        processors.remove(name);
      }
    }
  }

}
