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
package org.apache.sling.jcr.jackrabbit.server.security.dynamic;

import org.apache.jackrabbit.core.security.principal.ItemBasedPrincipal;
import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.DynamicEntryCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 *
 */
public class PrincipalPathUtils {

  private static final Logger LOG = LoggerFactory.getLogger(DynamicEntryCollector.class);

  /**
   * 
   * @param o
   *          an object that implements <code>ItemBasedPrincipal</code>
   * @return The hashed path.
   */
  public static String getSubPath(Object o) {
    String sub = null;

    if (o instanceof ItemBasedPrincipal) {
      try {
        sub = ((ItemBasedPrincipal) o).getPath();
      } catch (RepositoryException e) {
        LOG.error("Failed to retrieve path from ItemBasedPrincipal.", e);
      }
    }

    return sub;
  }

}
