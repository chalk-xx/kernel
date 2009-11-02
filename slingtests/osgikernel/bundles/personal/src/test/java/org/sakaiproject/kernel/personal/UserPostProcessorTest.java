package org.sakaiproject.kernel.personal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PATH;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PATH;
import static org.sakaiproject.kernel.api.user.UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Test;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public class UserPostProcessorTest {

  @Test
  public void testProcessEmptyPath() throws Exception {
    testProcess("");
  }

  @Test
  public void testProcessManagerUser() throws Exception {
    testProcess(SYSTEM_USER_MANAGER_USER_PATH);
  }

  @Test
  public void testProcessManagerGroup() throws Exception {
    testProcess(SYSTEM_USER_MANAGER_GROUP_PATH);
  }

  @Test
  public void testProcessManagerUserPrefix() throws Exception {
    testProcess(SYSTEM_USER_MANAGER_USER_PREFIX);
  }

  @Test
  public void testProcessManagerGroupPrefix() throws Exception {
    testProcess(SYSTEM_USER_MANAGER_GROUP_PREFIX);
  }

  private void testProcess(String path) throws Exception {
    UserPostProcessorImpl uppi = new UserPostProcessorImpl();

    Principal principal = createMock(Principal.class);
    expect(principal.getName()).andReturn("foo");

    ArrayList<String> propNames = new ArrayList<String>();
    propNames.add("rep:userId");

    Authorizable authorizable = createMock(Authorizable.class);
    expect(authorizable.getID()).andReturn("bar").times(2);
    expect(authorizable.isGroup()).andReturn(false);
    expect(authorizable.getPrincipal()).andReturn(principal);
    expect(authorizable.getPropertyNames()).andReturn(propNames.iterator());
    expect(authorizable.getProperty("rep:userId")).andReturn(new Value[] {});

    UserManager userManager = createMock(UserManager.class);
    expect(userManager.getAuthorizable(isA(String.class))).andReturn(authorizable);

    PropertyDefinition uidDef = createMock(PropertyDefinition.class);
    expect(uidDef.isProtected()).andReturn(true);

    Property userId = createMock(Property.class);
    expect(userId.getDefinition()).andReturn(uidDef);

    Node profileNode = createMock(Node.class);
    expect(profileNode.hasProperty("sakai:privateproperties")).andReturn(false);
    expect(profileNode.hasProperty("rep:userId")).andReturn(true);
    expect(profileNode.getProperty("rep:userId")).andReturn(userId);

    Node node = createMock(Node.class);
    expect(node.hasNode("authprofile")).andReturn(true);
    expect(node.getNode("authprofile")).andReturn(profileNode);
    expect(node.getParent()).andReturn(null);
    expect(node.getPath()).andReturn("/");

    expect(profileNode.getParent()).andReturn(node);

    JackrabbitSession session = createMock(JackrabbitSession.class);
    
    expect(session.itemExists("/_user/private/62/cd/b7/02/bar/created")).andReturn(
        true);
    Node createdNode = createMock(Node.class);
    expect(session.getItem("/_user/private/62/cd/b7/02/bar/created")).andReturn(
        createdNode);
    
    Node privateNode = createMock(Node.class);
    expect(createdNode.getParent()).andReturn(privateNode);

    expect(session.getUserManager()).andReturn(userManager);
    expect(session.itemExists("/_user/public/62/cd/b7/02/bar/authprofile")).andReturn(
        true);
    expect(session.getItem("/_user/public/62/cd/b7/02/bar/authprofile")).andReturn(
        profileNode);

    


    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    expect(session.getUserID()).andReturn("admin");

    RequestPathInfo requestPathInfo = createMock(RequestPathInfo.class);
    expect(requestPathInfo.getResourcePath()).andReturn(path);

    RequestParameter requestParameter = createMock(RequestParameter.class);
    expect(requestParameter.getString()).andReturn("foo");

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResourceResolver()).andReturn(rr);
    expect(request.getRequestPathInfo()).andReturn(requestPathInfo);
    expect(request.getRequestParameter(SlingPostConstants.RP_NODE_NAME)).andReturn(
        requestParameter);
    
    expect(session.hasPendingChanges()).andReturn(false).times(1);

    List<Modification> changes = new ArrayList<Modification>();

    replay(principal, authorizable, userManager, uidDef, userId, profileNode, node,
        session, rr, requestPathInfo, requestParameter, request);
    uppi.process(session, request, changes);
  }
}
