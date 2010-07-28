package org.sakaiproject.nakamura.files.pool;

import static javax.jcr.security.Privilege.JCR_ALL;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.jcr.JCRConstants;
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(methods = "POST", paths = "/system/pool/createfile")
public class CreateContentPoolServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -5099697955361286370L;

  public static final char[] ENCODING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
  public static final char[] HASHENCODING = "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray();

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateContentPoolServlet.class);
 
  
  private String serverId;
  private long startingPoint;

  @Reference
  protected ClusterTrackingService clusterTrackingService;
  @Reference
  protected SlingRepository slingRepository;
  private Object lock = new Object();


  public void activate(ComponentContext componentContext) {
    synchronized (lock) {
      serverId = clusterTrackingService.getCurrentServerId();
      startingPoint = System.currentTimeMillis();      
    }
  }
  


  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      String userId = request.getRemoteUser();
      PrincipalManager principalManager = AccessControlUtil
      .getPrincipalManager(session);
      Principal userPrincipal = principalManager.getPrincipal(userId);
      Map<String, String> results = new HashMap<String, String>();
      for (Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap()
          .entrySet()) {
        for (RequestParameter p : e.getValue()) {
          if (!p.isFormField()) {
            String poolId = generatePoolId();
            createFile(hash(poolId), session, p, userPrincipal);
            results.put(p.getFileName(), poolId);
          }
        }
      }
      // save so the resolver further down will find this file.
      if (session.hasPendingChanges()) {
        session.save();
      }
      

      response.setStatus(HttpServletResponse.SC_CREATED);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      JSONObject jsonObject = new JSONObject(results);
      response.getWriter().write(jsonObject.toString());

    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(),e);
      throw new ServletException(e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.warn(e.getMessage(),e);
      throw new ServletException(e.getMessage(), e);
    } finally {
      session.logout();
    }
  }
  
  private void createFile(String path, Session session, RequestParameter value, Principal userPrincipal) throws RepositoryException, IOException {
    // get content type
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

    Node fileNode = JcrUtils.deepGetOrCreateNode(session, path, JcrConstants.NT_FILE);
    Node resourceNode = JcrUtils.deepGetOrCreateNode(session, fileNode.getPath()+"/"+JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
    resourceNode.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
    resourceNode.setProperty(JcrConstants.JCR_MIMETYPE, contentType);
    resourceNode.setProperty(JcrConstants.JCR_DATA, session.getValueFactory().createBinary(value.getInputStream()));

    AccessControlUtil.replaceAccessControlEntry(session, fileNode.getPath(),
                userPrincipal, new String[] { JCR_ALL }, null, null, null);

    fileNode.addMixin(JCRConstants.MIX_SAKAIPROPERTIES);
            // set some properties to make it possible to locate this pool file without
            // having to use the path.
    fileNode.setProperty("sakai:pool-file", "1");
    fileNode.setProperty("sakai:pool-file-owner", userPrincipal.getName());
    fileNode.setProperty("sakai:pool-file-name", value.getFileName());

  }



  public static String hash(String poolId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    String encodedId = StringUtils.encode(md.digest(poolId.getBytes("UTF-8")), HASHENCODING);
    LOGGER.info("Hashing [{}] gave [{}] ",poolId,encodedId);
    return "/_p/"+encodedId.charAt(0)+"/"+encodedId.substring(1,3)+"/"+encodedId.substring(3,5)+"/"+encodedId.substring(5,7)+"/"+poolId;
  }

  
  private String generatePoolId() throws UnsupportedEncodingException, NoSuchAlgorithmException {
    synchronized (lock) {
      String newId = String.valueOf(startingPoint++) + "-" + serverId;
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return StringUtils.encode(md.digest(newId.getBytes("UTF-8")), ENCODING);      
    }
  }


}
