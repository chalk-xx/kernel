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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import org.junit.Before;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 *
 */
public class AbstractActivityRouterTest {

  protected Node activity;
  protected String user = "jack";
  protected String path = "/sites/foo/_pages/welcome/activity";
  protected List<ActivityRoute> routes = new ArrayList<ActivityRoute>();
  protected boolean playNode = true;

  @Before
  public void setUp() throws RepositoryException {

    activity = createMock(Node.class);

    expect(activity.hasProperty(ActivityConstants.ACTIVITY_ITEM_RESOURCE_TYPE))
        .andReturn(true).anyTimes();

    Property actorProperty = createMock(Property.class);
    expect(actorProperty.getString()).andReturn(user).anyTimes();

    Value valActor = createMock(Value.class);
    expect(valActor.getString()).andReturn(user).anyTimes();
    expect(actorProperty.getValue()).andReturn(valActor).anyTimes();

    expect(activity.getProperty(ActivityConstants.PARAM_ACTOR_ID)).andReturn(
        actorProperty).anyTimes();

    expect(activity.getPath()).andReturn(path).anyTimes();
    replay(valActor, actorProperty);
    if (playNode) {
      replay(activity);
    }

  }

}
