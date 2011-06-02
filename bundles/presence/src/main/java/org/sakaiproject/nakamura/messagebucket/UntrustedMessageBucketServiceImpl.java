package org.sakaiproject.nakamura.messagebucket;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.cluster.ClusterUser;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucket;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketService;
import org.sakaiproject.nakamura.util.Signature;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

/**
 * Buckets from this service are not greatly trusted, and we wont trust them to push data
 * into the server, only receive data from the server.
 */
@Component(immediate = true, metatype = true)
@Service(value=MessageBucketService.class)
public class UntrustedMessageBucketServiceImpl implements MessageBucketService {

  private static final String DEFAULT_URL_PATTERN = "http://localhost:8080/system/uievent/default?token={3}&server={6}&user={7}";
  private static final String BUCKETURLPATTERN_CONFIG = "bucketurlpattern";
  private String sharedSecret;
  private Map<String, MessageBucket> messageBuckets = new ConcurrentHashMap<String, MessageBucket>();
  private String urlPattern;
  
  @Reference
  private ClusterTrackingService clusterService;
  
  @Activate
  public void activate(Map<String, Object> properties) {
    sharedSecret = String.valueOf(System.currentTimeMillis()); // not that secure !
    urlPattern = OsgiUtil.toString(properties.get(BUCKETURLPATTERN_CONFIG), DEFAULT_URL_PATTERN);
  }

  public MessageBucket getBucket(String token) throws MessageBucketException {
    String key = getKey(token);
    if (key == null) {
      throw new MessageBucketException("Invalid Token " + token);
    }
    MessageBucket mb = messageBuckets.get(key);
    if (mb == null) {
      mb = new MessageBucketImpl();
      messageBuckets.put(key, mb);
    }
    return mb;
  }

  public String getToken(String userId, String context) throws MessageBucketException {
    try {
      String timeStamp = Long.toHexString(System.currentTimeMillis());
      String hmac = Signature.calculateRFC2104HMAC(userId + ";" + timeStamp + ";"
          + context, sharedSecret);
      String token = userId + ";" + timeStamp + ";" + context + ";" + hmac;
      return Base64.encodeBase64URLSafeString(token.getBytes("UTF8"));
    } catch (SignatureException e) {
      throw new MessageBucketException(e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      throw new MessageBucketException(e.getMessage(), e);
    }
  }

  public String getKey(String token) throws MessageBucketException {
    try {
      String bareToken = new String(Base64.decodeBase64(token), "UTF8");
      String[] parts = StringUtils.split(bareToken, ";", 4);
      String hmac = Signature.calculateRFC2104HMAC(parts[0] + ";" + parts[1] + ";"
          + parts[2], sharedSecret);
      if (hmac.equals(parts[3])) {
        return parts[0] + "-" + parts[2];
      }
      return null;
    } catch (UnsupportedEncodingException e) {
      throw new MessageBucketException(e.getMessage(), e);
    } catch (SignatureException e) {
      throw new MessageBucketException(e.getMessage(), e);
    }
  }
  
  public String getBucketUrl(HttpServletRequest request, String context) throws MessageBucketException {
    String[] trackingCookies = clusterService.getRequestTrackingCookie(request);
    if ( trackingCookies != null ) {
      for(String trackingCookie : trackingCookies) {
        ClusterUser clusterUser = clusterService.getUser(trackingCookie);
        if (clusterUser != null) {
          String token = getToken(clusterUser.getUser(), context);
          return MessageFormat.format(urlPattern,
              request.getScheme(),
              request.getLocalName(),
              String.valueOf(request.getLocalPort()),
              token,
              token.substring(0,1),
              token.substring(0,2),
              clusterUser.getServerId(),
              request.getRemoteUser());
        }
      }
    }
    throw new MessageBucketException("No Cluster tracking is available");
  }

}
