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
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.drools.KnowledgeBase;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.StatelessKnowledgeSession;
import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionPreProcessor;
import org.sakaiproject.nakamura.api.rules.RuleExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component(label = "Drools Rule Execution Service", description = "Provides Rule Execution using Drools Knowledgebases")
@Service(value = RuleExecutionService.class)
public class RuleExecutionServiceImpl implements RuleExecutionService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RuleExecutionServiceImpl.class);
  private KnowledgeBaseFactory knowledgeBaseFactory;
  private ExecutionPreprocessorFactory executionPreprocessorFactory;

  /**
   * {@inheritDoc}
   * 
   * @param ruleContext
   * @see org.sakaiproject.nakamura.api.rules.RuleExecutionService#executeRuleSet(java.lang.String,
   *      org.apache.sling.api.SlingHttpServletRequest)
   */
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
        cmds
            .add(CommandFactory.newSetGlobal("resourceResolver", resourceResolver, false));
        cmds.add(CommandFactory.newSetGlobal("currentUser", session.getUserID(), false));
        cmds.add(CommandFactory.newSetGlobal("results", new HashMap<String, Object>(),
            true)); // add an out parameter

        // add other globals and input instances with the RuleExecutionPreProcessor ....
        RuleExecutionPreProcessor preProcessor = executionPreprocessorFactory
            .getProcessor(ruleSetNode);
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
      }

    }
    return null;
  }

}
