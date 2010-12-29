package org.sakaiproject.nakamura.api.resource;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;

import javax.servlet.ServletException;

public abstract class AbstractAllMethodsServletResourceHandler implements
    ServletResourceHandler {

  public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doTrace(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doOptions(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doGeneric(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

  public void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
  }

}
