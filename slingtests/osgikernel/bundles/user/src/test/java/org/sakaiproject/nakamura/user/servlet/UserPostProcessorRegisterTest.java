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
package org.sakaiproject.nakamura.user.servlet;

import junit.framework.Assert;

import static org.sakaiproject.nakamura.api.user.UserConstants.USER_POST_PROCESSOR;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.UserPostProcessor;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.Iterator;


/**
 *
 */
public class UserPostProcessorRegisterTest extends AbstractEasyMockTest {
  
  @Test
  public void testWithComponentContext() {
    ComponentContext componentContext = createNiceMock(ComponentContext.class);
    ServiceReference serviceReference = createNiceMock(ServiceReference.class);
    UserPostProcessor processor = createNiceMock(UserPostProcessor.class);
    
    
    EasyMock.expect(componentContext.locateService(
        USER_POST_PROCESSOR, serviceReference)).andReturn(processor).anyTimes();
    EasyMock.expect(serviceReference.getProperty(Constants.SERVICE_ID)).andReturn(10L).anyTimes();
    replay();
    UserPostProcessorRegister upp = new UserPostProcessorRegister();
    upp.setComponentContext(componentContext);
    upp.bindUserPostProcessor(serviceReference);
    Iterator<UserPostProcessor> i = upp.getProcessors().iterator();
    Assert.assertTrue(i.hasNext());
    Assert.assertEquals(processor, i.next());
    Assert.assertFalse(i.hasNext());
    upp.unbindUserPostProcessor(serviceReference);
    verify();
  }

  @Test
  public void testWithOutComponentContext() {
    ComponentContext componentContext = createNiceMock(ComponentContext.class);
    ServiceReference serviceReference = createNiceMock(ServiceReference.class);
    UserPostProcessor processor = createNiceMock(UserPostProcessor.class);
    
    
    EasyMock.expect(componentContext.locateService(
        USER_POST_PROCESSOR, serviceReference)).andReturn(processor).anyTimes();
    EasyMock.expect(serviceReference.getProperty(Constants.SERVICE_ID)).andReturn(10L).anyTimes();
    replay();
    UserPostProcessorRegister upp = new UserPostProcessorRegister();
    upp.bindUserPostProcessor(serviceReference);
    upp.setComponentContext(componentContext);
    Iterator<UserPostProcessor> i = upp.getProcessors().iterator();
    Assert.assertTrue(i.hasNext());
    Assert.assertEquals(processor, i.next());
    Assert.assertFalse(i.hasNext());
    upp.unbindUserPostProcessor(serviceReference);
    verify();
  }

}
