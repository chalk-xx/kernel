package org.sakaiproject.nakamura.api.resource.lite;

import org.sakaiproject.nakamura.api.resource.DateParser;
import org.sakaiproject.nakamura.resource.lite.servlet.post.helper.DefaultNodeNameGenerator;
import org.sakaiproject.nakamura.resource.lite.servlet.post.operations.ModifyOperation;

import javax.servlet.ServletContext;

public class ResourceModifyOperation extends ModifyOperation {

  public ResourceModifyOperation(ServletContext servletContext) {
    super(new DefaultNodeNameGenerator(new String[]{}, 255), new DateParser(), servletContext);
  }
}
