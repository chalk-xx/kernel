package org.sakaiproject.nakamura.files.pool;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.OptingServlet;

import java.io.IOException;

import javax.servlet.ServletException;

public interface DefaultServletDelegate extends OptingServlet {

  public void doDelegateGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

}
