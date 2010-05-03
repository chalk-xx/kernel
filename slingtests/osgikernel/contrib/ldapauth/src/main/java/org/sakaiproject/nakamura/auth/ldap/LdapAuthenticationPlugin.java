package org.sakaiproject.nakamura.auth.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

/**
 * Authentication plugin for verifying a user against an LDAP instance.
 */
@Component(metatype = true)
@Service(value = LdapAuthenticationPlugin.class)
public class LdapAuthenticationPlugin implements AuthenticationPlugin {

  private static final Logger log = LoggerFactory
      .getLogger(LdapAuthenticationPlugin.class);

  private static final String UTF8 = "UTF-8";

  @Property(value = "o=sakai")
  static final String LDAP_BASE_DN = "sakai.auth.ldap.baseDn";
  private String baseDn;

  @Property(value = "uid={}")
  static final String USER_FILTER = "sakai.auth.ldap.filter.user";
  private String userFilter;

  /**
   * Filter applied to make sure user has the required authorization (ie. attributes).
   */
  @Property(value = "(&(allowSakai=true))")
  static final String AUTHZ_FILTER = "sakai.auth.ldap.filter.authz";
  private String authzFilter;

  @Reference
  private LdapConnectionManager connMgr;

  public LdapAuthenticationPlugin() {
  }

  LdapAuthenticationPlugin(LdapConnectionManager connMgr) {
    this.connMgr = connMgr;
  }

  @Activate
  protected void activate(Map<?, ?> props) {
    init(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    init(props);
  }

  private void init(Map<?, ?> props) {
    baseDn = OsgiUtil.toString(props.get(LDAP_BASE_DN), "");
    userFilter = OsgiUtil.toString(props.get(USER_FILTER), "");
    authzFilter = OsgiUtil.toString(props.get(AUTHZ_FILTER), "");
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      // get application user credentials
      String appUser = connMgr.getConfig().getLdapUser();
      String appPass = connMgr.getConfig().getLdapPassword();

      // get user credentials
      SimpleCredentials sc = (SimpleCredentials) credentials;
      String userDn = userFilter.replace("{}", sc.getUserID());
      String userPass = new String(sc.getPassword());

      LDAPConnection conn = null;
      try {
        // 0) Get a connection to the server
        try {
          conn = connMgr.getConnection();
          log.debug("Connected to LDAP server");
        } catch (LDAPException e) {
          throw new IllegalStateException("Unable to connect to LDAP server ["
              + connMgr.getConfig().getLdapHost() + "]");
        }

        // 1) Bind as app user
        try {
          conn.bind(LDAPConnection.LDAP_V3, appUser, appPass.getBytes(UTF8));
          log.debug("Bound as application user");
        } catch (LDAPException e) {
          throw new IllegalArgumentException("Can't bind application user [" + appUser
              + "]", e);
        }

        // 2) Search for username (not authz).
        // If search fails, log/report invalid username or password.
        LDAPSearchResults results = conn.search(baseDn, LDAPConnection.SCOPE_ONE, userDn,
            null, false);
        if (results.getCount() == 0) {
          throw new IllegalArgumentException("Can't find user [" + userDn + "]");
        } else {
          log.debug("Found user via search");
        }

        // 3) Bind as username.
        // If bind fails, log/report invalid username or password.
        try {
          conn.bind(LDAPConnection.LDAP_V3, userDn + ", " + baseDn, userPass
              .getBytes(UTF8));
          log.debug("Bound as user");
        } catch (LDAPException e) {
          log.warn("Can't bind user [{}]", userDn);
          throw e;
        }

        if (authzFilter.length() > 0) {
          // 4) Return to app user
          try {
            conn.bind(LDAPConnection.LDAP_V3, appUser, appPass.getBytes(UTF8));
            log.debug("Rebound as application user");
          } catch (LDAPException e) {
            log.warn("{}]", appUser);
            throw new IllegalArgumentException("Can't bind application user [" + appUser
                + "]");
          }

          // 5) Search user DN with authz filter
          // If search fails, log/report that user is not authorized
          String userAuthzFilter = "(&(" + userDn + ")(" + authzFilter + "))";
          results = conn.search(baseDn, LDAPConnection.SCOPE_ONE, userAuthzFilter, null,
              false);
          if (results.getCount() == 0) {
            throw new IllegalArgumentException("User not authorized [" + userDn + "]");
          } else {
            log.debug("Found user + authz filter via search");
          }
        }

        // FINALLY!
        auth = true;
      } catch (Exception e) {
        log.warn(e.getMessage(), e);

        if (e instanceof RepositoryException) {
          throw (RepositoryException) e;
        } else {
          throw new RepositoryException(e.getMessage(), e);
        }
      } finally {
        connMgr.returnConnection(conn);
      }
    }
    return auth;
  }
}
