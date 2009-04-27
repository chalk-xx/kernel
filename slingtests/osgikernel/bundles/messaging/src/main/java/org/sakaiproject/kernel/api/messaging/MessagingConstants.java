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

/**
 * Common constants used in the messaging bundle.
 */
public interface MessagingConstants {
  /**
   * Property name for node labeling.
   */
  String JCR_LABELS = "sakaijcr:labels";

  /**
   * Property value for 'inbox' label.
   */
  String LABEL_INBOX = "inbox";

  /**
   * JCR folder name for message outbox.
   */
  String FOLDER_OUTBOX = "outbox";

  /**
   * JCR folder name for messages.
   */
  String FOLDER_MESSAGES = "messages";

  /**
   * Property name for the JMS broker URL.
   */
  String JMS_BROKER_URL = "jms.brokerurl";

  /**
   * Property name for the JMS email type.
   */
  String JMS_EMAIL_TYPE = "jms.email.type";

  /**
   * Property name for the JMS email queue name.
   */
  String JMS_EMAIL_QUEUE = "jms.email.queue";
}
