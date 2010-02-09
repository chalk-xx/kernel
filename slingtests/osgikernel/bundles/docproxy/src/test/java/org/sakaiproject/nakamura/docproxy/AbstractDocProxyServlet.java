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
package org.sakaiproject.nakamura.docproxy;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.expect;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_LOCATION;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_PROCESSOR;

import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.docproxy.disk.DiskProcessor;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 *
 */
public class AbstractDocProxyServlet extends AbstractEasyMockTest {
  protected Node proxyNode;
  protected String currPath;
  protected ExternalRepositoryProcessorTracker tracker;
  protected BundleContext bundleContext;
  protected ComponentContext componentContext;
  protected DiskProcessor diskProcessor;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    // Setup a proxy node.
    String readmePath = getClass().getClassLoader().getResource("README").getPath();
    currPath = readmePath.substring(0, readmePath.lastIndexOf("/"));

    proxyNode = createMock(Node.class);
    // We use the default disk processor.
    Property processorProperty = createMock(Property.class);
    expect(processorProperty.getString()).andReturn("disk");

    // Our resource type
    Property resourceTypeProp = createMock(Property.class);
    expect(resourceTypeProp.getString()).andReturn(
        DocProxyConstants.RT_EXTERNAL_REPOSITORY);

    // Repository location
    Property locationProp = createMock(Property.class);
    expect(locationProp.getString()).andReturn(currPath).anyTimes();

    expect(proxyNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).andReturn(true)
        .anyTimes();
    expect(proxyNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).andReturn(
        resourceTypeProp).anyTimes();
    expect(proxyNode.getProperty(REPOSITORY_PROCESSOR)).andReturn(processorProperty)
        .anyTimes();
    expect(proxyNode.getPath()).andReturn("/docproxy/disk").anyTimes();
    expect(proxyNode.getProperty(REPOSITORY_LOCATION)).andReturn(locationProp).anyTimes();
    expect(proxyNode.isNode()).andReturn(true).anyTimes();

    // Mock up the tracker
    diskProcessor = new DiskProcessor();
    bundleContext = expectServiceTrackerCalls(ExternalRepositoryProcessor.class.getName());

    componentContext = EasyMock.createMock(ComponentContext.class);
    expect(componentContext.getBundleContext()).andReturn(bundleContext);
    EasyMock.replay(componentContext);

    tracker = new ExternalRepositoryProcessorTracker(bundleContext,
        ExternalRepositoryProcessor.class.getName(), null);

    tracker.putProcessor(diskProcessor, "disk");

  }
}
