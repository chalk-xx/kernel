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
package org.sakaiproject.nakamura.api.workflow;

import javax.jcr.Session;

/**
 *
 */
public interface WorkflowConstants {

  public static final String SESSION_STORAGE_PREFIX = "/var/workflow/sessions/";
 
  public static final String PROCESS_INSTANCE_EVENT_STORAGE_PREFIX = "/var/workflow/signal/";

  public static final String WORK_ITEM_STORAGE_PREFIX = "/var/workflow/item/";

  public static final String PROCESS_INSTANCE_STORAGE_PREFIX = "/var/workflow/process/";

  public static final String PR_PROCESS_INSTANCE_ID = "sakai:workflow-process-instance-id";

  public static final String PR_PROCESS_ID = "sakai:workflow-process-id";

  public static final String PR_STARTDATE = "sakai:workflow-start-date";

  public static final String PR_LAST_READ_DATE = "sakai:workflow-lastread-date";

  public static final String PR_PROCESS_INSTANCE_LAST_MODIFICATION_DATE = "sakai:workflow-lastmodified-date";

  public static final String PR_PROCESS_INSTANCE_STATE = "sakai:workflow-process-instance-state";

  public static final String PR_PROCESS_INSTANCE_EVENT_TYPE = "sakai:workflow-event-type";
  
  public static final String PR_PROCESS_INSTANCE_EVENT_TYPES = "sakai:workflow-event-types";

  public static final String PR_WORKITEM_NAME = "sakai:workflow-item-name";

  public static final String PR_WORKITEM_CREATION_DATE = "sakai:workflow-create-date";

  public static final String PR_WORKITEM_STATE = "sakai:workflow-item-state";

  public static final String SESSION_IDENTIFIER = Session.class.getName();

}
