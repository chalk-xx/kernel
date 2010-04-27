package org.sakaiproject.nakamura.auth.ldap;

import com.novell.ldap.LDAPConnection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapException;

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
  
  public static final String DEFAULT_LDAP_BASE_DN = "uid={}";
  @Property(value = DEFAULT_LDAP_BASE_DN)
  static final String LDAP_BASE_DN = "sakai.auth.ldap.baseDn";
  private String baseDn;

  @Property(cardinality = Integer.MAX_VALUE)
  static final String REQUIRED_ATTRIBUTES = "sakai.auth.ldap.reqattrs";

  @Reference
  private LdapConnectionManager connMgr;

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
