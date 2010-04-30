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
package org.sakaiproject.nakamura.workflow.persistence.managers;

import org.drools.WorkingMemory;
import org.drools.process.instance.ProcessInstance;
import org.drools.process.instance.ProcessInstanceManager;

import java.util.Collection;

/**
 *
 */
public class JcrProcessInstanceManager implements ProcessInstanceManager {

  /**
   * @param workingMemory
   */
  public JcrProcessInstanceManager(WorkingMemory workingMemory) {
    // TODO Auto-generated constructor stub
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.ProcessInstanceManager#addProcessInstance(org.drools.process.instance.ProcessInstance)
   */
  public void addProcessInstance(ProcessInstance arg0) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.ProcessInstanceManager#getProcessInstance(long)
   */
  public ProcessInstance getProcessInstance(long arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.ProcessInstanceManager#getProcessInstances()
   */
  public Collection<ProcessInstance> getProcessInstances() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.ProcessInstanceManager#internalAddProcessInstance(org.drools.process.instance.ProcessInstance)
   */
  public void internalAddProcessInstance(ProcessInstance arg0) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.ProcessInstanceManager#internalRemoveProcessInstance(org.drools.process.instance.ProcessInstance)
   */
  public void internalRemoveProcessInstance(ProcessInstance arg0) {
    // TODO Auto-generated method stub
    
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.ProcessInstanceManager#removeProcessInstance(org.drools.process.instance.ProcessInstance)
   */
  public void removeProcessInstance(ProcessInstance arg0) {
    // TODO Auto-generated method stub
    
  }

}
