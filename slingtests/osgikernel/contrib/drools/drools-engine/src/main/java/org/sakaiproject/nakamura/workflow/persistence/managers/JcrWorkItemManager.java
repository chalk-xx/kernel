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

import org.drools.process.instance.WorkItem;
import org.drools.process.instance.WorkItemManager;
import org.drools.runtime.process.WorkItemHandler;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class JcrWorkItemManager implements WorkItemManager {

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.WorkItemManager#getWorkItem(long)
   */
  public WorkItem getWorkItem(long arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.WorkItemManager#getWorkItems()
   */
  public Set<WorkItem> getWorkItems() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.WorkItemManager#internalAbortWorkItem(long)
   */
  public void internalAbortWorkItem(long arg0) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.WorkItemManager#internalAddWorkItem(org.drools.process.instance.WorkItem)
   */
  public void internalAddWorkItem(WorkItem arg0) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.WorkItemManager#internalExecuteWorkItem(org.drools.process.instance.WorkItem)
   */
  public void internalExecuteWorkItem(WorkItem arg0) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.runtime.process.WorkItemManager#abortWorkItem(long)
   */
  public void abortWorkItem(long id) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.runtime.process.WorkItemManager#completeWorkItem(long, java.util.Map)
   */
  public void completeWorkItem(long id, Map<String, Object> results) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.runtime.process.WorkItemManager#registerWorkItemHandler(java.lang.String, org.drools.runtime.process.WorkItemHandler)
   */
  public void registerWorkItemHandler(String workItemName, WorkItemHandler handler) {
    // TODO Auto-generated method stub

  }

}
