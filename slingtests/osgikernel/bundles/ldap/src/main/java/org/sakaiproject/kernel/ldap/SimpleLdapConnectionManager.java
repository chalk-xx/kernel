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
package org.sakaiproject.kernel.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPJSSEStartTLSFactory;

import org.sakaiproject.kernel.api.ldap.LdapConnectionManager;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.api.ldap.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Allocates connected, constrained, and optionally bound and secure
 * <code>LDAPConnections</code>
 *
 * @see LdapConnectionManagerConfig
 * @author Dan McCallum, Unicon Inc
 * @author John Lewis, Unicon Inc
 */
public class SimpleLdapConnectionManager implements LdapConnectionManager {

  // public static final String KEYSTORE_LOCATION_SYS_PROP_KEY =
  // "javax.net.ssl.trustStore";
  // public static final String KEYSTORE_PASSWORD_SYS_PROP_KEY =
  // "javax.net.ssl.trustStorePassword";

  /** Class-specific logger */
  private static Logger log = LoggerFactory.getLogger(SimpleLdapConnectionManager.class);

  /** connection allocation configuration */
  private LdapConnectionManagerConfig config;

  /**
   * {@inheritDoc}
   */
  public void init() throws LdapException {

    log.debug("init()");

    if (config.isSecureConnection()) {
      log.debug("init(): initializing secure socket factory");
      try {
        // initialize the keystore which will create an SSL context by which
        // socket factories can be created. this allows for multiple keystores
        // to be managed without the use of system properties.
        SSLContext ctx = initKeystore(config.getKeystoreLocation(), config.getKeystorePassword());
        SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();
        if (config.isTLS()) {
          LDAPConnection.setSocketFactory(new LDAPJSSEStartTLSFactory(sslSocketFactory));
        } else {
          LDAPConnection.setSocketFactory(new LDAPJSSESecureSocketFactory(sslSocketFactory));
        }
      } catch (GeneralSecurityException e) {
        log.error(e.getMessage(), e);
        throw new LdapException(e.getMessage(), e);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
        throw new LdapException(e.getMessage(), e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.ldap.LdapConnectionManager#destroy()
   */
  public void destroy() {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   */
  public LDAPConnection getConnection() throws LdapException {
    log.debug("getConnection()");

    try {
      LDAPConnection conn = newLDAPConnection();
      applyConstraints(conn);
      connect(conn);

      if (config.isAutoBind()) {
        log.debug("getConnection(): auto-binding");
        bind(conn, config.getLdapUser(), config.getLdapPassword());
      }

      return conn;
    } catch (LDAPException e) {
      throw new LdapException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public LDAPConnection getBoundConnection() throws LdapException {
    log.debug("getBoundConnection(): [dn = {}]", config.getLdapUser());

    try {
      LDAPConnection conn = newLDAPConnection();
      applyConstraints(conn);
      connect(conn);
      bind(conn, config.getLdapUser(), config.getLdapPassword());

      return conn;
    } catch (LDAPException e) {
      throw new LdapException(e.getMessage(), e);
    }
  }

  protected LDAPConnection newLDAPConnection() {
    LDAPConnection conn = new LDAPConnection();
    return conn;
  }

  private void bind(LDAPConnection conn, String dn, String pw) throws LDAPException {
    log.debug("bind(): binding [dn = {}]", dn);

    try {
      byte[] password = pw.getBytes("UTF8");
      conn.bind(LDAPConnection.LDAP_V3, dn, password);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Failed to encode user password", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void returnConnection(LDAPConnection conn) {
    try {
      if (conn != null) {
        conn.disconnect();
      }
    } catch (LDAPException e) {
      log.error("returnConnection(): failed on disconnect: ", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setConfig(LdapConnectionManagerConfig config) {
    this.config = config;
  }

  /**
   * {@inheritDoc}
   */
  public LdapConnectionManagerConfig getConfig() {
    return config;
  }

  /**
   * Loads a keystore and sets up an SSL context that can be used to create
   * socket factories that use the suggested keystore.
   *
   * @param config
   * @throws CertificateException
   * @throws KeyStoreException
   * @throws NoSuchProviderException
   * @throws NoSuchAlgorithmException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NullPointerException
   *           if a non-null keystore location cannot be resolved
   */
  protected SSLContext initKeystore(String keystoreLocation, String keystorePassword)
      throws GeneralSecurityException, IOException {
    FileInputStream fis = new FileInputStream(keystoreLocation);
    char[] passChars = (keystorePassword != null) ? keystorePassword.toCharArray() : null;
    TrustManager[] myTM = new TrustManager[] { new LdapX509TrustManager(fis, passChars) };
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, myTM, null);
    return ctx;
  }

  /**
   * Applies {@link LDAPConstraints} to the specified {@link LDAPConnection}.
   * Implemented to assign <code>timeLimit</code> and
   * <code>referralFollowing</code> constraint values retrieved from the
   * currently assigned {@link LdapConnectionManagerConfig}.
   *
   * @param conn
   */
  protected void applyConstraints(LDAPConnection conn) {
    int timeout = config.getOperationTimeout();
    boolean followReferrals = config.isFollowReferrals();
    log.debug("applyConstraints(): values [timeout = {}][follow referrals = {}]", timeout,
        followReferrals);
    LDAPConstraints constraints = new LDAPConstraints();
    constraints.setTimeLimit(timeout);
    constraints.setReferralFollowing(followReferrals);
    conn.setConstraints(constraints);
  }

  /**
   * Connects the specified <code>LDAPConnection</code> to the currently
   * configured host and port.
   *
   * @param conn
   *          an <code>LDAPConnection</code>
   * @throws LDAPConnection
   *           if the connect attempt fails
   */
  protected void connect(LDAPConnection conn) throws LDAPException {
    log.debug("connect()");

    conn.connect(config.getLdapHost(), config.getLdapPort());

    try {
      postConnect(conn);
    } catch (LDAPException e) {
      log.error("Failed to completely initialize a connection [host = " + config.getLdapHost()
          + "][port = " + config.getLdapPort() + "]", e);
      try {
        conn.disconnect();
      } catch (LDAPException ee) {
      }

      throw e;
    } catch (Throwable e) {
      log.error("Failed to completely initialize a connection [host = " + config.getLdapHost()
          + "][port = " + config.getLdapPort() + "]", e);
      try {
        conn.disconnect();
      } catch (LDAPException ee) {
      }

      if (e instanceof Error) {
        throw (Error) e;
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }

      throw new RuntimeException("LDAPConnection allocation failure", e);
    }

  }

  protected void postConnect(LDAPConnection conn) throws LDAPException {

    log.debug("postConnect()");

    if (config.isSecureConnection() && config.isTLS()) {
      log.debug("postConnect(): starting TLS");
      conn.startTLS();
    }
  }
}
