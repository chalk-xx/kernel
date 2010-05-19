package org.sakaiproject.nakamura.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPJSSEStartTLSFactory;
import com.novell.ldap.LDAPSocketFactory;

import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class LdapUtil {
  private static final Logger log = LoggerFactory.getLogger(LdapUtil.class);

  /**
   * Initializes an LDAP socket factory is a non-default socket factory is needed. This
   * scenario becomes relevant when needing to connect using SSL or TLS. If no special
   * socket factory is needed, null is returned which is safe to provide to the
   * constructor of {@link LDAPConnection} or to
   * {@link LDAPConnection#setSocketFactory(LDAPSocketFactory)}.
   *
   * @param config
   *          The configuration used for connecting.
   * @return The proper socket factory based on the provided configuration. null if
   *         special socket factory is required.
   */
  public static LDAPSocketFactory initLDAPSocketFactory(LdapConnectionManagerConfig config) {
    LDAPSocketFactory socketFactory = null;

    if (config.isSecureConnection() || config.isTLS()) {
      log.debug("init(): initializing secure socket factory");
      try {
        // initialize the keystore which will create an SSL context by which
        // socket factories can be created. this allows for multiple keystores
        // to be managed without the use of system properties.
        SSLContext ctx = initKeystore(config.getKeystoreLocation(), config.getKeystorePassword());
        SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();
        if (config.isTLS()) {
          socketFactory = new LDAPJSSEStartTLSFactory(sslSocketFactory);
        } else {
          socketFactory = new LDAPJSSESecureSocketFactory(sslSocketFactory);
        }
      } catch (GeneralSecurityException e) {
        log.error(e.getMessage(), e);
        throw new RuntimeException(e.getMessage(), e);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    return socketFactory;
  }

  /**
   * Loads a keystore and sets up an SSL context that can be used to create
   * socket factories that use the suggested keystore.
   *
   * @param keystoreLocation
   * @param keystorePassword
   * @throws CertificateException
   * @throws KeyStoreException
   * @throws NoSuchProviderException
   * @throws NoSuchAlgorithmException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NullPointerException
   *           if a non-null keystore location cannot be resolved
   */
  public static SSLContext initKeystore(String keystoreLocation, String keystorePassword)
      throws GeneralSecurityException, IOException {
    FileInputStream fis = new FileInputStream(keystoreLocation);
    char[] passChars = (keystorePassword != null) ? keystorePassword.toCharArray() : null;
    TrustManager[] myTM = new TrustManager[] { new LdapX509TrustManager(fis, passChars) };
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, myTM, null);
    return ctx;
  }

  /**
   * Escapes input intended for filter using a DN.<br/>
   * http://www.owasp.org/index.php/Preventing_LDAP_Injection_in_Java<br/>
   * <table>
   * <tr>
   * <th>Input</th>
   * <th>Output</th>
   * </tr>
   * <tr>
   * <td>\</td>
   * <td>\\</td>
   * </tr>
   * <tr>
   * <td>,</td>
   * <td>\,</td>
   * </tr>
   * <tr>
   * <td>+</td>
   * <td>\+</td>
   * </tr>
   * <tr>
   * <td>&quot;</td>
   * <td>\&quot;</td>
   * </tr>
   * <tr>
   * <td>&lt;</td>
   * <td>\&lt;</td>
   * </tr>
   * <tr>
   * <td>&gt;</td>
   * <td>\&gt;</td>
   * </tr>
   * <tr>
   * <td>;</td>
   * <td>\;</td>
   * </tr>
   * </table>
   *
   * @param name
   * @return
   */
  public static String escapeDN(String name) {
    StringBuilder sb = new StringBuilder();
    if ((name.length() > 0) && ((name.charAt(0) == ' ') || (name.charAt(0) == '#'))) {
      sb.append('\\'); // add the leading backslash if needed
    }
    for (int i = 0; i < name.length(); i++) {
      char curChar = name.charAt(i);
      switch (curChar) {
      case '\\':
        sb.append("\\\\");
        break;
      case ',':
        sb.append("\\,");
        break;
      case '+':
        sb.append("\\+");
        break;
      case '"':
        sb.append("\\\"");
        break;
      case '<':
        sb.append("\\<");
        break;
      case '>':
        sb.append("\\>");
        break;
      case ';':
        sb.append("\\;");
        break;
      default:
        sb.append(curChar);
      }
    }
    if ((name.length() > 1) && (name.charAt(name.length() - 1) == ' ')) {
      sb.insert(sb.length() - 1, '\\'); // add the trailing backslash if needed
    }
    return sb.toString();
  }

  /**
   * Escapes input intended for filter using a search.<br/>
   * http://www.owasp.org/index.php/Preventing_LDAP_Injection_in_Java<br/>
   * <table>
   * <tr>
   * <th>Input</th>
   * <th>Output</th>
   * </tr>
   * <tr>
   * <td>\</td>
   * <td>\5c</td>
   * </tr>
   * <tr>
   * <td>*</td>
   * <td>\2a</td>
   * </tr>
   * <tr>
   * <td>(</td>
   * <td>\28</td>
   * </tr>
   * <tr>
   * <td>)</td>
   * <td>\29</td>
   * </tr>
   * <tr>
   * <td>\u0000</td>
   * <td>\00</td>
   * </tr>
   * </table>
   *
   * @param filter
   * @return
   */
  public static final String escapeLDAPSearchFilter(String filter) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < filter.length(); i++) {
      char curChar = filter.charAt(i);
      switch (curChar) {
      case '\\':
        sb.append("\\5c");
        break;
      case '*':
        sb.append("\\2a");
        break;
      case '(':
        sb.append("\\28");
        break;
      case ')':
        sb.append("\\29");
        break;
      case '\u0000':
        sb.append("\\00");
        break;
      default:
        sb.append(curChar);
      }
    }
    return sb.toString();
  }
}
