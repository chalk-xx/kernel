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
package org.sakaiproject.nakamura.auth.trusted;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.auth.trusted.TokenStore.SecureCookie;
import org.sakaiproject.nakamura.auth.trusted.TokenStore.SecureCookieException;
import org.sakaiproject.nakamura.util.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 */
@Component(immediate = true)
@Service
public final class TrustedTokenServiceImpl implements TrustedTokenService {




  private static final Logger LOG = LoggerFactory.getLogger(TrustedTokenServiceImpl.class);

  /** Property to indivate if the session should be used. */
  @Property(boolValue = false, description = "If True the session will be used to track authentication of the user, otherwise a cookie will be used.")
  public static final String USE_SESSION = "sakai.auth.trusted.token.usesession";

  /** Property to indicate if only cookies should be secure */
  @Property(boolValue = false, description = "If true and cookies are bieng used, then only secure cookies will be accepted.")
  public static final String SECURE_COOKIE = "sakai.auth.trusted.token.securecookie";

  /** Property to indicate the TTL on cookies */
  @Property(longValue = 1200000, description = "The TTL of a cookie based token, in ms")
  public static final String TTL = "sakai.auth.trusted.token.ttl";

  /** Property to indicate the name of the cookie. */
  @Property(value = "sakai-trusted-authn", description = "The name of the token")
  public static final String COOKIE_NAME = "sakai.auth.trusted.token.name";

  /** Property to point to keystore file */
  @Property(value = "sling/cookie-keystore.bin", description = "The name of the token store")
  public static final String TOKEN_FILE_NAME = "sakai.auth.trusted.token.storefile";

  /** Property to contain the shared secret used by all trusted servers */
  @Property(value = "default-setting-change-before-use", description= "The shared secret used for server to server trusted tokens")
  public static final String SERVER_TOKEN_SHARED_SECRET = "sakai.auth.trusted.server.secret";

  /** True if server tokens are enabled. */
  @Property(boolValue=true, description = "If true, trusted tokens from servers are accepted considered" )
  public static final String SERVER_TOKEN_ENABLED = "sakai.auth.trusted.server.enabled";

  /** A list of all the known safe hosts to trust as servers */
  @Property(value =";localhost;", description="A ; seperated list of hosts that this instance trusts to make server connections.")
  public static final String SERVER_TOKEN_SAFE_HOSTS = "sakai.auth.trusted.server.safe-hosts";

  /**
   * If True, sessions will be used, if false cookies.
   */
  private boolean usingSession = false;

  /**
   * Shoudl the cookies go over ssl.
   */
  private boolean secureCookie = false;

  /**
   * The name of the authN token.
   */
  private String trustedAuthCookieName;


  /**
   * An optional cookie server can be used in a cluster to centralize the management of
   * authN tokens. This is for situations where session storage and replication is not
   * desired, and session affinity can't be tolerated. Without this clients must come back
   * to the same host where they were authenticated as the cookie encode decode has
   * entropy associated with the instance of the server they are operating on.
   */
  @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
  private ClusterCookieServer clusterCookieServer;

  private TokenStore tokenStore;

  private Long ttl;


  @Reference
  protected ClusterTrackingService clusterTrackingService;

  @Reference
  protected CacheManagerService cacheManager;

  /**
   * If this is true the implementation is in test mode to enable external components to
   * test, without compromising the protection of the class.
   */
  private boolean testing = false;

  /**
   * Contains the calls made during testing.
   */
  private ArrayList<Object[]> calls;

  private String sharedSecret;

  private boolean trustedTokenEnabled;

  private String safeHosts;

