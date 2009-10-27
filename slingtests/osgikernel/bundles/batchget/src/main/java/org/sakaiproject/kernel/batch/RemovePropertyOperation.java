package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.kernel.util.URIExpander;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service
 * @scr.property name="sling.post.operation" value="removeProperty"
 * @scr.reference name="URIExpander" interface="org.sakaiproject.kernel.util.URIExpander"
 */
public class RemovePropertyOperation extends AbstractPropertyOperationModifier {

  private URIExpander uriExpander;

  protected void bindURIExpander(URIExpander uriExpander) {
    this.uriExpander = uriExpander;
  }

  protected void unbindURIExpander(URIExpander uriExpander) {
    this.uriExpander = null;
  }
  
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    doModify(request, response, changes, uriExpander);

  }

}
