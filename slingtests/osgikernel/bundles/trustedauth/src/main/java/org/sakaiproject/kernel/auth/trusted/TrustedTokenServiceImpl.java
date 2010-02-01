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
package org.sakaiproject.kernel.auth.trusted;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Dictionary;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

  /**
   * 
   */
  private static final String SHA1PRNG = "SHA1PRNG";

  /**
   * 
   */
  private static final String HMAC_SHA1 = "HmacSHA1";
  /**
   * 
   */
  private static final String UTF_8 = "UTF-8";

  /**
   *
   */
  public static final class SecureCookieException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -3461255120101001664L;

    /**
     * @param string
     */
    public SecureCookieException(String string) {
      super(string);
    }

  }

  /**
   *
   */
  public final class SecureCookie {

    private SecretKey secretKey;
    private int tokenNumber;

    /**
     * @param currentToken
     * @param secretKey
     */
    public SecureCookie(int tokenNumber) {
      this.tokenNumber = tokenNumber;
      this.secretKey = currentTokens[tokenNumber];
    }

    /**
     * @param value
     */
    public SecureCookie() {
    }

    /**
     * @return the secretKey
     */
    public SecretKey getSecretKey() {
      return secretKey;
    }

    /**
     * @return the tokenNumber
     */
    public int getTokenNumber() {
      return tokenNumber;
    }

    /**
     * @param expires
     * @param userId
     * @return
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public String encode(long expires, String userId) throws IllegalStateException,
        UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
      String cookiePayload = String.valueOf(tokenNumber) + String.valueOf(expires) + "@"
          + userId;
      Mac m = Mac.getInstance(HMAC_SHA1);
      m.init(secretKey);
      m.update(cookiePayload.getBytes(UTF_8));
      String cookieValue = byteToHex(m.doFinal());
      return cookieValue + "@" + cookiePayload;
    }

    /**
     * @return
     * @throws SecureCookieException
     */
    public String decode(String value) throws SecureCookieException {
      String[] parts = StringUtils.split(value, "@");
      if (parts != null && parts.length == 3) {
        this.tokenNumber = Integer.parseInt(parts[1].substring(0, 1));
        long cookieTime = Long.parseLong(parts[1].substring(1));
        if (System.currentTimeMillis() < cookieTime) {
          try {
            this.secretKey = currentTokens[tokenNumber];
            String hmac = encode(cookieTime, parts[2]);
            if (value.equals(hmac)) {
              return parts[2];
            }
          } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error(e.getMessage(), e);
          } catch (InvalidKeyException e) {
            LOG.error(e.getMessage(), e);
          } catch (IllegalStateException e) {
            LOG.error(e.getMessage(), e);
          } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
          } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(), e);
          }
          throw new SecureCookieException("AuthNCookie is invalid " + value);
        } else {
          throw new SecureCookieException("AuthNCookie has expired " + value + " "
              + (System.currentTimeMillis() - cookieTime) + " ms ago");
        }
      } else {
        throw new SecureCookieException("AuthNCookie is invalid format " + value);
      }
    }
  }

  /**
   * 
   */
  private static final char[] TOHEX = "0123456789abcdef".toCharArray();

  private static final Logger LOG = LoggerFactory.getLogger(TrustedTokenService.class);

  /** Property to indivate if the session should be used. */
  @Property(boolValue = false, description = "If True the session will be used to track authentication of the user, otherwise a cookie will be used.")
  static final String USE_SESSION = "sakai.auth.trusted.token.usesession";

  /** Property to indicate if only cookies should be secure */
  @Property(boolValue = false, description = "If true and cookies are bieng used, then only secure cookies will be accepted.")
  static final String SECURE_COOKIE = "sakai.auth.trusted.token.securecookie";

  /** Property to indicate the TTL on cookies */
  @Property(longValue = 1200000, description = "The TTL of a cookie based token, in ms")
  static final String TTL = "sakai.auth.trusted.token.ttl";

  /** Property to indicate the TTL on cookies */
  @Property(value = "sakai-trusted-authn", description = "The name of the token")
  static final String COOKIE_NAME = "sakai.auth.trusted.token.name";

  /**
   * If True, sessions will be used, if false cookies.
   */
  private boolean usingSession = false;

  /**
   * Shoudl the cookies go over ssl.
   */
  private boolean secureCookie = false;
  /**
   * The ttl of the cookie before it becomes invalid (in ms)
   */
  private long ttl = 20L * 60000L; // 20 minutes

  /**
   * The name of the authN token.
   */
  private String trustedAuthCookieName;

  /**
   * The time when a new token should be created.
   */
  private long nextUpdate = System.currentTimeMillis();
  /**
   * The location of the current token.
   */
  private int currentToken = 0;
  /**
   * A ring of tokens used to encypt.
   */
  private SecretKey[] currentTokens = new SecretKey[5];
  /**
   * A secure random used for generating new tokens.
   */
  private SecureRandom random;

  /**
   * An optional cookie server can be used in a cluster to centralize the management of
   * authN tokens. This is for situations where session storage and replication is not
   * desired, and session affinity can't be tolerated. Without this clients must come back
   * to the same host where they were authenticated as the cookie encode decode has
   * entropy associated with the instance of the server they are operating on.
   */
  @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
  private ClusterCookieServer clusterCookieServer;

  /**
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws UnsupportedEncodingException
   * @throws IllegalStateException
   * 
   */
  public TrustedTokenServiceImpl() throws NoSuchAlgorithmException, InvalidKeyException,
      IllegalStateException, UnsupportedEncodingException {
    random = SecureRandom.getInstance(SHA1PRNG);
    // warm up the crypto API
    Mac m = Mac.getInstance(HMAC_SHA1);
    byte[] b = new byte[20];
    random.nextBytes(b);
    SecretKey secretKey = new SecretKeySpec(b, HMAC_SHA1);
    m.init(secretKey);
    m.update(UTF_8.getBytes("UTF-8"));
    m.doFinal();

  }

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext context) {
    Dictionary props = context.getProperties();
    usingSession = (Boolean) props.get(USE_SESSION);
    secureCookie = (Boolean) props.get(SECURE_COOKIE);
    ttl = (Long) props.get(TTL);
    trustedAuthCookieName = (String) props.get(COOKIE_NAME);
  }

  /**
   * Extract credentials from the request.
   * 
   * @param req
   * @return credentials associated with the request.
   */
  public Credentials getCredentials(HttpServletRequest req, HttpServletResponse response) {
    Credentials cred = null;
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
            String userId = decodeCookie(c.getValue());
            if (userId != null) {
              cred = createCredentials(userId);
              refreshToken(response, c.getValue(), userId);
              break;
            }
          }
        }
      }
    }
    return cred;
  }

  /**
   * Remove credentials so that subsequent request dont contain credentials.
   * 
   * @param request
   * @param response
   */
  public void dropCredentials(HttpServletRequest request, HttpServletResponse response) {
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
   * Inject a token into the request/response
   * 
   * @param req
   * @param resp
   */
  public void injectToken(HttpServletRequest request, HttpServletResponse response) {
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
    if (parts != null && parts.length == 3) {
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
      SecureCookie secretKeyHolder = getActiveToken();

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
        SecureCookie secureCookie = new SecureCookie();
        return secureCookie.decode(value);
      } catch (SecureCookieException e) {
        LOG.error(e.getMessage());
      }
    }
    return null;

  }

  /**
   * Maintain a circular buffer to tokens, and return the current one.
   * 
   * @return the current token.
   */
  private synchronized SecureCookie getActiveToken() {
    if (System.currentTimeMillis() > nextUpdate || currentTokens[currentToken] == null) {

      // cycle so that during a typical ttl the tokens get completely refreshed.
      long tnow = System.currentTimeMillis();
      nextUpdate = System.currentTimeMillis() + ttl / (currentTokens.length - 1);
      byte[] b = new byte[20];
      random.nextBytes(b);

      SecretKey newToken = new SecretKeySpec(b, HMAC_SHA1);
      int nextToken = currentToken + 1;
      if (nextToken == currentTokens.length) {
        nextToken = 0;
      }
      currentTokens[nextToken] = newToken;
      currentToken = nextToken;
    }
    return new SecureCookie(currentToken);
  }

  /**
   * Encode a byte array.
   * 
   * @param base
   * @return
   */
  private String byteToHex(byte[] base) {
    char[] c = new char[base.length * 2];
    int i = 0;

    for (byte b : base) {
      int j = b;
      j = j + 128;
      c[i++] = TOHEX[j / 0x10];
      c[i++] = TOHEX[j % 0x10];
    }
    return new String(c);
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
