package org.sakaiproject.kernel.user;
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




import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**

 * 
 * @scr.component inherit="true" label="%dist.events.name" description="%dist.events.description"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="service.description" value="Event Handler Listening to Authorizable Events"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="event.topics" value="org/apache/sling/jackrabbit/usermanager/event/*"

 */
public class PostUserCreationListener implements EventHandler {


  private static final Logger LOGGER = LoggerFactory.getLogger(PostUserCreationListener.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
   */
  public void handleEvent(Event event) {
    
    LOGGER.info("Got Event "+event);
  }


}
