package org.sakaiproject.kernel.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trust manager implementation to allow for multiple keystores to be used when
 * creating LDAP connections. Implementation details derived from <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager"
 * >here.</a>
 */
class LdapX509TrustManager implements X509TrustManager {

  X509TrustManager sunX509TrustManager;

  LdapX509TrustManager(InputStream keystore, char[] password) throws NoSuchAlgorithmException,
      NoSuchProviderException, KeyStoreException, CertificateException, IOException {
    this(keystore, password, "SunX509", "SunJSSE");
  }

  LdapX509TrustManager(InputStream keystore, char[] password, String algorithm, String provider)
      throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException,
      CertificateException, IOException {
    // create sunX509TrustManager
    //
    // for example:
    // Create/load a keystore
    // Get instance of a "SunX509" TrustManagerFactory "tmf"
    // init the TrustManagerFactory with the keystore
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm, provider);
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(keystore, password);
    tmf.init(ks);
    sunX509TrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
  }

  public X509Certificate[] getAcceptedIssuers() {
    return sunX509TrustManager.getAcceptedIssuers();
  }

  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    sunX509TrustManager.checkClientTrusted(chain, authType);
  }

  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    sunX509TrustManager.checkServerTrusted(chain, authType);
  }
}
