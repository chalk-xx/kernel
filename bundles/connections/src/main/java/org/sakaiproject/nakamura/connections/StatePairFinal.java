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
package org.sakaiproject.nakamura.connections;

import static org.sakaiproject.nakamura.api.connections.ConnectionConstants.SAKAI_CONNECTION_STATE;

import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.text.MessageFormat;


/**
 * 
 */
public class StatePairFinal implements StatePair {
  private ConnectionState thisState;
  private ConnectionState otherState;
  /**
   * 
   */
  public StatePairFinal(ConnectionState thisState, ConnectionState otherState) {

    this.thisState = thisState;
    this.otherState = otherState;
  }
  /**
   * {@inheritDoc}
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return thisState.hashCode()*1000+otherState.hashCode();
  }
  /**
   * {@inheritDoc}
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if ( obj instanceof StatePairFinal ) {
      StatePairFinal sp = (StatePairFinal) obj;
      return otherState.equals(sp.otherState) && thisState.equals(sp.thisState);
    }
    return false;
  }
 
  /**
   * {@inheritDoc}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MessageFormat.format("Local [{0}] remote [{1}]", thisState.toString(), otherState.toString());
  }
  
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.connections.StatePair#transition(org.sakaiproject.nakamura.api.lite.content.Content, org.sakaiproject.nakamura.api.lite.content.Content)
   */
  public void transition(Content thisNode, Content otherNode) {
    thisNode.setProperty(SAKAI_CONNECTION_STATE, thisState.toString());
    otherNode.setProperty(SAKAI_CONNECTION_STATE,otherState.toString());
  }

}
