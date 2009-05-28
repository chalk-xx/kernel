package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class TestSitePostProcessor {

  private ArrayList<Modification> changes;
  private Item item;
  private Session session;
  private ResourceResolver resolver;
  private SlingHttpServletRequest request;
  private SitePostProcessor processor;
  private String itemPath = "/some/string";

  @Before
  public void setUp()
  {
    processor = new SitePostProcessor();
    request = createMock(SlingHttpServletRequest.class);
    resolver = createMock(ResourceResolver.class);
    session = createMock(Session.class);
    item = createMock(Item.class);
    
    expect(request.getResourceResolver()).andReturn(resolver);
    expect(resolver.adaptTo(eq(Session.class))).andReturn(session);
    
    Modification modification = new Modification(ModificationType.MODIFY, itemPath, null);
    changes = new ArrayList<Modification>();
    changes.add(modification);    
  }
  
  @Test
  public void testModifySite() throws PathNotFoundException, RepositoryException
  {    
    expect(session.itemExists(eq(itemPath))).andReturn(true);
    expect(session.getItem(eq(itemPath))).andReturn(item);
    expect(item.isNode()).andReturn(true);
    checkSatisfied();    
  }

  @Test
  public void testModifySiteProperty() throws PathNotFoundException, RepositoryException
  {  
    expect(session.itemExists(eq(itemPath))).andReturn(true);
    expect(session.getItem(eq(itemPath))).andReturn(item);
    expect(item.isNode()).andReturn(false);
    checkSatisfied();    
  }

  @Test
  public void testModifySiteException() throws PathNotFoundException, RepositoryException
  {    
    expect(session.itemExists(eq(itemPath))).andThrow(new RepositoryException("Exceptional"));
    checkSatisfied();    
  }

  private void checkSatisfied() {
    replay(request, resolver, session, item);
    processor.doProcess(request, changes);
    verify(request, resolver, session, item);    
  }
  
}
