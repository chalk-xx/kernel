package org.sakaiproject.nakamura.ldap;

import com.novell.ldap.LDAPConnection;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionLivenessValidator;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component(metatype = true)
@Service(serviceFactory = true)
public class LdapConnectionManagerFactory implements ServiceFactory, ManagedServiceFactory {

  private Map<Long, PoolingLdapConnectionManager> managers;

  private LdapConnectionManagerConfig defaultConfig;

  static final boolean DEFAULT_AUTO_BIND = false;
  @Property(boolValue = DEFAULT_AUTO_BIND)
  static final String AUTO_BIND = "sakai.ldap.autobind";

  static final boolean DEFAULT_FOLLOW_REFERRALS = false;
  @Property(boolValue = DEFAULT_FOLLOW_REFERRALS)
  static final String FOLLOW_REFERRALS = "sakai.ldap.referrals.follow";

  static final String DEFAULT_KEYSTORE_LOCATION = "";
  @Property(value = DEFAULT_KEYSTORE_LOCATION)
  static final String KEYSTORE_LOCATION = "sakai.ldap.keystore.location";

  static final String DEFAULT_KEYSTORE_PASSWORD = "";
  @Property(value = DEFAULT_KEYSTORE_PASSWORD)
  static final String KEYSTORE_PASSWORD = "sakai.ldap.keystore.password";

  static final String DEFAULT_HOST = "localhost";
  @Property(value = DEFAULT_HOST)
  static final String HOST = "sakai.ldap.host";

  static final int DEFAULT_PORT = LDAPConnection.DEFAULT_PORT;
  @Property(intValue = DEFAULT_PORT)
  static final String PORT = "sakai.ldap.port";

  static final String DEFAULT_USER = "";
  @Property(value = DEFAULT_USER)
  static final String USER = "sakai.ldap.user";

  static final String DEFAULT_PASSWORD = "";
  @Property(value = DEFAULT_PASSWORD)
  static final String PASSWORD = "sakai.ldap.password";

  static final boolean DEFAULT_SECURE_CONNECTION = false;
  @Property(boolValue = DEFAULT_SECURE_CONNECTION)
  static final String SECURE_CONNECTION = "sakai.ldap.connection.secure";

  static final int DEFAULT_OPERATION_TIMEOUT = 5000;
  @Property(intValue = DEFAULT_OPERATION_TIMEOUT)
  static final String OPERATION_TIMEOUT = "sakai.ldap.operation.timeout";

  static final boolean DEFAULT_POOLING = true;
  @Property(boolValue = DEFAULT_POOLING)
  static final String POOLING = "sakai.ldap.pooling";

  static final int DEFAULT_POOLING_MAX_CONNS = 10;
  @Property(intValue = DEFAULT_POOLING_MAX_CONNS)
  static final String POOLING_MAX_CONNS = "sakai.ldap.pooling.maxConns";

  static final boolean DEFAULT_TLS = false;
  @Property(boolValue = DEFAULT_TLS)
  static final String TLS = "sakai.ldap.tls";

  @Reference(referenceInterface = LdapConnectionLivenessValidator.class,
      cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
      policy = ReferencePolicy.DYNAMIC, bind = "bindLivenessValidator",
      unbind = "unbindLivenessValidator")
  private List<LdapConnectionLivenessValidator> livenessValidators = new LinkedList<LdapConnectionLivenessValidator>();

  protected void bindLivenessValidator(LdapConnectionLivenessValidator validator) {
    livenessValidators.add(validator);
  }

  protected void unbindLivenessValidator(LdapConnectionLivenessValidator validator) {
    livenessValidators.remove(validator);
  }

  // ---------- ServiceFactory
  public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
    PoolingLdapConnectionManager mgr = null;

    if (managers.containsKey(bundle.getBundleId())) {
      mgr = managers.get(bundle.getBundleId());
    } else {
      mgr = new PoolingLdapConnectionManager(defaultConfig);

      try {
        mgr.init();
      } catch (LdapException e) {
        throw new RuntimeException(e.getMessage(), e);
      }

      managers.put(bundle.getBundleId(), mgr);
    }
    return mgr;
  }

  public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration,
                           Object o) {

  }

  // ---------- SCR integration
  @Activate
  public void activate(ComponentContext context) {

  }

  @Modified
  public void modified(ComponentContext context) {
    
  }
  
  // ---------- ManagedServiceFactory
  public String getName() {
    return getClass().toString();
  }

  public void updated(String pid, Dictionary dictionary) throws ConfigurationException {

  }

  public void deleted(String pid) {
  }

  // ---------- Property update support
  public void update(Dictionary props) {
    if (props != null && !props.isEmpty()) {
      LdapConnectionManagerConfig config = new LdapConnectionManagerConfig();

      config.setAutoBind(OsgiUtil.toBoolean(props.get(AUTO_BIND), DEFAULT_AUTO_BIND));
      config.setFollowReferrals(OsgiUtil.toBoolean(props.get(FOLLOW_REFERRALS), DEFAULT_FOLLOW_REFERRALS));
      config.setKeystoreLocation(OsgiUtil.toString(props.get(KEYSTORE_LOCATION), DEFAULT_KEYSTORE_LOCATION));
      config.setKeystorePassword(OsgiUtil.toString(props.get(KEYSTORE_PASSWORD), DEFAULT_KEYSTORE_PASSWORD));
      config.setSecureConnection(OsgiUtil.toBoolean(props.get(SECURE_CONNECTION), DEFAULT_SECURE_CONNECTION));
      config.setLdapHost(OsgiUtil.toString(props.get(HOST), DEFAULT_HOST));
      config.setLdapPort(OsgiUtil.toInteger(props.get(PORT), DEFAULT_PORT));
      config.setLdapUser(OsgiUtil.toString(props.get(USER), DEFAULT_USER));
      config.setLdapPassword(OsgiUtil.toString(props.get(PASSWORD), DEFAULT_PASSWORD));
      config.setOperationTimeout(OsgiUtil.toInteger(props.get(OPERATION_TIMEOUT), DEFAULT_OPERATION_TIMEOUT));
      config.setPooling(OsgiUtil.toBoolean(props.get(POOLING), DEFAULT_POOLING));
      config.setPoolMaxConns(OsgiUtil.toInteger(props.get(POOLING_MAX_CONNS), DEFAULT_POOLING_MAX_CONNS));
      config.setTLS(OsgiUtil.toBoolean(props.get(TLS), DEFAULT_TLS));

      // set the default configuration
      defaultConfig = config;
    }
  }
}
