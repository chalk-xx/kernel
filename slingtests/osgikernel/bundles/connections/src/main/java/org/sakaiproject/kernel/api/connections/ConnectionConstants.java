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
package org.sakaiproject.kernel.api.connections;

/**
 * These are the constants related to contacts / connections for users
 */
public interface ConnectionConstants {

  /**
   * This marks the base contacts node,
   * this must match the one in /resources/SLING-INF/content/_user/contacts.json
   */
  public static final String SAKAI_CONTACTSTORE_RT = "sakai/contactstore";

  /**
   * This marks the contact user store nodes (1 per user)
   */
  public static final String SAKAI_CONTACT_USERSTORE_RT = "sakai/contactuserstore";

  /**
   * This marks the contact nodes which are stored under the contact user store nodes
   * (1 per connection, where connection is a contact user store)
   */
  public static final String SAKAI_CONTACT_USERCONTACT_RT = "sakai/contactusercontact";

  public static final String CONNECTION_OPERATION = "org.sakaiproject.kernel.connection.operation";

  public static final String SAKAI_CONNECTION_STATE = "sakai:state";
  public static final String SAKAI_CONNECTION_TYPES = "sakai:types";
  public static final String SAKAI_CONNECTION_REQUESTER = "sakai:requester";

  /**
   * 
   */
  public enum ConnectionStates {
    NONE(null), PENDING("pending"), REQUEST("requested"), ACCEPT("accepted"), REJECT("rejected"), IGNORE("ignored"), BLOCK("blocked");
    private ConnectionStates(String name) {
      this.name = name;
    }
    private final String name;
    public String toString() {
      return name;
    }
  }

  /**
   * Indicates the operations which are valid for dealing with connections
   */
  public enum ConnectionOperations {
    REQUEST("request"), ACCEPT("accept"), REJECT("reject"), IGNORE("ignore"), BLOCK("block"), CANCEL("cancel"), REMOVE("remove");
    private ConnectionOperations(String name) {
      this.name = name;
    }
    private final String name;
    public String toString() {
      return name;
    }
  }

}
