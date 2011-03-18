package org.sakaiproject.nakamura.http.usercontent;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionValidator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This implementation of the ServerProtectionService allows GETs and POSTs to the host,
 * provided they don't request bodies from areas not consider part of the application, and
 * provided the posts come with a referer header that is trusted.
 * </p>
 * <p>
 * GETs to application content (file bodies and encapsulated feeds) are allowed.
 * </p>
 * <p>
 * POSTS (or non GET/HEAD) operations from locations that are not trusted get a 400, bad
 * request.
 * </p>
 * <p>
 * GETs to non application content get redirected, with a HMAC SHA-512 digest to a host
 * that serves that content.
 * </p>
 * <p>
 * If the GET is an anon GET, then it gets redirected with no HMAC.
 * </p>
 * <p>
 * The default setup is http://localhost:8080 is the trusted server. http://localhost:8082
 * is the content server, referers of /* and http://localhost:8080* are trusted sources
 * of POST operations. Application data is any feed that does not stream a body, and
 * anything under /dev, /devwidgets, /index.html, all other GET operations are assumed to
 * be raw user content.
 * </p>
 */
@Component(immediate = true, metatype = true)
@Service(value = ServerProtectionService.class)
public class ServerProtectionServiceImpl implements ServerProtectionService {
  private static final String HMAC_SHA512 = "HmacSHA512";
  private static final String HMAC_PARAM = ":hmac";
  private static final String[] DEFAULT_TRUSTED_HOSTS = { "http://localhost:8080" };
  private static final String[] DEFAULT_TRUSTED_REFERERS = { "/",
      "http://localhost:8080" };
  private static final String[] DEFAULT_TRUSTED_PATHS = { "/dev", "/devwidgets", "/system" };
  private static final String[] DEFAULT_TRUSTED_EXACT_PATHS = { "/", "/index.html" };
  private static final String DEFAULT_UNTRUSTED_CONTENT_URL = "http://localhost:8082";
  private static final String DEFAULT_TRUSTED_SECRET_VALUE = "This Must Be set in production";
  private static final String[] DEFAULT_WHITELIST_POST_PATHS = {"/system/console"};
  private static final String[] DEFAULT_ANON_WHITELIST_POST_PATHS = {"/system/userManager/user.create"};