  /**
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws UnsupportedEncodingException
   * @throws IllegalStateException
   * 
   */
  public TrustedTokenServiceImpl() throws NoSuchAlgorithmException, InvalidKeyException,
      IllegalStateException, UnsupportedEncodingException {
      tokenStore = new TokenStore(); 
  }
  

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext context) {
    Dictionary props = context.getProperties();
    usingSession = (Boolean) props.get(USE_SESSION);
    secureCookie = (Boolean) props.get(SECURE_COOKIE);
    ttl = (Long) props.get(TTL);
    trustedAuthCookieName = (String) props.get(COOKIE_NAME);
    sharedSecret = (String) props.get(SERVER_TOKEN_SHARED_SECRET);
    trustedTokenEnabled = (Boolean) props.get(SERVER_TOKEN_ENABLED);
    safeHosts = (String) props.get(SERVER_TOKEN_SAFE_HOSTS);
    
    String tokenFile = (String) props.get(TOKEN_FILE_NAME);
    String serverId = clusterTrackingService.getCurrentServerId();
    tokenStore.doInit(cacheManager, tokenFile, serverId, ttl);
  }

  public void activateForTesting() {
    testing = true;
    calls = new ArrayList<Object[]>();
  }
  
  /**
   * @return the calls used in testing.
   */
  public ArrayList<Object[]> getCalls() {
    return calls;
  }

  /**
   * Extract credentials from the request.
   * 
   * @param req
   * @return credentials associated with the request.
   */
  public Credentials getCredentials(HttpServletRequest req, HttpServletResponse response) {
    if (testing) {
      calls.add(new Object[] { "getCredentials", req, response });
      return new SimpleCredentials("testing", "testing".toCharArray());
    }
    Credentials cred = null;
    String userId = null;
    String sakaiTrustedHeader = req.getHeader("x-sakai-token");
    if (trustedTokenEnabled && sakaiTrustedHeader != null
        && sakaiTrustedHeader.trim().length() > 0) {
      String host = req.getRemoteHost();
      if (safeHosts.indexOf(host) < 0) {
        LOG.warn("Ignoring Trusted Token request from {} ", host);
      } else {
        // we have a HMAC based token, we should see if it is valid against the key we
        // have
        // and if so create some credentials.
        String[] parts = sakaiTrustedHeader.split(";");
        if (parts.length == 3) {
          try {
            String hash = parts[0];
            String user = parts[1];
            String timestamp = parts[2];
            String hmac = Signature.calculateRFC2104HMAC(
                user + ";" + timestamp, sharedSecret);
            if (hmac.equals(hash)) {
              // the user is Ok, we will trust it.
              userId = user;
              cred = createCredentials(userId);
            }
          } catch (SignatureException e) {
            LOG.warn("Failed to validate server token : {} {} ", sakaiTrustedHeader, e
                .getMessage());
          }
        } else {
          LOG.warn("Illegal number of elements in trusted server token:{} {}  ",
              sakaiTrustedHeader, parts.length);
        }
      }
    }
    if (userId == null) {
      if (usingSession) {
        HttpSession session = req.getSession(false);
        if (session != null) {
          Credentials testCredentials = (Credentials) session
              .getAttribute(SA_AUTHENTICATION_CREDENTIALS);
          if (testCredentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) testCredentials;
            Object o = sc.getAttribute(CA_AUTHENTICATION_USER);
            if (o instanceof TrustedUser) {
              TrustedUser tu = (TrustedUser) o;
              if (tu.getUser() != null) {
                userId = tu.getUser();
                cred = testCredentials;
              }
            }
          }
        } else {
          cred = null;
        }
      } else {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
          for (Cookie c : cookies) {
            if (trustedAuthCookieName.equals(c.getName())) {
              if (secureCookie && !c.getSecure()) {
                continue;
              }
              String cookieValue = c.getValue();
              userId = decodeCookie(c.getValue());
              if (userId != null) {
                cred = createCredentials(userId);
                refreshToken(response, c.getValue(), userId);
                break;
              } else {
                LOG.debug("Invalid Cookie {} ", cookieValue);
              }
            }
          }
        }
      }
    }
    if (userId != null) {
      LOG.debug("Trusted Authentication for {} with credentials {}  ", userId, cred);
    }
    return cred;
  }

  /**
   * Remove credentials so that subsequent request don't contain credentials.
   * 
   * @param request
   * @param response
   */
  public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
    if ( testing ) {
      calls.add(new Object[]{"dropCredentials",request,response});
      return;
    }
    if (usingSession) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.setAttribute(SA_AUTHENTICATION_CREDENTIALS, null);
      }
    } else {
      Cookie c = new Cookie(trustedAuthCookieName, "invalid");
      c.setMaxAge(-1);
      c.setPath("/");
      c.setSecure(secureCookie);
      response.addCookie(c);
    }
  }

  /**
   * Inject a token into the request/response, this assumes htat the getUserPrincipal() of the request
   * or the request.getRemoteUser() contain valid user ID's from which to generate the request.
   *
   * 
   * @param req
   * @param resp
   */
  public void injectToken(HttpServletRequest request, HttpServletResponse response) {
    if ( testing ) {
      calls.add(new Object[]{"injectToken",request,response});
      return;
    }
    String userId = null;
    Principal p = request.getUserPrincipal();
    if (p != null) {
      userId = p.getName();
    }
    if (userId == null) {
      userId = request.getRemoteUser();
    }
    if (userId != null) {
      if (usingSession) {
        HttpSession session = request.getSession(true);
        if (session != null) {
          LOG.info("Injecting Credentials into Session for " + userId);
          session.setAttribute(SA_AUTHENTICATION_CREDENTIALS, createCredentials(userId));
        }
      } else {
        addCookie(response, userId);
      }
    }
  }

  /**
   * @param userId
   * @param response
   */
  void addCookie(HttpServletResponse response, String userId) {
    Cookie c = new Cookie(trustedAuthCookieName, encodeCookie(userId));
    c.setMaxAge(-1);
    c.setPath("/");
    c.setSecure(secureCookie);
    response.addCookie(c);
  }

  /**
   * Refresh the token, assumes that the cookie is valid.
   * 
   * @param req
   * @param value
   * @param userId
   */
  void refreshToken(HttpServletResponse response, String value, String userId) {
    String[] parts = StringUtils.split(value, "@");
    if (parts != null && parts.length == 4) {
      long cookieTime = Long.parseLong(parts[1].substring(1));
      if (System.currentTimeMillis() + (ttl / 2) > cookieTime) {
        addCookie(response, userId);
      }
    }

  }

  /**
   * Encode the user ID in a secure cookie.
   * 
   * @param userId
   * @return
   */
  String encodeCookie(String userId) {
    if (userId == null) {
      return null;
    }
    if (clusterCookieServer != null) {
      return clusterCookieServer.encodeCookie(userId);
    } else {
      long expires = System.currentTimeMillis() + ttl;
      SecureCookie secretKeyHolder = tokenStore.getActiveToken();

      try {
        return secretKeyHolder.encode(expires, userId);
      } catch (NoSuchAlgorithmException e) {
        LOG.error(e.getMessage(), e);
      } catch (InvalidKeyException e) {
        LOG.error(e.getMessage(), e);
      } catch (IllegalStateException e) {
        LOG.error(e.getMessage(), e);
      } catch (UnsupportedEncodingException e) {
        LOG.error(e.getMessage(), e);
      } catch (SecureCookieException e) {
        LOG.error(e.getMessage(), e);
      }
      return null;
    }
  }

  /**
   * Decode the user ID.
   * 
   * @param value
   * @return
   */
  String decodeCookie(String value) {
    if (value == null) {
      return null;
    }
    if (clusterCookieServer != null) {
      return clusterCookieServer.decodeCookie(value);
    } else {
      try {
        SecureCookie secureCookie = tokenStore.getSecureCookie();
        return secureCookie.decode(value);
      } catch (SecureCookieException e) {
        LOG.error(e.getMessage());
      }
    }
    return null;

  }


  /**
   * Create credentials from a validated userId.
   * 
   * @param req
   *          The request to sniff for a user.
   * @return
   */
  private Credentials createCredentials(String userId) {
    SimpleCredentials sc = new SimpleCredentials(userId, new char[0]);
    TrustedUser user = new TrustedUser(userId);
    sc.setAttribute(CA_AUTHENTICATION_USER, user);
    return sc;
  }

  /**
   * "Trusted" inner class for passing the user on to the authentication handler.<br/>
   * <br/>
   * By being a static, inner class with a private constructor, it is harder for an
   * external source to inject into the authentication chain.
   */
  static final class TrustedUser {
    private final String user;

    /**
     * Constructor.
     * 
     * @param user
     *          The user to represent.
     */
    private TrustedUser(String user) {
      this.user = user;
    }

    /**
     * Get the user that is being represented.
     * 
     * @return The represented user.
     */
    String getUser() {
      return user;
    }
  }

}
