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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.sakaiproject.nakamura.api.rules.RuleConstants;
import org.sakaiproject.nakamura.api.rules.RuleExecutionPreProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class ExecutionPreprocessorFactory implements ServiceTrackerCustomizer {

  /**
   * 
   */
  private ServiceTracker tracker;
  private Map<String, RuleExecutionPreProcessor> processors = new ConcurrentHashMap<String, RuleExecutionPreProcessor>();
  private BundleContext context;

  /**
   * 
   */
  public ExecutionPreprocessorFactory() {
  }

  public void open(ComponentContext componentContext) {
    context = componentContext.getBundleContext();
    tracker = new ServiceTracker(context, RuleExecutionPreProcessor.class.getName(), this);
    tracker.open();
  }

  public void close() {
    tracker.close();
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
    if (ruleSetNode
        .hasProperty(RuleConstants.SAKAI_RULE_EXECUTION_PREPROCESSOR)) {
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
  public Object addingService(ServiceReference reference) {
    RuleExecutionPreProcessor o = (RuleExecutionPreProcessor) context
        .getService(reference);
    String name = (String) reference
        .getProperty(RuleConstants.PROCESSOR_NAME);
    processors.put(name, o);
    return o;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
   *      java.lang.Object)
   */
  public void modifiedService(ServiceReference reference, Object object) {
    addingService(reference);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
   *      java.lang.Object)
   */
  public void removedService(ServiceReference reference, Object object) {
    String name = (String) reference
        .getProperty(RuleConstants.PROCESSOR_NAME);
    processors.remove(name);
  }

}
