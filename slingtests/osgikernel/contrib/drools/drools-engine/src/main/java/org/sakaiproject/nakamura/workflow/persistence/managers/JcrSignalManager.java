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

import org.drools.process.instance.event.SignalManager;
import org.drools.runtime.process.EventListener;

/**
 *
 */
public class JcrSignalManager implements SignalManager {

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.event.SignalManager#addEventListener(java.lang.String, org.drools.runtime.process.EventListener)
   */
  public void addEventListener(String arg0, EventListener arg1) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.event.SignalManager#removeEventListener(java.lang.String, org.drools.runtime.process.EventListener)
   */
  public void removeEventListener(String arg0, EventListener arg1) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.event.SignalManager#signalEvent(java.lang.String, java.lang.Object)
   */
  public void signalEvent(String arg0, Object arg1) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * @see org.drools.process.instance.event.SignalManager#signalEvent(long, java.lang.String, java.lang.Object)
   */
  public void signalEvent(long arg0, String arg1, Object arg2) {
    // TODO Auto-generated method stub

  }

}
