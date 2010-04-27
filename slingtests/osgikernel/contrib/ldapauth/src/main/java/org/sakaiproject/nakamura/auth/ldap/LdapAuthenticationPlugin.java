package org.sakaiproject.nakamura.auth.ldap;

import com.novell.ldap.LDAPConnection;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapException;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;

import java.security.Principal;
import java.util.Map;
import java.util.logging.Level;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  @Property(cardinality = Integer.MAX_VALUE)
  static final String REQUIRED_ATTRIBUTES = "sakai.auth.ldap.reqattrs";

  @Reference
  private LdapConnectionManager connMgr;

  @Reference
  private SlingRepository repository;

  public LdapAuthenticationPlugin() {
  }

  LdapAuthenticationPlugin(LdapConnectionManager connMgr) {
    this.connMgr = connMgr;
  }

  @Activate
  protected void activate(Map<?, ?> props) {
    baseDn = OsgiUtil.toString(props.get(LDAP_BASE_DN), "");
    String attrs = OsgiUtil.toString(props.get(REQUIRED_ATTRIBUTES), "");
  }

  @Modified
  protected void modified(Map<?, ?> props) {
    baseDn = OsgiUtil.toString(props.get(LDAP_BASE_DN), null);
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

        ensureUserExists(sc.getUserID());
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

  private void ensureUserExists(final String principalName) {
    Session session = null;

    try {
      session = repository.loginAdministrative(null);
      
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(principalName);

      if (authorizable == null) {
        // create user
        log.debug("Createing user {}", principalName);
        userManager.createUser(principalName,
            RandomStringUtils.random(32),
            new Principal() {
              public String getName() {
                return principalName;
              }
            },
            PathUtils
                .getUserPrefix(principalName, UserConstants.DEFAULT_HASH_LEVELS));
      }
    } catch (RepositoryException e) {
      log.error(e.getMessage(), e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }
}
