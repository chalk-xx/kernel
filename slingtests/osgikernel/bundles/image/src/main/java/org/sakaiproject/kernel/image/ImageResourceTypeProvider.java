package org.sakaiproject.kernel.image;

import org.apache.sling.jcr.resource.JcrResourceTypeProvider;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @scr.component immediate="true" label="ImageResourceTypeProvider"
 *                description="Image Service JCR resource type provider"
 * @scr.property name="service.description" value="Handles requests for Image
 *               resources"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.service 
 *              interface="org.apache.sling.jcr.resource.JcrResourceTypeProvider"
 * 
 */

public class ImageResourceTypeProvider implements JcrResourceTypeProvider {

  private static final String SAKAI_IMAGE_TYPE = "sakai:imageType";

  public String getResourceTypeForNode(Node node) throws RepositoryException {
    if (node.hasProperty(SAKAI_IMAGE_TYPE)) {
      return node.getProperty(SAKAI_IMAGE_TYPE).getString();
    }
    return null;

  }

}
