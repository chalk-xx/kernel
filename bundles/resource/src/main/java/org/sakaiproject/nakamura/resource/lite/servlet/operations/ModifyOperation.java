package org.sakaiproject.nakamura.resource.lite.servlet.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.NodeNameGenerator;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.apache.sling.servlets.post.impl.helper.DateParser;

import javax.servlet.ServletContext;

public class ModifyOperation implements SlingPostOperation {

  public ModifyOperation(NodeNameGenerator defaultNodeNameGenerator,
      DateParser dateParser, ServletContext servletContext) {
    // TODO Auto-generated constructor stub
  }

  public void run(SlingHttpServletRequest request, HtmlResponse response,
      SlingPostProcessor[] postprocessors) {
    // TODO Auto-generated method stub
    
  }

  public void setExtraNodeNameGenerators(NodeNameGenerator[] cachedNodeNameGenerators) {
    // TODO Auto-generated method stub
    
  }

}
