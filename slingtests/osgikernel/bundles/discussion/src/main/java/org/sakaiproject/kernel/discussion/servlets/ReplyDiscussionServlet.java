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
package org.sakaiproject.kernel.discussion.servlets;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.kernel.api.discussion.DiscussionException;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;

/**
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/discussionpost"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" values.0="reply"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 *                bind="bindDiscussionManager" unbind="unbindDiscussionManager"
 */
public class ReplyDiscussionServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -8678619138387662438L;
  private DiscussionManager discussionManager;

  public void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  public void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    try {
      // Get the post we are replying on and create the reply.
      Resource post = request.getResource();
      discussionManager.reply(request.getParameterMap(), post);
    } catch (DiscussionException ex) {
      response.sendError(ex.getCode(), ex.getMessage());
    }
  }
}
