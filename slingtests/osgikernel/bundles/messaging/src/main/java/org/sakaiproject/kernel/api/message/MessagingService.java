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
package org.sakaiproject.kernel.api.message;

import org.apache.sling.api.resource.Resource;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

public interface MessagingService {
  /**
   * Creates a message for the current user. Will take all the resource meta
   * data from the provided resource and create a new node.
   * 
   * @param resource
   * @return
   * @throws MessagingException
   */
  public String create(Resource resource) throws MessagingException;

  /**
   * Gets the absolute path to the message store from a message. ex:
   * /_private/D0/33/E2/admin/messages
   * 
   * @param msg
   *          A message node
   * @return
   */
  public String getMessageStorePathFromMessageNode(Node msg)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException;

  /**
   * Gets the path for the message starting at the message store. ex:
   * /fd/e1/df/h1/45fsdf4sd453uy4ods4fa45r4
   * 
   * @param msg
   * @return
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws ItemNotFoundException
   * @throws AccessDeniedException
   * @throws RepositoryException
   */
  public String getMessagePathFromMessageStore(Node msg)
      throws ValueFormatException, PathNotFoundException,
      ItemNotFoundException, AccessDeniedException, RepositoryException;
}
