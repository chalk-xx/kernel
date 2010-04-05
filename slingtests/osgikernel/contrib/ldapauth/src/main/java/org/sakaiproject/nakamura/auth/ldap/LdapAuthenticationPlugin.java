package org.sakaiproject.nakamura.auth.ldap;

import com.novell.ldap.LDAPConnection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

import java.util.Dictionary;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

/**
 * Authentication plugin for verifying a user against an LDAP instance.
 */
@Component(metatype = true)
@Service(value = LdapAuthenticationPlugin.class)
public class LdapAuthenticationPlugin implements AuthenticationPlugin {
  private static final String BROKER_NAME = LdapAuthenticationPlugin.class.getName();

  @Property(value = "localhost")
  static final String LDAP_HOST = "sakai.auth.ldap.host";

  @Property(intValue = LDAPConnection.DEFAULT_PORT)
  static final String LDAP_PORT = "sakai.auth.ldap.port";

  @Property(boolValue = false)
  static final String LDAP_CONNECTION_SECURE = "sakai.auth.ldap.connection.secure";

  @Property
  static final String KEYSTORE_LOCATION = "sakai.auth.ldap.keystore.location";

  @Property
  static final String KEYSTORE_PASSWORD = "sakai.auth.ldap.keystore.password";

  @Property
  static final String LDAP_BASE_DN = "sakai.auth.ldap.baseDn";
  private String baseDn;

  @Reference
  protected LdapConnectionBroker connBroker;

  private LdapConnectionManager connMgr;

  @Activate
  protected void activate(ComponentContext ctx) {
    Dictionary<?, ?> props = ctx.getProperties();
    LdapConnectionManagerConfig config = connBroker.getDefaultConfig();

    Boolean useSecure = (Boolean) props.get(LDAP_CONNECTION_SECURE);
    if (useSecure != null) {
      config.setSecureConnection(useSecure);
    }

    String host = (String) props.get(LDAP_HOST);
    if (host != null && host.length() > 0) {
      config.setLdapHost(host);
    }

    Integer port = (Integer) props.get(LDAP_PORT);
    if (port != null) {
      config.setLdapPort(port);
    }

    String keystoreLocation = (String) props.get(LDAP_HOST);
    config.setKeystoreLocation(keystoreLocation);

    String keystorePassword = (String) props.get(LDAP_HOST);
    config.setKeystorePassword(keystorePassword);

    baseDn = (String) props.get(LDAP_BASE_DN);

    try {
      // establish the connection to ldap
      connMgr = connBroker.create(BROKER_NAME, config);
    } catch (LdapException le) {
      throw new RuntimeException(le.getMessage(), le);
    }
  }

  protected void deactivate(ComponentContext ctx) {
    connBroker.destroy(BROKER_NAME);
  }

  public boolean canHandle(Credentials credentials) {
    return credentials instanceof SimpleCredentials;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;
      String dn = getBaseDn(sc.getUserID());
      String pass = new String(sc.getPassword());

      try {
        LDAPConnection conn = connMgr.getBoundConnection(dn, pass);
        auth = true;
        connMgr.returnConnection(conn);
      } catch (LdapException e) {
        throw new RepositoryException(e.getMessage(), e);
      }
    }
    return auth;
  }

  protected String getBaseDn(String userId) {
    String dn = baseDn.replace("{}", userId);
    return dn;
  }
}
