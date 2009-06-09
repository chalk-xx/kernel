/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.server.security.dynamic;

import javax.jcr.Node;

/**
 * Provides dynamic resolution of principals based on the current context of the request.
 * This should be implemented by declarative services to enable installations to integrate
 * with dynamic sources of principals.
 */
public interface DynamicPrincipalManager {
  /**
   * Returns true if the current session has the principal in the current context.
   * 
   * @param principalName
   *          the name of the principal
   * @param aclNode the ACL node associated with the node under test
   * @return true if the user has the principal.
   */
  boolean hasPrincipalInContext(String principalName, Node aclNode);

}
