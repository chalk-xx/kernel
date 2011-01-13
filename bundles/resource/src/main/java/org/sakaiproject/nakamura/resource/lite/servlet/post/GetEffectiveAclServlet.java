
package org.sakaiproject.nakamura.resource.lite.servlet.post;


import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Nakamura GET servlet implementation for dumping the declared ACL of a resource to JSON.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Mapped to the default resourceType. Gets and Acl for a resource. Get of the form
 * &gt;resource&lt;.acl.json Provided the user has access to the ACL, they get a chunk of
 * JSON of the form.
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>GET</li>
 * </ul>
 * <h4>Response</h4>
 * <dl>
 * <dt>200</dt>
 * <dd>Success.</dd>
 * <dt>404</dt>
 * <dd>The resource was not found.</dd>
 * <dt>500</dt>
 * <dd>Failure. HTML explains the failure.</dd>
 * </dl>
 * <h4>Example Response</h4>
 * <code>
 * <pre>
 * {
 * &quot;principalNameA&quot;:
 *      { &quot;granted&quot; : [
 *           &quot;permission1&quot;,
 *           &quot;permission2&quot;,
 *           &quot;permission3&quot;,
 *           &quot;permission4&quot; ],
 *        &quot;denied&quot; : [
 *           &quot;permission5&quot;,
 *           &quot;permission6&quot;,
 *           &quot;permission7&quot;,
 *           &quot;permission8&quot;]
 *       },
 * &quot;principalNameB&quot;:
 *       { &quot;granted&quot; : [
 *           &quot;permission1&quot;,
 *           &quot;permission2&quot;,
 *           &quot;permission3&quot;,
 *           &quot;permission4&quot; ],
 *         &quot;denied&quot; : [
 *           &quot;permission5&quot;,
 *           &quot;permission6&quot;,
 *           &quot;permission7&quot;,
 *           &quot;permission8&quot;] },
 * &quot;principalNameC&quot;:
 *       { &quot;granted&quot; : [
 *           &quot;permission1&quot;,
 *           &quot;permission2&quot;,
 *           &quot;permission3&quot;,
 *           &quot;permission4&quot; ],
 *         &quot;denied&quot; : [
 *           &quot;permission5&quot;,
 *           &quot;permission6&quot;,
 *           &quot;permission7&quot;,
 *           &quot;permission8&quot;] }
 * }
 * </pre>
 * </code>
 *
 */
@SlingServlet(resourceTypes={"sparse/Content"}, methods={"GET"}, selectors={"eacl"}, extensions={"json"})
public class GetEffectiveAclServlet extends AbstractGetAclServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1718943150427810377L;
  private static final Logger LOGGER = LoggerFactory.getLogger(GetEffectiveAclServlet.class);

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.
   * SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      Resource resource = request.getResource();
      if (resource == null) {
        throw new ResourceNotFoundException("Resource not found.");
      }
      Session session = resource.adaptTo(Session.class);
      if (session == null) {
        throw new ServletException("Sparse Session not found");
      }

      String resourcePath = null;
        Content contentItem = resource.adaptTo(Content.class);
        if (contentItem != null) {
          resourcePath = contentItem.getPath();
        } else {
          throw new ResourceNotFoundException("Resource is not a JCR Node");
        }

      AccessControlManager accessControlManager = session.getAccessControlManager();
      Map<String, Object> acl = accessControlManager.getEffectiveAcl(Security.ZONE_CONTENT,
          resourcePath);
      
      outputAcl(acl, response);

    } catch (AccessDeniedException ade) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (ResourceNotFoundException rnfe) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, rnfe.getMessage());
    } catch (Throwable throwable) {
      LOGGER.debug("Exception while handling GET " + request.getResource().getPath()
          + " with " + getClass().getName(), throwable);
      throw new ServletException(throwable);
    }
  }
}
