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

import java.io.UnsupportedEncodingException;
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

  public static final String DEFAULT_LDAP_BASE_DN = "uid={}";
  @Property(value = DEFAULT_LDAP_BASE_DN)
  static final String LDAP_BASE_DN = "sakai.auth.ldap.baseDn";
  private String baseDn;

  /**
   * Filter applied to make sure user has the required authorization (ie. attributes).
   */
  @Property
  static final String FILTER = "sakai.auth.ldap.filter";
  private String filter;

  @Reference
  private LdapConnectionManager connMgr;

  public LdapAuthenticationPlugin() {
  }

  LdapAuthenticationPlugin(LdapConnectionManager connMgr) {
    this.connMgr = connMgr;
  }

  @Activate
  protected void activate(Map<?, ?> props) {
    processProps(props);
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    processProps(props);
  }

  private void processProps(Map<?, ?> props) {
    baseDn = OsgiUtil.toString(props.get(LDAP_BASE_DN), "");
    filter = OsgiUtil.toString(props.get(FILTER), null);
  }

  public boolean authenticate(Credentials credentials) throws RepositoryException {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      // get application user credentials
      String appUser = connMgr.getConfig().getLdapUser();
      String appPass = connMgr.getConfig().getLdapPassword();

      // get user credentials
      SimpleCredentials sc = (SimpleCredentials) credentials;
      String user = sc.getUserID();
      String userDn = buildBaseDn(user);
      String userPass = new String(sc.getPassword());

      LDAPConnection conn = null;
      try {
        // 1) Bind as app user
        conn = connMgr.getBoundConnection(appUser, appPass);

        // 2) Search for username (not authz).
        // If search fails, log/report invalid username or password.
        LDAPSearchResults results = conn.search(userDn, LDAPConnection.SCOPE_ONE, null,
            null, false);
        if (results.getCount() == 0) {
          log.warn("Invalid username when check LDAP [{}]", user);
        } else {
          try {
            // 3) Bind as username.
            // If bind fails, log/report invalid username or password.
            conn.bind(LDAPConnection.LDAP_V3, userDn, userPass.getBytes("UTF-8"));

            // 4) Return to app user
            conn.bind(LDAPConnection.LDAP_V3, appUser, appPass.getBytes("UTF-8"));

            // 5) Search user DN with authz filter
            // If search fails, log/report that user is not authorized
            results = conn.search(userDn, LDAPConnection.SCOPE_ONE, filter, null, false);

            if (results.getCount() == 0) {
              log.warn("User not authorized to login [{}]", user);
            } else {
              // FINALLY!
              auth = true;
            }
          } catch (LDAPException e) {
            log.warn(e.getMessage(), e);
          } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(e.getMessage(), e);
          }
        }
      } catch (LDAPException e) {
        throw new RepositoryException(e.getMessage(), e);
      } finally {
        connMgr.returnConnection(conn);
      }
    }
    return auth;
  }

  /**
   * Builds the DN needed to search for the specified user.
   * 
   * @param userId
   * @return
   */
  protected String buildBaseDn(String userId) {
    String dn = baseDn.replace("{}", userId);
    return dn;
  }
}
