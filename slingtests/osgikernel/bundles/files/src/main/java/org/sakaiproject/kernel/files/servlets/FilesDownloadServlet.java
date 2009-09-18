package org.sakaiproject.kernel.files.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/file"
 * @scr.property name="sling.servlet.methods" values.0="GET"
 * @scr.property name="sling.servlet.selectors" values.0="download"
 */
public class FilesDownloadServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 7648061864468616272L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
        
  }

}
