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
package org.sakaiproject.nakamura.rules;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.rules.RuleContext;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;


public class RuleExecutionServiceImpleTest {

  @Mock
  private RuleContext ruleContext;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Resource ruleSet;
  @Mock
  private Node ruleSetNode;
  @Mock
  private ComponentContext context;
  @Mock
  private BundleContext bundleContext;
  @Mock
  private NodeIterator nodeIterator;
  @Mock
  private Node packageNode;
  @Mock
  private NodeType packageNodeType;
  @Mock
  private Property property;
  @Mock
  private Node packageFileNode;
  @Mock
  private Property packageFileBody;
  @Mock
  private Binary binary;

  public RuleExecutionServiceImpleTest() {
    MockitoAnnotations.initMocks(this);
  }
  
  @Test
  public void testRuleExecution() throws RepositoryException {
    String path = "/test/ruleset";
    
    Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.getResource(path)).thenReturn(ruleSet);
    Mockito.when(ruleSet.getResourceType()).thenReturn("sakai/rule-set");
    Mockito.when(ruleSet.adaptTo(Node.class)).thenReturn(ruleSetNode);
    Mockito.when(context.getBundleContext()).thenReturn(bundleContext);
    Mockito.when(ruleSetNode.getNodes()).thenReturn(nodeIterator);
    Mockito.when(nodeIterator.hasNext()).thenReturn(true, false);
    Mockito.when(nodeIterator.nextNode()).thenReturn(packageNode);
    
    Mockito.when(packageNode.getPrimaryNodeType()).thenReturn(packageNodeType);
    Mockito.when(packageNodeType.getName()).thenReturn("nt:file");
        
    Mockito.when(packageNode.getPath()).thenReturn("/test/ruleset");
    Mockito.when(packageNode.getProperty(Property.JCR_LAST_MODIFIED)).thenReturn(property);
    Calendar lastModified = GregorianCalendar.getInstance();
    lastModified.setTime(new Date());
    Mockito.when(property.getDate()).thenReturn(lastModified);
    
    Mockito.when(packageNode.getNode(Node.JCR_CONTENT)).thenReturn(packageFileNode);
    
    Mockito.when(packageFileNode.getProperty(Property.JCR_DATA)).thenReturn(packageFileBody);
    
    Mockito.when(packageFileBody.getBinary()).thenReturn(binary);
    
    
    
    Mockito.when(binary.getStream()).thenAnswer(new Answer<InputStream>() {

      public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return  this.getClass().getResourceAsStream("/testruleset.drl");
      }
    });
    
    
    
    
    RuleExecutionServiceImpl res = new RuleExecutionServiceImpl();
    res.activate(context);
    Map<String, Object> result = res.executeRuleSet(path, request, ruleContext);
    
    Assert.assertNotNull(result);
    res.deactivate(context);
  }
  
}
