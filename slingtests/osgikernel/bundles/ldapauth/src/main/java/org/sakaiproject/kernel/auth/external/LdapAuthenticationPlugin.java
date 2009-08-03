package org.sakaiproject.kernel.auth.external;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPSocketFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.osgi.service.component.ComponentContext;

import java.io.UnsupportedEncodingException;
import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

/**
 * Authentication plugin for verifying a user against an LDAP instance.
 */
@Service
@Component
public class LdapAuthenticationPlugin implements AuthenticationPlugin {
  @Property(value = "localhost")
  static final String LDAP_HOST = "sakai.ldap.host";

  @Property(intValue = LDAPConnection.DEFAULT_SSL_PORT)
  static final String LDAP_PORT = "sakai.ldap.port";

  @Property(boolValue = true)
  static final String LDAP_CONNECTION_SECURE = "sakai.ldap.connection.secure";

  @Property
  static final String LDAP_LOGIN_DN = "sakai.ldap.loginDn";

  @Property
  static final String LDAP_LOGIN_PASSWORD = "sakai.ldap.login.password";

  @Property
  static final String LDAP_BASE_DN = "sakai.ldap.baseDn";

  @Property
  static final String LDAP_ATTR_PASSWORD = "sakai.ldap.attribute.password";

  private boolean useSecure;
  private String host;
  private int port;
  private String loginDn;
  private String password;
  private String baseDn;
  private String passwordAttributeName;

  private LDAPConnection conn;

  protected void activate(ComponentContext ctx) {
    Dictionary props = ctx.getProperties();

    useSecure = Boolean.parseBoolean((String) props.get(LDAP_CONNECTION_SECURE));
    host = (String) props.get(LDAP_HOST);
    port = (Integer) props.get(LDAP_PORT);
    loginDn = (String) props.get(LDAP_LOGIN_DN);
    password = (String) props.get(LDAP_LOGIN_PASSWORD);
    baseDn = (String) props.get(LDAP_BASE_DN);
    passwordAttributeName = (String) props.get(LDAP_ATTR_PASSWORD);

    // create connection to ldap. this does not establish the connection.
    if (useSecure) {
      // Dynamically set JSSE as a security provider
      // Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

      // Dynamically set the property that JSSE uses to identify
      // the keystore that holds trusted root certificates
      // System.setProperty("javax.net.ssl.trustStore", path);

      LDAPSocketFactory factory = new LDAPJSSESecureSocketFactory();
      LDAPConnection.setSocketFactory(factory);
    }

    conn = new LDAPConnection();

    try {
      // establish the connection to ldap
      conn.connect(host, port);
      conn.bind(LDAPConnection.LDAP_V3, loginDn, password.getBytes("UTF8"));
    } catch (LDAPException le) {
      throw new RuntimeException(le.getMessage(), le);
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee.getMessage(), uee);
    }
  }

  protected void deactivate(ComponentContext ctx) {
    if (conn.isConnected()) {
      try {
        conn.disconnect();
      } catch (LDAPException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  public static boolean canHandle(Credentials credentials) {
    return credentials instanceof SimpleCredentials;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;

      // TODO check credentials against ldap instance
      LDAPAttribute passwordAttr = new LDAPAttribute(passwordAttributeName, new String(sc
          .getPassword()));

      try {
        auth = conn.compare(baseDn + "/" + sc.getUserID(), passwordAttr);
      } catch (LDAPException e) {
        throw new RepositoryException(e.getMessage(), e);
      }
    }
    return auth;
  }
}
