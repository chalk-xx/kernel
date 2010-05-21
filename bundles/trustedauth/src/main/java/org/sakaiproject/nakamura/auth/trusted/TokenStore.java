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
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A Token Storage class that maintains a local ring buffer of keys for encoding and uses
 * a cluster replecated cache for keys to be shared with other servers in the cluster.
 */
public class TokenStore {

  /**
   * Thrown when there is a problem with a Cookie.
   */
  public static final class SecureCookieException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -1291914895288707428L;

    /**
     * @param string
     */
    public SecureCookieException(String string) {
      super(string);
    }

  }

  /**
   * A secure cookie, with encoding and decoding methods.
   */
  public final class SecureCookie {

    /**
     * The secret key used to encode this cookie
     */
    private SecretKey secretKey;
    /**
     * The ID of the secret key.
     */
    private int secretKeyId;
    private String serverId;

    /**
     * Create the token, using the secure key number specified.
     * 
     * @param secretKeyId
     * @param secretKey
     */
    public SecureCookie(String serverId, int secretKeyId) {
      this.secretKeyId = secretKeyId;
      this.serverId = serverId;
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
     * Encode the cookie for a user, a serverId and an expiry
     * 
     * @param expires
     *          the time of expiry
     * @param userId
     *          the user id
     * @param serverId
     *          the id of the server where the
     * @return
     * @throws UnsupportedEncodingException
     * @throws IllegalStateException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SecureCookieException
     */
    public String encode(long expires, String userId) throws IllegalStateException,
        UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException,
        SecureCookieException {
      String cookiePayload = String.valueOf(secretKeyId) + String.valueOf(expires) + "@"
          + userId + "@" + serverId;
      Mac m = Mac.getInstance(HMAC_SHA1);
      ExpiringSecretKey expiringSecretKey = TokenStore.this.getSecretKey(serverId,
          secretKeyId);
      if (expiringSecretKey == null) {
        throw new SecureCookieException("Key "
            + TokenStore.this.getSecretKey(serverId, secretKeyId) + " not found ");
      }
      m.init(TokenStore.this.getSecretKey(serverId, secretKeyId).getSecretKey());
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
      if (parts != null && parts.length == 4) {
        this.secretKeyId = Integer.parseInt(parts[1].substring(0, 1));
        this.serverId = parts[3];
        long cookieTime = Long.parseLong(parts[1].substring(1));
        if (System.currentTimeMillis() < cookieTime) {
          try {
            ExpiringSecretKey expiringSecretKey = TokenStore.this.getSecretKey(serverId,
                secretKeyId);
            if (expiringSecretKey == null) {
              throw new SecureCookieException("No Secure Key found "
                  + getCacheKey(serverId, secretKeyId));
            }
            this.secretKey = expiringSecretKey.getSecretKey();
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

  public static final Logger LOG = LoggerFactory.getLogger(TokenStore.class);

  /**
   * 
   */
  private static final char[] TOHEX = "0123456789abcdef".toCharArray();

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

  private static final String DEFAULT_TOKEN_FILE = "sling/cookie-tokens.bin";
  /**
   * The ttl of the cookie before it becomes invalid (in ms)
   */
  private long ttl = 20L * 60000L; // 20 minutes

  /**
   * The time when a new token should be created.
   */
  private long nextUpdate = System.currentTimeMillis();
  /**
   * The location of the current token.
   */
  private int secretKeyId = 0;
  /**
   * A ring of tokens used to encypt.
   */
  private ExpiringSecretKey[] secretKeyRingBuffer;
  /**
   * A secure random used for generating new tokens.
   */
  private SecureRandom random;

  /**
   * File where the secure keys are written to.
   */
  private File tmpTokenFile;

  /**
   * File where the secure keys are read from.
   */
  private File tokenFile;

  /**
   * The Id of this server
   */
  private String serverId;

  /**
   * The cache manager.
   */
  private CacheManagerService cacheManager;

  /**
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws UnsupportedEncodingException
   * @throws IllegalStateException
   * 
   */
  public TokenStore() throws NoSuchAlgorithmException, InvalidKeyException,
      IllegalStateException, UnsupportedEncodingException {
    random = SecureRandom.getInstance(SHA1PRNG);
    // warm up the crypto API
    Mac m = Mac.getInstance(HMAC_SHA1);
    byte[] b = new byte[20];
    random.nextBytes(b);
    SecretKey secretKey = new SecretKeySpec(b, HMAC_SHA1);
    m.init(secretKey);
    m.update(UTF_8.getBytes(UTF_8));
    m.doFinal();
    this.tokenFile = new File(DEFAULT_TOKEN_FILE);
    tmpTokenFile = new File(DEFAULT_TOKEN_FILE + ".tmp");
  }

  /**
   * Initialise the token store.
   * 
   * @param cacheManager
   *          the cache manager
   * @param tokenFile
   *          file where the local secret keys are stored.
   * @param serverId
   *          id of this server.
   * @param ttl
   *          the ttl of cookies.
   */
  public void doInit(CacheManagerService cacheManager, String tokenFile, String serverId,
      long ttl) {
    this.tokenFile = new File(tokenFile);
    tmpTokenFile = new File(tokenFile + ".tmp");
    this.serverId = serverId;
    this.ttl = ttl;
    this.cacheManager = cacheManager;
    getActiveToken();
  }

  /**
   * Maintain a circular buffer to tokens, and return the current one.
   * 
   * @return the current token.
   */
  synchronized SecureCookie getActiveToken() {
    if (secretKeyRingBuffer == null) {
      loadLocalSecretKeys();
    }
    if (System.currentTimeMillis() > nextUpdate
        || hasExpired(secretKeyRingBuffer[secretKeyId]) ) {
      // cycle so that during a typical ttl the tokens get completely refreshed.
      nextUpdate = System.currentTimeMillis() + ttl / 2;
      byte[] b = new byte[20];
      random.nextBytes(b);

      // the key will last 2x ttl so far longer than the cookie. There are 5 tokens, to
      // the key expires before
      // being replaced, this is important in a clustered environment.
      ExpiringSecretKey expiringSecretKey = new ExpiringSecretKey(b, HMAC_SHA1, System
          .currentTimeMillis()
          + (ttl * 2));
      int nextToken = secretKeyId + 1;
      if (nextToken == secretKeyRingBuffer.length) {
        nextToken = 0;
      }
      secretKeyRingBuffer[nextToken] = expiringSecretKey;
      getServerKeyCache().put(getCacheKey(serverId, nextToken),
          expiringSecretKey.getSecretKeyData());
      secretKeyId = nextToken;
      saveLocalSecretKeys();
    }
    return new SecureCookie(serverId, secretKeyId);
  }

  /**
   * @param expiringSecretKey
   * @return
   */
  private boolean hasExpired(ExpiringSecretKey expiringSecretKey) {
    return expiringSecretKey == null || (System.currentTimeMillis() > expiringSecretKey.getExpires());
  }

  /**
   * @return
   */
  private Cache<ExpiringSecretKeyData> getServerKeyCache() {
    return cacheManager.getCache(this.getClass().getName(), CacheScope.CLUSTERREPLICATED);
  }

  /**
   * Save all the secureKeys to file
   */
  private void saveLocalSecretKeys() {
    FileOutputStream fout = null;
    DataOutputStream keyOutputStream = null;
    try {
      File parent = tokenFile.getAbsoluteFile().getParentFile();
      LOG.info("Saving Local Secret Keys to {} ", tokenFile.getAbsoluteFile());
      if (!parent.exists()) {
        parent.mkdirs();
      }
      fout = new FileOutputStream(tmpTokenFile);
      keyOutputStream = new DataOutputStream(fout);
      keyOutputStream.writeInt(secretKeyId);
      keyOutputStream.writeLong(nextUpdate);
      for (int i = 0; i < secretKeyRingBuffer.length; i++) {
        if (secretKeyRingBuffer[i] == null) {
          keyOutputStream.writeInt(0);
        } else {
          keyOutputStream.writeInt(1);
          keyOutputStream.writeLong(secretKeyRingBuffer[i].getExpires());
          byte[] b = secretKeyRingBuffer[i].getSecretKey().getEncoded();
          keyOutputStream.writeInt(b.length);
          keyOutputStream.write(b);
        }
      }
      keyOutputStream.close();
      tmpTokenFile.renameTo(tokenFile);
    } catch (IOException e) {
      LOG.error("Failed to save cookie keys " + e.getMessage());
    } finally {
      try {
        keyOutputStream.close();
      } catch (Exception e) {
      }
      try {
        fout.close();
      } catch (Exception e) {
      }

    }
  }

  /**
   * 
   */
  private void loadLocalSecretKeys() {
    FileInputStream fin = null;
    DataInputStream keyInputStream = null;
    try {
      fin = new FileInputStream(tokenFile);
      keyInputStream = new DataInputStream(fin);
      int newCurrentToken = keyInputStream.readInt();
      long newNextUpdate = keyInputStream.readLong();
      ExpiringSecretKey[] newKeys = new ExpiringSecretKey[5];
      for (int i = 0; i < newKeys.length; i++) {
        int isNull = keyInputStream.readInt();
        if (isNull == 1) {
          long expires = keyInputStream.readLong();
          int l = keyInputStream.readInt();
          byte[] b = new byte[l];
          keyInputStream.read(b);
          newKeys[i] = new ExpiringSecretKey(b, HMAC_SHA1, expires);
          getServerKeyCache()
              .put(getCacheKey(serverId, i), newKeys[i].getSecretKeyData());
        } else {
          newKeys[i] = null;
        }
      }
      keyInputStream.close();
      nextUpdate = newNextUpdate;
      secretKeyId = newCurrentToken;
      secretKeyRingBuffer = newKeys;
    } catch (IOException e) {
      LOG.error("Failed to load cookie keys " + e.getMessage());
    } finally {
      try {
        keyInputStream.close();
      } catch (Exception e) {
      }
      try {
        fin.close();
      } catch (Exception e) {
      }
    }
    if (secretKeyRingBuffer == null) {
      secretKeyRingBuffer = new ExpiringSecretKey[5];
      nextUpdate = System.currentTimeMillis();
      secretKeyId = 0;
    }

  }

  /**
   * Get a cache key for the secret key.
   * 
   * @param serverId2
   * @param i
   * @return
   */
  private String getCacheKey(String serverId, int i) {
    return serverId + ":" + i;
  }

  /**
   * Get the secret key keyNumber from server serverId
   * 
   * @param serverId
   *          the server that owns the secret Key
   * @param keyNumber
   *          the key number
   * @return
   */
  private ExpiringSecretKey getSecretKey(String serverId, int keyNumber) {
    if (this.serverId.equals(serverId)) {
      return secretKeyRingBuffer[keyNumber];
    }
    String cacheKey = getCacheKey(serverId, keyNumber);
    Cache<ExpiringSecretKeyData> keyCache = getServerKeyCache();
    if (keyCache.containsKey(cacheKey)) {
      ExpiringSecretKeyData cachedServerKeyData = keyCache.get(cacheKey);
      if (cachedServerKeyData.getExpires() < System.currentTimeMillis()) {
        return new ExpiringSecretKey(cachedServerKeyData);
      }
    }

    // load tokens for the server up
    if (keyCache.containsKey(cacheKey)) {
      ExpiringSecretKeyData cachedServerKeyData = keyCache.get(cacheKey);
      if (cachedServerKeyData.getExpires() < System.currentTimeMillis()) {
        return new ExpiringSecretKey(cachedServerKeyData);
      }
    }
    // none found.
    return null;
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
   * @return
   */
  public SecureCookie getSecureCookie() {
    return new SecureCookie();
  }

}
