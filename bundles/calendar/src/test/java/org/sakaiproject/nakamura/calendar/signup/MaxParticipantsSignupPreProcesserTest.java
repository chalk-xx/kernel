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
package org.sakaiproject.nakamura.calendar.signup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_EVENT_SIGNUP_PARTICIPANT_RT;
import static org.sakaiproject.nakamura.calendar.signup.MaxParticipantsSignupPreProcessors.SAKAI_EVENT_MAX_PARTICIPANTS;

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.calendar.CalendarException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class MaxParticipantsSignupPreProcesserTest {


  @Before
  public void setUp() {
    // reimplement
  }

  @Test
  public void testNoCheckNeeded() {
    // reimplement
  }

  @Test
  public void testMax() throws RepositoryException {
    // reimplement
  }

  @Test
  public void testException() throws RepositoryException {
    // reimplement

  }
}
