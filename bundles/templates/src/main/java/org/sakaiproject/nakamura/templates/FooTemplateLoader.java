package org.sakaiproject.nakamura.templates;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: zach Date: 1/4/11 Time: 5:00 PM To change this template
 * use File | Settings | File Templates.
 */
public class FooTemplateLoader extends ResourceLoader {
  @Override
  public void init(ExtendedProperties extendedProperties) {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public InputStream getResourceStream(String s) throws ResourceNotFoundException {
    return new ByteArrayInputStream(
        sampleTemplate().getBytes());
  }

  @Override
  public boolean isSourceModified(Resource resource) {
    return false; // To change body of implemented methods use File | Settings | File
                  // Templates.
  }

  @Override
  public long getLastModified(Resource resource) {
    return 0; // To change body of implemented methods use File | Settings | File
              // Templates.
  }

  private String sampleTemplate() {
    return "Dear ${person}, thanks for the fruitcake!";
  }
}
