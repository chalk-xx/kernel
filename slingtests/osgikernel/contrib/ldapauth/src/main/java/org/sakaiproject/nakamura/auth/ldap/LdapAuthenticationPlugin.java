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
@Component(enabled = false, metatype = true)
@Service(value = LdapAuthenticationPlugin.class)
public class LdapAuthenticationPlugin implements AuthenticationPlugin {
  private static final String BROKER_NAME = LdapAuthenticationPlugin.class.getName();

  @Property(value = "localhost")
  static final String LDAP_HOST = "sakai.ldap.host";
  private String host;

  @Property(intValue = LDAPConnection.DEFAULT_SSL_PORT)
  static final String LDAP_PORT = "sakai.ldap.port";
  private Integer port;

  @Property(boolValue = true)
  static final String LDAP_CONNECTION_SECURE = "sakai.ldap.connection.secure";
  private boolean useSecure;

  @Property
  static final String LDAP_BASE_DN = "sakai.ldap.baseDn";
  private String baseDn;

  private LdapConnectionManager connMgr;

  @Reference
  protected LdapConnectionBroker connBroker;

  @Activate
  protected void activate(ComponentContext ctx) {
    Dictionary<?, ?> props = ctx.getProperties();
    LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();
    config.setAutoBind(true);

    useSecure = (Boolean) props.get(LDAP_CONNECTION_SECURE);
    config.setSecureConnection(useSecure);

    host = (String) props.get(LDAP_HOST);
    config.setLdapHost(host);

    port = (Integer) props.get(LDAP_PORT);
    if (port == null || port <= 0) {
      if (useSecure) {
        port = LDAPConnection.DEFAULT_SSL_PORT;
      } else {
        port = LDAPConnection.DEFAULT_PORT;
      }
    }
    config.setLdapPort(port);

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
