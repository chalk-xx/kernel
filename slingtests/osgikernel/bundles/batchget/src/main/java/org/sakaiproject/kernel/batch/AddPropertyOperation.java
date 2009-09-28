package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service
 * @scr.property name="sling.post.operation" value="addProperty"
 */
public class AddPropertyOperation extends AbstractPropertyOperationModifier {

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    modifyProperties(request, response, changes);
  }

}
