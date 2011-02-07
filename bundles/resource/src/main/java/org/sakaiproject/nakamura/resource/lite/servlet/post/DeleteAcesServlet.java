
package org.sakaiproject.nakamura.resource.lite.servlet.post;

import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

/**
 * <p>
 * Nakamura Sparse Post Servlet implementation for deleting the ACE for a set of principals on a JCR
 * resource.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Delete a set of Ace's from a node, the node is identified as a resource by the request
 * url &gt;resource&lt;.deleteAce.html
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>:applyTo</dt>
 * <dd>An array of ace principal names to delete. Note the principal name is the primary
 * key of the Ace in the Acl</dd>
 * </dl>
 *
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure. HTML explains the failure.</dd>
 * </dl>
 *
 */

@SlingServlet(resourceTypes={"sparse/Content"}, methods={"POST"}, selectors={"deleteAce"})
public class DeleteAcesServlet extends AbstractAccessPostServlet {
  private static final long serialVersionUID = 3784866802938282971L;

  /*
   * (non-Javadoc)
   *
   * @see
   * org.apache.sling.jackrabbit.accessmanager.post.AbstractAccessPostServlet#handleOperation
   * (org.apache.sling.api.SlingHttpServletRequest,
   * org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse htmlResponse, List<Modification> changes) throws ServletException, StorageClientException, AccessDeniedException {

    String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
    if (applyTo == null) {
      throw new ServletException("principalIds were not sumitted.");
    } else {
      Resource resource = request.getResource();
      if (resource == null) {
        throw new ResourceNotFoundException("Resource not found.");
      }
      Content contentItem = resource.adaptTo(Content.class);
      if ( contentItem == null ) {
        throw new ResourceNotFoundException("Resource is not a Sparse Content Object");
      }
      String resourcePath = contentItem.getPath();
      Session session = resource.adaptTo(Session.class);
      if (session == null) {
        throw new ServletException("Sparse Session not found");
      }

      // load the principalIds array into a set for quick lookup below
      Set<String> pidSet = new HashSet<String>();
      pidSet.addAll(Arrays.asList(applyTo));
      List<AclModification> aclModifications = Lists.newArrayList();
      for ( String p : pidSet) {
        aclModifications.add(new AclModification(AclModification.grantKey(p), 0,
            Operation.OP_DEL));
        aclModifications.add(new AclModification(AclModification.denyKey(p), 0,
            Operation.OP_DEL));
      }

      AccessControlManager accessControlManager = session.getAccessControlManager();
      accessControlManager.setAcl(Security.ZONE_CONTENT, resourcePath, aclModifications.toArray(new AclModification[aclModifications.size()]));
    }
  }

}
