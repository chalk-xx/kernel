package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>GroupGetServlet</code>
 *
 */
@SlingServlet(resourceTypes={"sparse/group"},methods={"GET"},extensions={"json"})
@ServiceDocumentation(name="Get Group Servlet", okForVersion = "0.11",
    description="Returns a group in json format, using all the standard Sling semantics, but includes group profile." +
    		" Binds to any resource of type sparse/group although these are" +
    		"store under a: in the repo eg " +
    		"a:math101. The URL is exposed as  " +
        "url /system/userManager/group/math101. This servlet responds at " +
        "/system/userManager/group/math101.json",
    shortDescription="Get a group as json",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sparse/group"},
        extensions=@ServiceExtension(name="*", description="All the standard Sling serializations are possible, json, xml, html")),
    methods=@ServiceMethod(name="GET",
        description={"Get the group json.",
            "Example<br>" +
            "<pre>curl http://localhost:8080/system/userManager/group/math101.json</pre>"},
        response={
          @ServiceResponse(code=200,description="Success, the body contains the Group json with profile."),
          @ServiceResponse(code=404,description="Group was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
   ))
public class LiteGroupGetServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 2792407832129918578L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Authorizable authorizable = null;
    Resource resource = request.getResource();
    if (resource != null) {
        authorizable = resource.adaptTo(Authorizable.class);
    }

    if (!(authorizable instanceof Group)) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find group");
      return;
    }


    try {
      
      Session session = StorageClientUtils.adaptToSession(request.getResourceResolver().adaptTo(javax.jcr.Session.class));
      AccessControlManager acm = session.getAccessControlManager();
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      Authorizable thisUser = authorizableManager.findAuthorizable(session.getUserId());

      if (! acm.can(thisUser, Security.ZONE_CONTENT, LitePersonalUtils.getHomePath(authorizable.getId()), Permissions.CAN_READ)) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      write.object();
      ValueMap groupProps = resource.adaptTo(ValueMap.class);
      if (groupProps != null)
      {
        write.key("properties");
        write.valueMap(groupProps);

      }
      write.key("profile");
      write.value("a:"+authorizable.getId()+"/public/authprofile");
      write.key("members");
      write.array();

      Group group = (Group)authorizable;
      Set<String> memberNames = new HashSet<String>();
      String[] members = group.getMembers();
      for (String name : members )
      {
        if (!memberNames.contains(name)) {
          write.value(name);
        }
        memberNames.add(name);
      }
      write.endArray();
      write.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to render group details");
      return;
    } catch (StorageClientException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error using the access control manager");
    } catch (AccessDeniedException e) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permission to use the access control manager");
    }
  }



}
