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

import org.osgi.service.event.Event;

import javax.jcr.Node;


/**
 * Definition for handling messages that originate in the system. Messages are
 * written to known areas of JCR. Events are triggered by JCR when this content
 * appears and based on the type of message, all appropriate message handlers
 * are dispatched with the event and node of content.
 */
public interface MessageHandler {
  /**
   * The type of messages in which the handler is interested.
   *
   * @return
   */
  String getType();

  /**
   * The dispatch method called to handle a message.
   *
   * @param event
   *          The event fired by JCR.
   * @param node
   *          The node that caused the event.
   */
  void handle(Event event, Node node);
}