  @Property(value = { DEFAULT_UNTRUSTED_CONTENT_URL })
  private static final String UNTRUSTED_CONTENTURL_CONF = "untrusted.contenturl";
  @Property(value = { "/dev", "/devwidgets", "/system" })
  private static final String TRUSTED_PATHS_CONF = "trusted.paths";
  @Property(value = { "/", "/index.html" })
  private static final String TRUSTED_EXACT_PATHS_CONF = "trusted.exact.paths";
  @Property(value = { "/", "http://localhost:8080" })
  private static final String TRUSTED_REFERER_CONF = "trusted.referer";
  @Property(value = { "http://localhost:8080" })
  private static final String TRUSTED_HOSTS_CONF = "trusted.hosts";
  @Property(value = { DEFAULT_TRUSTED_SECRET_VALUE })
  private static final String TRUSTED_SECRET_CONF = "trusted.secret";
  @Property(value = {"/system/console"})
  private static final String WHITELIST_POST_PATHS_CONF = "trusted.postwhitelist";
  @Property(value = {"/system/userManager/user.create"})
  private static final String ANON_WHITELIST_POST_PATHS_CONF = "trusted.anonpostwhitelist";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ServerProtectionServiceImpl.class);

  /**
   * Set of hosts, that it is safe to receive non GET operations from.
   */
  private Set<String> safeHosts;
  /**
   * List of referer stems its safe to accept non GET operations from
   */
  private String[] safeReferers;
  /**
   * List of path stems its safe to stream content bodies from using a trusted host
   */
  private String[] safeToStreamPaths;
  /**
   * List of path stems its safe to stream content bodies from using a trusted host
   */
  private Set<String> safeToStreamExactPaths;
  /**
   * The Stub of the URL used to deliver content bodies.
   */
  private String contentUrl;
  /**
   * Array of keys created from the secret, indexed by the second digit of the timestamp
   */
  private Key[] transferKeys;
  /**
   * List of url stems that are always Ok to accept posts from on any URL (eg
   * /system/console). You will want to add additional protection on these.
   */
  private String[] postWhiteList;
  /**
   * list of paths where its safe for anon to post to.
   */
  private String[] safeForAnonToPostPaths;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, bind = "bindServerProtectionValidator", unbind = "unbindServerProtectionValidator")
  private ServerProtectionValidator[] serverProtectionValidators;
  private Map<ServiceReference, ServerProtectionValidator> serverProtectionValidatorsStore = Maps
      .newConcurrentHashMap();
  private BundleContext bundleContext;

  @Activate
  public void activate(ComponentContext componentContext)
      throws NoSuchAlgorithmException, UnsupportedEncodingException,
      InvalidSyntaxException {
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> properties = componentContext.getProperties();
    safeHosts = ImmutableSet.of(OsgiUtil.toStringArray(
        properties.get(TRUSTED_HOSTS_CONF), DEFAULT_TRUSTED_HOSTS));
    safeReferers = OsgiUtil.toStringArray(properties.get(TRUSTED_REFERER_CONF),
        DEFAULT_TRUSTED_REFERERS);
    safeToStreamPaths = OsgiUtil.toStringArray(properties.get(TRUSTED_PATHS_CONF),
        DEFAULT_TRUSTED_PATHS);
    safeToStreamExactPaths = ImmutableSet.of(OsgiUtil.toStringArray(
        properties.get(TRUSTED_EXACT_PATHS_CONF), DEFAULT_TRUSTED_EXACT_PATHS));
    contentUrl = OsgiUtil.toString(properties.get(UNTRUSTED_CONTENTURL_CONF),
        DEFAULT_UNTRUSTED_CONTENT_URL);
    postWhiteList = OsgiUtil.toStringArray(
        properties.get(WHITELIST_POST_PATHS_CONF), DEFAULT_WHITELIST_POST_PATHS);
    safeForAnonToPostPaths = OsgiUtil.toStringArray(
        properties.get(ANON_WHITELIST_POST_PATHS_CONF), DEFAULT_ANON_WHITELIST_POST_PATHS);
    String transferSharedSecret = OsgiUtil.toString(properties.get(TRUSTED_SECRET_CONF),
        DEFAULT_TRUSTED_SECRET_VALUE);
    if (DEFAULT_TRUSTED_SECRET_VALUE.equals(transferSharedSecret)) {
      LOGGER.error("Configuration Error =============================");
      LOGGER
          .error("Configuration Error: Please set {} to secure Content Server in procuction ",TRUSTED_SECRET_CONF);
      LOGGER.error("Configuration Error =============================");
    }

    LOGGER.info("Trusted Hosts {}",safeHosts);
    LOGGER.info("Trusted Referers {} ",Arrays.toString(safeReferers));
    LOGGER.info("Trusted Stream Paths {} ",Arrays.toString(safeToStreamPaths));
    LOGGER.info("Trusted Stream Resources {} ",safeToStreamExactPaths);
    LOGGER.info("POST Whitelist {} ",postWhiteList);
    LOGGER.info("Content Host {} ",contentUrl);
    LOGGER.info("Content Shared Secret [{}] ",transferSharedSecret);

    transferKeys = new Key[10];
    MessageDigest md = MessageDigest.getInstance("SHA-512");
    Base64 encoder = new Base64(true);
    byte[] input = transferSharedSecret.getBytes("UTF-8");
    // create a static ring of 10 keys by repeatedly hashing the last key seed
    // starting with the transferSharedSecret
    for (int i = 0; i < transferKeys.length; i++) {
      md.reset();
      byte[] data = md.digest(input);
      transferKeys[i] = new SecretKeySpec(data, HMAC_SHA512);
      input = encoder.encode(data);
    }

    bundleContext = componentContext.getBundleContext();
    ServiceReference[] srs = bundleContext.getAllServiceReferences(
        ServerProtectionValidator.class.getName(), null);
    if ( srs != null ) {
      for (ServiceReference sr : srs) {
        bindServerProtectionValidator(sr);
      }
    }
  }

  public void destroy(ComponentContext c) {
    BundleContext bc = c.getBundleContext();
    for (Entry<ServiceReference, ServerProtectionValidator> e : serverProtectionValidatorsStore
        .entrySet()) {
      bc.ungetService(e.getKey());
    }
    serverProtectionValidatorsStore.clear();
    serverProtectionValidators = null;
  }

  public boolean isRequestSafe(SlingHttpServletRequest srequest,
      SlingHttpServletResponse sresponse) throws UnsupportedEncodingException,
      IOException {
    // if the method is not safe, the request can't be safe.
    if (!isMethodSafe(srequest, sresponse)) {
      return false;
    }
    String method = srequest.getMethod();
    if ( "GET|OPTIONS|HEAD".indexOf(method) < 0 ) {
      String userId = srequest.getRemoteUser();
      if ( User.ANON_USER.equals(userId) ) {
        String path = srequest.getRequestURI();
        boolean safeForAnonToPost = false;
        for (String safePath : safeForAnonToPostPaths) {
          if (path.startsWith(safePath)) {
            safeForAnonToPost = true;
            break;
          }
        }
        if ( ! safeForAnonToPost ) {
          sresponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Anon users may not perform POST operations");
          return false;
        }
      }
    }
    boolean safeHost = isSafeHost(srequest);
    if (safeHost && "GET".equals(method)) {
      String ext = srequest.getRequestPathInfo().getExtension();
      if (ext == null || "res".equals(ext)) {
        // this is going to stream
        String path = srequest.getRequestURI();
        LOGGER.debug("Checking [{}] ", path);
        boolean safeToStream = safeToStreamExactPaths.contains(path);
        if (!safeToStream) {
          for (String safePath : safeToStreamPaths) {
            if (path.startsWith(safePath)) {
              safeToStream = true;
              break;
            }
          }
          if (!safeToStream) {
            Resource resource = srequest.getResource();
            if ( resource != null ) {
              String resourcePath = resource.getPath();
              LOGGER.debug("Checking Resource Path [{}]",resourcePath);
              safeToStream = safeToStreamExactPaths.contains(resourcePath);
              for (String safePath : safeToStreamPaths) {
                if (resourcePath.startsWith(safePath)) {
                  safeToStream = true;
                  break;
                }
              }
              if (!safeToStream) {
                for (ServerProtectionValidator serverProtectionValidator : serverProtectionValidators) {
                  if ( serverProtectionValidator.safeToStream(srequest, resource)) {
                    safeToStream = true;
                    break;
                  }
                }
              }
            }
            if (!safeToStream) {
              redirectToContent(srequest, sresponse);
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  private void redirectToContent(HttpServletRequest request, HttpServletResponse response)
      throws UnsupportedEncodingException, IOException {
    StringBuffer requestURL = request.getRequestURL();
    String queryString = request.getQueryString();
    if (queryString != null) {
      requestURL.append("?").append(queryString);
    }
    String url = requestURL.toString();
    // replace the protocol and host with the CDN host.
    int pathStart = requestURL.indexOf("/", requestURL.indexOf(":") + 3);
    url = contentUrl + url.substring(pathStart);
    // send via the session establisher
    LOGGER.debug("Sending redirect for {} {} ",request.getMethod(), url);
    response.sendRedirect(getTransferUrl(request, url));
  }

  /**
   * @param request
   * @param finalUrl
   * @return
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws IllegalStateException
   * @throws UnsupportedEncodingException
   */
  private String getTransferUrl(HttpServletRequest request, String finalUrl) {
    // only transfer authN from a trusted safe host
    if (isSafeHost(request)) {
      String userId = request.getRemoteUser();
      if (userId != null && !User.ANON_USER.equals(userId)) {
        try {
          long ts = System.currentTimeMillis();
          int keyIndex = (int) (ts - ((ts / 10) * 10));
          Mac m = Mac.getInstance(HMAC_SHA512);
          m.init(transferKeys[keyIndex]);

          String message = finalUrl + ";" + userId + ";" + ts;
          m.update(message.getBytes("UTF-8"));
          String hmac = Base64.encodeBase64URLSafeString(m.doFinal());
          hmac = Base64.encodeBase64URLSafeString((hmac + ";" + userId + ";" + ts)
              .getBytes("UTF-8"));
          String spacer = "?";
          if ( finalUrl.indexOf('?') >  0) {
            spacer = "&";
          }
          return finalUrl + spacer + HMAC_PARAM + "=" + hmac;
        } catch (Exception e) {
          LOGGER.warn(e.getMessage(), e);
        }
      }
    }
    return finalUrl;
  }

  public String getTransferUserId(HttpServletRequest request) {
    // only ever get a user ID in this way on a non trusted safe host.
    if (!isSafeHost(request)) {
      String hmac = request.getParameter(HMAC_PARAM);
      if (hmac != null) {
        try {
          hmac = new String(Base64.decodeBase64(hmac.getBytes("UTF-8")), "UTF-8");
          String[] parts = StringUtils.split(hmac, ';');
          String requestUrl = request.getRequestURL().append("?")
              .append(request.getQueryString()).toString();
          System.err.println("Checking requestUrl [" + requestUrl + "]");
          int i = requestUrl.indexOf("&" + HMAC_PARAM);
          if ( i < 0 ) {
            i = requestUrl.indexOf("?" + HMAC_PARAM);
          }
          String finalUrl = requestUrl.substring(0, i);
          String requestHmac = parts[0];
          String requestUserId = parts[1];
          String requestTs = parts[2];
          String message = finalUrl + ";" + requestUserId + ";" + requestTs;
          long requestTsL = Long.parseLong(requestTs);
          if (Math.abs(System.currentTimeMillis() - requestTsL) < 60000L) {
            int keyIndex = (int) (requestTsL - ((requestTsL / 10) * 10));
            Mac m = Mac.getInstance(HMAC_SHA512);
            m.init(transferKeys[keyIndex]);
            m.update(message.getBytes("UTF-8"));
            String testHmac = Base64.encodeBase64URLSafeString(m.doFinal());
            if (testHmac.equals(requestHmac)) {
              return requestUserId;
            }
          }
        } catch (Exception e) {
          LOGGER.warn(e.getMessage());
          LOGGER.debug(e.getMessage(), e);
        }
      }
    }
    return null;
  }

  public boolean isMethodSafe(HttpServletRequest hrequest, HttpServletResponse hresponse)
      throws IOException {
    String method = hrequest.getMethod();
    boolean safeHost = isSafeHost(hrequest);

    // protect against POST originating from other domains, this assumes that there is no
    // browser bug in this area
    // and no flash bug.
    if (!("GET".equals(method) || "HEAD".equals(method))) {
      String path = hrequest.getRequestURI();
      for (String okPostStem : postWhiteList) {
        if (path.startsWith(okPostStem)) {
          return true;
        }
      }
      // check the Referer
      @SuppressWarnings("unchecked")
      Enumeration<String> referers = hrequest.getHeaders("Referer");
      String referer = null;
      if (referers == null || !referers.hasMoreElements()) {
        LOGGER.debug("No Referer header present ");
        hresponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "POST Requests with no Referer are not acceptable");
        return false;
      }
      referer = referers.nextElement();
      if (referer == null) {
        LOGGER.debug("No Referer header present, was null ");
        hresponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "POST Requests with no Referer are not acceptable");
        return false;
      }

      // Do we allow non get operations to this host ?
      if (safeHost) {
        // and if we do, do we accept them from the Referer mentioned ?
        safeHost = false;
        for (String safeReferer : safeReferers) {
          if (referer.startsWith(safeReferer)) {
            safeHost = true;
            LOGGER.debug("Accepted referred {}  {}", safeReferer, referer);
            break;
          } else {
            LOGGER.debug("Rejecting referred {}  {}", safeReferer, referer);
          }
        }
      }
      if (!safeHost) {
        hresponse
            .sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "POST Requests are only accepted from the Application, this request was not from the application.");
        return false;
      }
    }
    return true;
  }

  private boolean isSafeHost(HttpServletRequest hrequest) {
    String requestHost = hrequest.getScheme() + "://" + hrequest.getServerName() + ":"
        + hrequest.getServerPort();
    // safe hosts are defiend as hosts from which we we can accept non get operations
    return safeHosts.contains(requestHost);
  }

  public void bindServerProtectionValidator(ServiceReference serviceReference) {
    serverProtectionValidatorsStore.put(serviceReference,
        (ServerProtectionValidator) bundleContext.getService(serviceReference));
    serverProtectionValidators = serverProtectionValidatorsStore.values().toArray(
        new ServerProtectionValidator[serverProtectionValidatorsStore.size()]);

  }

  public void unbindServerProtectionValidator(ServiceReference serviceReference) {
    serverProtectionValidatorsStore.remove(serviceReference);
    bundleContext.ungetService(serviceReference);
    serverProtectionValidators = serverProtectionValidatorsStore.values().toArray(
        new ServerProtectionValidator[serverProtectionValidatorsStore.size()]);
  }

}
