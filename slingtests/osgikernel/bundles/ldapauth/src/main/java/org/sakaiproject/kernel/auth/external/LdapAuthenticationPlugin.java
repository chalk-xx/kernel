package org.sakaiproject.kernel.auth.external;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.osgi.service.component.ComponentContext;

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
  @Property
  static final String LDAP_HOST = "sakai.ldap.host";

  @Property
  static final String LDAP_LOGIN_USER = "sakai.ldap.user";

  @Property
  static final String LDAP_LOGIN_PASSWORD = "sakai.ldap.password";

  @Property
  static final String LDAP_BASE_DN = "sakai.ldap.baseDn";

  private String host;
  private String user;
  private String password;
  private String baseDn;

  protected void activate(ComponentContext ctx) {
    Dictionary props = ctx.getProperties();

    host = (String) props.get(LDAP_HOST);
    user = (String) props.get(LDAP_LOGIN_USER);
    password = (String) props.get(LDAP_LOGIN_PASSWORD);
    baseDn = (String) props.get(LDAP_BASE_DN);

    // TODO create connection to ldap
  }

  public static boolean canHandle(Credentials credentials) {
    return credentials instanceof SimpleCredentials;
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      SimpleCredentials sc = (SimpleCredentials) credentials;

      // TODO check credentials against ldap instance
    }
    return auth;
  }
}
