package org.sakaiproject.nakamura.http.usercontent;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.http.usercontent.ServerProtectionService;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This implementation of the ServerProtectionService allows GETs and POSTs to the host,
 * provided they don't request bodies from areas not consider part of the application, and
 * provided the posts come with a referrer header that is trusted.
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
 * is the content server, referrers of /* and http://localhost:8080* are trusted sources
 * of POST operations. Application data is any feed that does not stream a body, and
 * anything under /dev, /devwidgets, /index.html, all other GET operations are assumed to
 * be raw user content.
 * </p>
 */
@Component(immediate = true, metatype = true, enabled = false)
@Service(value = ServerProtectionService.class)
public class ServerProtectionServiceImpl implements ServerProtectionService {
  private static final String HMAC_SHA512 = "HmacSHA512";
  private static final String HMAC_PARAM = ":hmac";
  private static final String[] DEFAULT_TRUSTED_HOSTS = { "http://localhost:8080" };
  private static final String[] DEFAULT_TRUSTED_REFERRERS = { "/",
      "http://localhost:8080" };
  private static final String[] DEFAULT_TRUSTED_PATHS = { "/dev", "/devwidgets",
      "/index.html" };
  private static final String DEFAULT_UNTRUSTED_CONTENT_URL = "http://localhost:8082";
  private static final String DEFAULT_TRUSTED_SECRET_VALUE = "This Must Be set in production";

  @Property(value = { DEFAULT_UNTRUSTED_CONTENT_URL })
  private static final String UNTRUSTED_CONTENTURL_CONF = "untrusted.contenturl";
  @Property(value = { "/dev", "/devwidgets", "/index.html" })
  private static final String TRUSTED_PATHS_CONF = "trusted.paths";
  @Property(value = { "/", "http://localhost:8080" })
  private static final String TRUSTED_REFERRER_CONF = "trusted.referrer";
  @Property(value = { "http://localhost:8080" })
  private static final String TRUSTED_HOSTS_CONF = "trusted.hosts";
  @Property(value = { DEFAULT_TRUSTED_SECRET_VALUE })
  private static final String TRUSTED_SECRET_CONF = "trusted.secret";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ServerProtectionServiceImpl.class);

  /**
   * Set of hosts, that it is safe to receive non GET operations from.
   */
  private Set<String> safeHosts;
  /**
   * List of referer stems its safe to accept non GET operations from
   */
  private String[] safeReferrers;
  /**
   * List of path stems its safe to stream content bodies from using a trusted host
   */
  private String[] safeToStreamPaths;
  /**
   * The Stub of the URL used to deliver content bodies.
   */
  private String contentUrl;
  private Key[] transferKeys;

  @Activate
  public void activate(Map<String, Object> properties) throws NoSuchAlgorithmException,
      UnsupportedEncodingException {
    safeHosts = ImmutableSet.of(OsgiUtil.toStringArray(
        properties.get(TRUSTED_HOSTS_CONF), DEFAULT_TRUSTED_HOSTS));
    safeReferrers = OsgiUtil.toStringArray(properties.get(TRUSTED_REFERRER_CONF),
        DEFAULT_TRUSTED_REFERRERS);
    safeToStreamPaths = OsgiUtil.toStringArray(properties.get(TRUSTED_PATHS_CONF),
        DEFAULT_TRUSTED_PATHS);
    contentUrl = OsgiUtil.toString(properties.get(UNTRUSTED_CONTENTURL_CONF),
        DEFAULT_UNTRUSTED_CONTENT_URL);
    String transferSharedSecret = OsgiUtil.toString(properties.get(TRUSTED_SECRET_CONF),
        DEFAULT_TRUSTED_SECRET_VALUE);
    if (DEFAULT_TRUSTED_SECRET_VALUE.equals(transferSharedSecret)) {
      LOGGER.error("Configuration Error =============================");
      LOGGER
          .error("Configuration Error: Please set {} to secure Content Server in procuction "
              + TRUSTED_SECRET_CONF);
      LOGGER.error("Configuration Error =============================");
    }
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

  }

  public boolean isRequestSafe(SlingHttpServletRequest srequest,
      SlingHttpServletResponse sresponse) throws UnsupportedEncodingException,
      IOException {
    // if the method is not safe, the request can't be safe.
    if (!isMethodSafe(srequest, sresponse)) {
      return false;
    }
    boolean safeHost = isSafeHost(srequest);
    if (safeHost) {
      String ext = srequest.getRequestPathInfo().getExtension();
      if (ext == null || "res".equals(ext)) {
        // this is going to stream
        Resource resource = srequest.adaptTo(Resource.class);
        if ( resource == null ) {
          redirectToContent(srequest, sresponse);
          return false;
        }
        String path = resource.getPath();
        boolean safeToStream = false;
        for (String safePath : safeToStreamPaths) {
          if (path.startsWith(safePath)) {
            safeToStream = true;
          }
        }
        if (!safeToStream) {
          redirectToContent(srequest, sresponse);
          return false;
        }
      }
    }
    return true;
  }

  private void redirectToContent(HttpServletRequest request, HttpServletResponse response)
      throws UnsupportedEncodingException, IOException {
    String requestURL = request.getRequestURL().toString();
    // replace the protocol and host with the CDN host.
    int pathStart = requestURL.indexOf("/", requestURL.indexOf(":") + 3);
    requestURL = contentUrl + requestURL.substring(pathStart);
    // send via the session establisher
    String finalUrl = requestURL + "?" + request.getQueryString();
    response.sendRedirect(getTransferUrl(request, finalUrl));
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
          Base64 encoder = new Base64(true);
          return finalUrl
              + "&"
              + HMAC_PARAM
              + "="
              + URLEncoder.encode(encoder.encodeToString(m.doFinal()) + ";" + userId
                  + ";" + ts, "UTF-8");
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
          String[] parts = StringUtils.split(hmac, ';');
          String requestUrl = request.getRequestURL().append("?")
              .append(request.getQueryString()).toString();
          System.err.println("Checking requestUrl "+requestUrl);
          int i = requestUrl.indexOf("&" + HMAC_PARAM);
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
            Base64 encoder = new Base64(true);
            String testHmac = encoder.encodeToString(m.doFinal());
            if (testHmac.equals(requestHmac)) {
              return requestUserId;
            }
          }
        } catch (Exception e) {
          LOGGER.warn(e.getMessage());
          LOGGER.info(e.getMessage(), e);
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
      // check the referrer
      @SuppressWarnings("unchecked")
      Enumeration<String> referrers = hrequest.getHeaders("referrer");
      String referrer = null;
      if (referrers == null || !referrers.hasMoreElements()) {
        hresponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "POST Requests with no referrer are not acceptable");
        return false;
      }
      referrer = referrers.nextElement();
      if (referrer == null) {
        hresponse.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "POST Requests with no referrer are not acceptable");
        return false;
      }

      // Do we allow non get operations to this host ?
      if (safeHost) {
        // and if we do, do we accept them from the referrer mentioned ?
        safeHost = false;
        for (String safeReferrer : safeReferrers) {
          if (referrer.startsWith(safeReferrer)) {
            safeHost = true;
            LOGGER.info("Accepted referred {}  {}", safeReferrer, referrer);
            break;
          } else {
            LOGGER.info("Rejecting referred {}  {}", safeReferrer, referrer);
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

}
