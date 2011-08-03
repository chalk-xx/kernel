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
package org.sakaiproject.nakamura.activity.routing;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.activity.AbstractActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityRouter;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This router will deliver an activity to the acted-upon object's feed.
 */
@Component(immediate = true, enabled = true)
@Service(value = ActivityRouter.class)
public class SelfActivityRouter implements ActivityRouter {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SelfActivityRouter.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.activity.ActivityRouter#getPriority()
   */
  public int getPriority() {
    return 50;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.activity.ActivityRouter#route(javax.jcr.Node,
   *      java.util.List)
   */
  public void route(Node activity, List<ActivityRoute> routes) {
    try {
      String self = activity.getProperty("sakai:activity-source")
          .getString();
      String path = StorageClientUtils.newPath(self, "activityFeed");
      ActivityRoute route = new AbstractActivityRoute(path) {
      };
      routes.add(route);
    } catch (RepositoryException e) {

      LOGGER.error(
          "Exception when trying to deliver an activity.",
          e);
    }
  }

  public void route(Content activity, List<ActivityRoute> routes, Session adminSession) {
    String self = (String) activity.getProperty("sakai:activity-source");
    String path = StorageClientUtils.newPath(self, "activityFeed");
    ActivityRoute route = new AbstractActivityRoute(path) {
    };
    routes.add(route);    
  }

}
