
package org.sakaiproject.nakamura.resource.lite.servlet.post;

import com.google.common.collect.Lists;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;


/**
 * <p>
 * Nakamura Post Servlet implementation for modifying the ACEs for a principal on a JCR
 * resource.
 * </p>
 * <h2>Rest Service Description</h2>
 * <p>
 * Modify a principal's ACEs for the node identified as a resource by the request
 * URL &gt;resource&lt;.modifyAce.html
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>POST</li>
 * </ul>
 * <h4>Post Parameters</h4>
 * <dl>
 * <dt>principalId</dt>
 * <dd>The principal of the ACEs to modify in the ACL specified by the path.</dd>
 * <dt>privilege@*</dt>
 * <dd>One or more privileges, either granted or denied or none, which will be applied
 * to (or removed from) the node ACL. Any permissions that are present in an
 * existing ACE for the principal but not in the request are left untouched.</dd>
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
 * <h4>Notes</h4>
 * <p>
 * The principalId is assumed to refer directly to an Authorizable, that comes direct from
 * the UserManager. This can be a group or a user, but if its a group, denied permissions
 * will not be added to the group. The group will only contain granted privileges.
 * </p>
 *
 */
@SlingServlet(resourceTypes={"sparse/Content"}, methods={"POST"}, selectors={"modifyAce"})
public class ModifyAceServlet extends AbstractAccessPostServlet {
	private static final long serialVersionUID = -9182485466670280437L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ModifyAceServlet.class);

	@Override
	protected void handleOperation(SlingHttpServletRequest request,
			HtmlResponse htmlResponse, List<Modification> changes) throws StorageClientException, ServletException, AccessDeniedException {
	  Resource resource = request.getResource();
    if (resource == null) {
      throw new ResourceNotFoundException("Resource not found.");
    }
	  Session session = resource.adaptTo(Session.class);
		if (session == null) {
			throw new ServletException("Sparse Session not found");
		}
		String principalId = request.getParameter("principalId");
		if (principalId == null) {
			throw new ServletException("principalId was not submitted.");
		}
		
		Content contentItem = resource.adaptTo(Content.class); 
		if (contentItem == null) {
				throw new ResourceNotFoundException("Resource is not a Sparse Content Item");
		}
    String resourcePath = contentItem.getPath();
    
    Enumeration<?> parameterNames = request.getParameterNames();
    List<AclModification> aclModifications = Lists.newArrayList();
    while (parameterNames.hasMoreElements()) {
      Object nextElement = parameterNames.nextElement();
      if (nextElement instanceof String) {
        String paramName = (String)nextElement;
        if (paramName.startsWith("privilege@")) {
          String privilegeName = paramName.substring(10);
          Permission permssion = getPermission(privilegeName);
          String parameterValue = request.getParameter(paramName);
          if (parameterValue != null && parameterValue.length() > 0) {
            if ("granted".equals(parameterValue) && permssion != null ) {
              LOGGER.info("{}:{}:{}:{}", new Object[]{resourcePath,principalId,"granted",permssion.getName()});
              AclModification.addAcl(true, permssion, principalId, aclModifications);
            } else if ("denied".equals(parameterValue)  && permssion != null ) {
              LOGGER.info("{}:{}:{}:{}", new Object[]{resourcePath,principalId,"denied",permssion.getName()});
              AclModification.addAcl(false, permssion, principalId, aclModifications);
            } else if ("none".equals(parameterValue)){
              LOGGER.info("{}:{}:{}:{}", new Object[]{resourcePath,principalId,"cleared","all"});
              AclModification.removeAcl(true, Permissions.ALL, principalId, aclModifications);
              AclModification.removeAcl(false, Permissions.ALL, principalId, aclModifications);
            }
          }
        }
      }
    }
    

    AccessControlManager accessControlManager = session.getAccessControlManager();
    accessControlManager.setAcl(Security.ZONE_CONTENT, resourcePath, aclModifications.toArray(new AclModification[aclModifications.size()]));

	}

  private Permission getPermission(String privilegeName) {
    return Permissions.parse(privilegeName);
  }
}
