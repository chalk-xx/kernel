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
 *
 */
public class TokenStore {
  /**
  *
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

  private static final String DEFAULT_TOKEN_FILE = "cookie-tokens.bin";
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
  private int currentToken = 0;
  /**
   * A ring of tokens used to encypt.
   */
  private SecretKey[] currentTokens;
  /**
   * A secure random used for generating new tokens.
   */
  private SecureRandom random;

  private File tmpTokenFile;

  private File tokenFile;

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
    tmpTokenFile = new File(DEFAULT_TOKEN_FILE+".tmp");    
    getActiveToken();
  }
  
  public void setTokenFile(String tokenFile) {
    this.tokenFile = new File(tokenFile);
    tmpTokenFile = new File(tokenFile+".tmp");    
    getActiveToken();
  }

  /**
   * Maintain a circular buffer to tokens, and return the current one.
   * 
   * @return the current token.
   */
  synchronized SecureCookie getActiveToken() {
    if (currentTokens == null) {
      loadTokens();
    }
    if (System.currentTimeMillis() > nextUpdate || currentTokens[currentToken] == null) {
      // cycle so that during a typical ttl the tokens get completely refreshed.
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
      saveTokens();
    }
    return new SecureCookie(currentToken);
  }

  /**
   * 
   */
  private void saveTokens() {
    FileOutputStream fout = null;
    DataOutputStream keyOutputStream = null;
    try {
      File parent = tokenFile.getAbsoluteFile().getParentFile();
      LOG.info("Token File {} parent {} ", tokenFile, parent);
      if ( !parent.exists() ) {
        parent.mkdirs();
      }
      fout = new FileOutputStream(tmpTokenFile);
      keyOutputStream = new DataOutputStream(fout);
      keyOutputStream.writeInt(currentToken);
      keyOutputStream.writeLong(nextUpdate);
      for (int i = 0; i < currentTokens.length; i++) {
        if (currentTokens[i] == null) {
          keyOutputStream.writeInt(0);
        } else {
          keyOutputStream.writeInt(1);
          byte[] b = currentTokens[i].getEncoded();
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
  private void loadTokens() {
    FileInputStream fin = null;
    DataInputStream keyInputStream = null;
    try {
      fin = new FileInputStream(tokenFile);
      keyInputStream = new DataInputStream(fin);
      int newCurrentToken = keyInputStream.readInt();
      long newNextUpdate = keyInputStream.readLong();
      SecretKey[] newKeys = new SecretKey[5];
      for (int i = 0; i < newKeys.length; i++) {
        int isNull = keyInputStream.readInt();
        if (isNull == 1) {
          int l = keyInputStream.readInt();
          byte[] b = new byte[l];
          keyInputStream.read(b);
          newKeys[i] = new SecretKeySpec(b, HMAC_SHA1);
        } else {
          newKeys[i] = null;
        }
      }
      keyInputStream.close();
      nextUpdate = newNextUpdate;
      currentToken = newCurrentToken;
      currentTokens = newKeys;
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
    if ( currentTokens == null ) {
      currentTokens = new SecretKey[5];
      nextUpdate = System.currentTimeMillis();
      currentToken = 0;
    }

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

  /**
   * @param ttl
   */
  public void setTtl(long ttl) {
    this.ttl = ttl;
  }

}
