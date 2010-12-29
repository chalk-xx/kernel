package org.sakaiproject.nakamura.api.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ServletResourceHandler {

  void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  boolean accepts(SlingHttpServletRequest request);

  void doTrace(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  void doOptions(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  void doGeneric(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

  void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException;

}
