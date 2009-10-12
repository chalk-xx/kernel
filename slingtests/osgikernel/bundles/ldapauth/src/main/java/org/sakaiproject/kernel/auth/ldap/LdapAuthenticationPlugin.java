package org.sakaiproject.kernel.auth.ldap;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.auth.ldap.PasswordGuard;
import org.sakaiproject.kernel.api.ldap.LdapConnectionBroker;
import org.sakaiproject.kernel.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.kernel.api.ldap.LdapConstants;
import org.sakaiproject.kernel.api.ldap.LdapException;

import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;

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
  static final String LDAP_HOST = LdapConstants.HOST;

  @Property(intValue = LDAPConnection.DEFAULT_SSL_PORT)
  static final String LDAP_PORT = LdapConstants.PORT;

  @Property(boolValue = true)
  static final String LDAP_CONNECTION_SECURE = LdapConstants.SECURE_CONNECTION;

  @Property
  static final String LDAP_LOGIN_DN = LdapConstants.USER;

  @Property
  static final String LDAP_LOGIN_PASSWORD = LdapConstants.PASSWORD;

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

  @Reference
  protected LdapConnectionBroker connBroker;

  /** Using a concurrent hash map here to save from using a synchronized block later when we iterate over the password guards. */
  @Reference(referenceInterface = PasswordGuard.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private ConcurrentHashMap<String, PasswordGuard> passwordGuards = new ConcurrentHashMap<String, PasswordGuard>();

  protected void bindPasswordGuards(PasswordGuard guard) {
    passwordGuards.put(guard.toString(), guard);
  }

  protected void unbindPasswordGuard(PasswordGuard guard) {
    passwordGuards.remove(guard.toString());
  }

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
    config.setLdapPort(port);

    loginDn = (String) props.get(LDAP_LOGIN_DN);
    config.setLdapUser(loginDn);

    password = (String) props.get(LDAP_LOGIN_PASSWORD);
    config.setLdapPassword(password);

    baseDn = (String) props.get(LDAP_BASE_DN);
    passwordAttributeName = (String) props.get(LDAP_ATTR_PASSWORD);

    try {
      // establish the connection to ldap
      connBroker.create(BROKER_NAME);
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

      try {
        LDAPConnection conn = connBroker.getConnection(BROKER_NAME);
        String password = new String(sc.getPassword());
        // check credentials against ldap instance
        for (PasswordGuard guard : passwordGuards.values()) {
          String guarded = guard.guard(password);
          LDAPAttribute passwordAttr = new LDAPAttribute(passwordAttributeName, guarded);
          auth = conn.compare(baseDn + "/" + sc.getUserID(), passwordAttr);

          if (auth) {
            break;
          }
        }
      } catch (LdapException e) {
        throw new RepositoryException(e.getMessage(), e);
      } catch (LDAPException e) {
        throw new RepositoryException(e.getMessage(), e);
      }
    }
    return auth;
  }
}
