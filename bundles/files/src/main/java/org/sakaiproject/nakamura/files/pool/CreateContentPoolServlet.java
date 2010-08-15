/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.files.pool;

import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_RT;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.jcr.security.Privilege.JCR_READ;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.NT_RESOURCE;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_FILENAME;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_NT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_RT;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = "POST", paths = "/system/pool/createfile")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Allows for uploading files to the pool.") })
public class CreateContentPoolServlet extends SlingAllMethodsServlet {

  @Reference
  protected ClusterTrackingService clusterTrackingService;
  @Reference
  protected SlingRepository slingRepository;

  private static final long serialVersionUID = -5099697955361286370L;

  public static final char[] ENCODING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
      .toCharArray();
  public static final char[] HASHENCODING = "abcdefghijklmnopqrstuvwxyz1234567890"
      .toCharArray();

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CreateContentPoolServlet.class);

  private String serverId;
  private long startingPoint;
  private Object lock = new Object();

  @Activate
  public void activate(ComponentContext componentContext) {
    serverId = clusterTrackingService.getCurrentServerId();
    startingPoint = System.currentTimeMillis();
  }

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String userId = request.getRemoteUser();

    // Anonymous users cannot upload files.
    if (UserConstants.ANON_USERID.equals(userId)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
          "Anonymous users cannot upload files to the pool.");
      return;
    }

    Session adminSession = null;
    try {
      // Grab an admin session so we can create files in the pool space.
      adminSession = slingRepository.loginAdministrative(null);

      // We need the authorizable for the user node that we'll create under the file.
      Authorizable au = PersonalUtils.getAuthorizable(adminSession, userId);

      // Loop over all the parameters
      // All the ones that are files will be stored.
      Map<String, String> results = new HashMap<String, String>();
      for (Entry<String, RequestParameter[]> e : request.getRequestParameterMap()
          .entrySet()) {
        for (RequestParameter p : e.getValue()) {
          if (!p.isFormField()) {
            // This is a file upload.
            // Generate an ID and store it.
            String poolId = generatePoolId();
            createFile(hash(poolId), adminSession, p, au);

            // Add it to the map so we can output something to the UI.
            results.put(p.getFileName(), poolId);
          }
        }
      }

      // Persist any changes to JCR.
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }

      // Make sure we're outputting proper json.
      response.setStatus(HttpServletResponse.SC_CREATED);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      // Output some JSON.
      JSONObject jsonObject = new JSONObject(results);
      response.getWriter().write(jsonObject.toString());
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new ServletException(e.getMessage(), e);
    } finally {
      // Make sure we're logged out.
      adminSession.logout();
    }
  }

  private void createFile(String path, Session session, RequestParameter value,
      Authorizable au) throws RepositoryException, IOException {
    // Get the content type.
    String contentType = getContentType(value);

    // Create a proper nt:file node in jcr with some properties on it to make it possible
    // to locate this pool file without having to use the path.
    Node fileNode = JcrUtils.deepGetOrCreateNode(session, path, POOLED_CONTENT_NT);
    fileNode.setProperty(POOLED_CONTENT_FILENAME, value.getFileName());
    fileNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, POOLED_CONTENT_RT);
    Node resourceNode = fileNode.addNode(JCR_CONTENT, NT_RESOURCE);
    resourceNode.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
    resourceNode.setProperty(JcrConstants.JCR_MIMETYPE, contentType);
    resourceNode.setProperty(JcrConstants.JCR_DATA, session.getValueFactory()
        .createBinary(value.getInputStream()));

    // The current user will be a manager and thus gets the JCR_ALL privilege on the file.
    Principal userPrincipal = au.getPrincipal();
    AccessControlUtil.replaceAccessControlEntry(session, path, userPrincipal,
        new String[] { JCR_ALL }, null, null, null);

    // Other people can't see or do anything.
    Principal anon = new Principal() {
      public String getName() {
        return UserConstants.ANON_USERID;
      }
    };
    Principal everyone = new Principal() {
      public String getName() {
        return "everyone";
      }
    };
    AccessControlUtil.replaceAccessControlEntry(session, path, anon, null,
        new String[] { JCR_ALL }, null, null);
    AccessControlUtil.replaceAccessControlEntry(session, path, everyone, null,
        new String[] { JCR_ALL }, null, null);

    // Create a users node under this node.
    // We do this so we're still able to query the repository and find the files where a
    // user/group is viewer/manager of.
    // These nodes will be stored at /_p/a1/b2/c3/d4/FILENODE/e5/f6/g7/h8/USERNODE so we
    // can keep full ACL permissions on them.
    // We want to ACL control these user nodes because sometimes it's not allowed to see
    // who can view a file.
    String newPath = fileNode.getPath() + PersonalUtils.getUserHashedPath(au);
    Node userNode = JcrUtils.deepGetOrCreateNode(session, newPath);
    userNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, POOLED_CONTENT_USER_RT);
    userNode.setProperty(POOLED_CONTENT_USER_MANAGER, new String[] { userPrincipal
        .getName() });

    // These nodes can only be modified by an admin/manager.
    // Other people cannot even see it.
    replaceAccessControlEntry(session, userNode.getPath(), everyone, null, new String[] {
        JCR_READ, JCR_ALL }, null, null);

  }

  /**
   * Get the content type of a file that's in a {@link RequestParameter}.
   *
   * @param value
   *          The request parameter.
   * @return The content type.
   */
  private String getContentType(RequestParameter value) {
    String contentType = value.getContentType();
    if (contentType != null) {
      int idx = contentType.indexOf(';');
      if (idx > 0) {
        contentType = contentType.substring(0, idx);
      }
    }
    if (contentType == null || contentType.equals("application/octet-stream")) {
      // try to find a better content type
      contentType = getServletContext().getMimeType(value.getFileName());
      if (contentType == null || contentType.equals("application/octet-stream")) {
        contentType = "application/octet-stream";
      }
    }
    return contentType;
  }

  public static String hash(String poolId) throws NoSuchAlgorithmException,
      UnsupportedEncodingException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    String encodedId = StringUtils.encode(md.digest(poolId.getBytes("UTF-8")),
        HASHENCODING);
    LOGGER.info("Hashing [{}] gave [{}] ", poolId, encodedId);
    return "/_p/" + encodedId.charAt(0) + "/" + encodedId.substring(1, 3) + "/"
        + encodedId.substring(3, 5) + "/" + encodedId.substring(5, 7) + "/" + poolId;
  }

  private String generatePoolId() throws UnsupportedEncodingException,
      NoSuchAlgorithmException {
    synchronized (lock) {
      String newId = String.valueOf(startingPoint++) + "-" + serverId;
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return StringUtils.encode(md.digest(newId.getBytes("UTF-8")), ENCODING);
    }
  }

}
