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
package org.sakaiproject.kernel.api.messaging;

import javax.jcr.Node;

/**
 *
 */
public interface MessageConverter {
  String toString(Message msg) throws ConversionException;

  Message toMessage(String json) throws ConversionException;

  /**
   * Convert a message to JCR nodes. The conversion traverses all parts of all
   * messages and sets up a similar node structure.
   *
   * @param node
   * @param msg
   * @throws ConversionException
   */
  public void toNode(Node node, Message msg) throws ConversionException;
}
