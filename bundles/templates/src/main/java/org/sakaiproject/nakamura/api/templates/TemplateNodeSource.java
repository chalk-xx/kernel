package org.sakaiproject.nakamura.api.templates;

import javax.jcr.Node;

/**
 * Created by IntelliJ IDEA. User: zach Date: 1/5/11 Time: 11:08 AM To change this
 * template use File | Settings | File Templates.
 */
public interface TemplateNodeSource {
  /**
   * The resource Source implementation to be used by the resource loader, set to an
   * implementation of ReourceSource.
   */
  public static final String JCR_RESOURCE_LOADER_RESOURCE_SOURCE = "resourceSource";

  /**
   * @return gets the resource for the current context.
   */
  Node getNode();
}
