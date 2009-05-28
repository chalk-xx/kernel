package org.sakaiproject.kernel.site.servlet;

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

public abstract class AbstractSiteServiceServletTest extends AbstractSiteServiceTest {

  protected SlingHttpServletRequest request;
  protected SlingHttpServletResponse response;
  protected JackrabbitSession session;

  protected abstract void makeRequest() throws ServletException, IOException;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    request = createMock(SlingHttpServletRequest.class);
    response = createMock(SlingHttpServletResponse.class);
    session = createMock(JackrabbitSession.class);
    expect(session.getUserManager()).andReturn(userManager).anyTimes();
  }

  public byte[] makeGetRequestReturningBytes() throws IOException, ServletException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    expect(response.getWriter()).andReturn(writer);    
    makeRequest();
    writer.flush();
    return baos.toByteArray();
  }

  public JSONArray makeGetRequestReturningJSON() throws IOException, ServletException, JSONException
  {
    String jsonString = new String(makeGetRequestReturningBytes());
    System.out.println("Json: " + jsonString);
    return new JSONArray(jsonString);
  }

}
