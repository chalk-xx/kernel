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
package org.sakaiproject.nakamura.site;

import static org.junit.Assert.assertNull;

import org.apache.jackrabbit.api.jsr283.Session;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.RepositoryException;

@RunWith(MockitoJUnitRunner.class)
public class TestSiteAuthz {
  @Mock
  private SlingRepository repository;
  @Mock
  private Session session;

  @Test
  public void testNoAuthzConf() throws RepositoryException {
    MockNode site = new MockNode("/sites/testsite");
    SiteAuthz siteAuthz = new SiteAuthz(site);
    siteAuthz.initAccess("testuser");
    assertNull(siteAuthz.getMaintenanceRole());
    siteAuthz.deletionPostProcess(session, repository);
  }
}
