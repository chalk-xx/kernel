/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.auth.ldap;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.jackrabbit.server.security.AuthenticationPlugin;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.ldap.LdapUtil;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

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

  public static final boolean CREATE_ACCOUNT_DEFAULT = true;
  @Property(boolValue = CREATE_ACCOUNT_DEFAULT)
  static final String CREATE_ACCOUNT = "sakai.auth.ldap.account.create";
  private boolean createAccount;

  @Property(cardinality = 2147483647)
  static final String USER_PROPS = "sakai.auth.ldap.user.props";
  private HashMap<String, String> attrsProps;

  @Reference
  private LdapConnectionManager connMgr;

  @Reference
  private LiteAuthorizablePostProcessService authorizablePostProcessService;

  @Reference
  private Repository repository;

  public LdapAuthenticationPlugin() {
  }

  LdapAuthenticationPlugin(LdapConnectionManager connMgr,
		  LiteAuthorizablePostProcessService authorizablePostProcessService,
      Repository repository) {
    this.connMgr = connMgr;
    this.authorizablePostProcessService = authorizablePostProcessService;
    this.repository = repository;
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
    baseDn = PropertiesUtil.toString(props.get(LDAP_BASE_DN), "");
    userFilter = PropertiesUtil.toString(props.get(USER_FILTER), "");
    authzFilter = PropertiesUtil.toString(props.get(AUTHZ_FILTER), "");
    createAccount = PropertiesUtil.toBoolean(props.get(CREATE_ACCOUNT), CREATE_ACCOUNT_DEFAULT);

    parseUserProps(props);
  }

  protected boolean canDecorateUser() {
    return attrsProps != null;
  }

  private void parseUserProps(Map<?, ?> props) {
    // check for the existence of USER_PROPS
    if (props.containsKey(USER_PROPS)) {
      Object oProps = props.get(USER_PROPS);
      if (oProps == null) {
        attrsProps = null;
      }
      // check the String to be JSON
      else if (oProps instanceof String && ((String) oProps).length() > 0
          && ((String) oProps).charAt(0) == '{') {
        try {
          String strProps = (String) oProps;
          JSONObject jsonObj = new JSONObject(strProps);
          Iterator<String> keysIter = jsonObj.keys();
          attrsProps = new HashMap<String, String>();
          while (keysIter.hasNext()) {
            String key = keysIter.next();
            attrsProps.put(key, jsonObj.getString(key));
          }
        } catch (JSONException e) {
          log.error(e.getMessage(), e);
          attrsProps = null;
        }
      }
      // String[] should processed as "key":"value" pairs per index.
      else if (oProps instanceof String[]) {
        String[] userProps = (String[]) oProps;

        if (userProps.length > 0 && !(userProps.length == 1 && userProps[0] == null)) {
          attrsProps = new HashMap<String, String>();
          for (int i = 0; i < userProps.length; i++) {
            String[] ldapJcr = StringUtils.split(userProps[i], "\":\"");
            attrsProps.put(ldapJcr[0], ldapJcr[1]);
          }
        }
      }
    }
    // process entries as sakai.auth.ldap.user.props.ldapAttrName = jcrPropName
    else {
      attrsProps = new HashMap<String, String>();

      for (Entry<?, ?> entry : props.entrySet()) {
        String key = PropertiesUtil.toString(entry.getKey(), "");
        String value = PropertiesUtil.toString(entry.getValue(), "");
        if (key.length() > 0 && value.length() > 0 && key.startsWith(USER_PROPS)) {
          String ldapAttrName = key.substring(USER_PROPS.length());
          attrsProps.put(ldapAttrName, value);
        }
      }
    }

    if (attrsProps != null && attrsProps.size() == 0) {
      attrsProps = null;
    }
  }

  // ---------- AuthenticationPlugin ----------
  public boolean authenticate(Credentials credentials) {
    boolean auth = false;
    if (credentials instanceof SimpleCredentials) {
      // get application user credentials
      String appUser = connMgr.getConfig().getLdapUser();
      String appPass = connMgr.getConfig().getLdapPassword();

      // get user credentials
      SimpleCredentials sc = (SimpleCredentials) credentials;

      long timeStart = System.currentTimeMillis();

      String userDn = LdapUtil.escapeLDAPSearchFilter(userFilter.replace("{}",
          sc.getUserID()));
      String userPass = new String(sc.getPassword());

      LDAPConnection conn = null;
      Exception ldapTrouble = null;
      boolean didLdapSucceed = false;
      for (int i = 0; i < 4; i++) {
        try {
          if (i > 0) {
            log.debug("LDAP error on ldap auth. Retrying. " + ldapTrouble.getMessage());
          }
          // 0) Get a connection to the server
          conn = connMgr.getConnection();
          log.debug("Connected to LDAP server");
          bindAppUser(appUser, appPass, conn);
          // 2) Search for username (not authz).
          // If search fails, log/report invalid username or password.
          LDAPSearchResults results = conn.search(baseDn, LDAPConnection.SCOPE_SUB,
              userDn, null, true);
          if (results.hasMore()) {
            log.debug("Found user via search");
          } else {
            throw new IllegalArgumentException("Can't find user [" + userDn + "]");
          }

          // 3) Bind as user.
          // If bind fails, log/report invalid username or password.

          // value is set below. define here for use in authz check.
          String userEntryDn = null;
          // KERN-776 Resolve the user DN from the search results and check for an aliased
          // entry
          LDAPEntry userEntry = results.next();
          LDAPAttribute objectClass = userEntry.getAttribute("objectClass");

          if ("aliasObject".equals(objectClass.getStringValue())) {
            LDAPAttribute aliasDN = userEntry.getAttribute("aliasedObjectName");
            userEntryDn = aliasDN.getStringValue();
          } else {
            userEntryDn = userEntry.getDN();
          }

          conn.bind(LDAPConnection.LDAP_V3, userEntryDn, userPass.getBytes(UTF8));
          log.debug("Bound as user");

          if (authzFilter.length() > 0) {
            // 4) Return to app user
            conn.bind(LDAPConnection.LDAP_V3, appUser, appPass.getBytes(UTF8));
            log.debug("Rebound as application user");
            // 5) Search user DN with authz filter
            // If search fails, log/report that user is not authorized
            String userAuthzFilter = "(&(" + userEntryDn + ")(" + authzFilter + "))";
            results = conn.search(baseDn, LDAPConnection.SCOPE_SUB, userAuthzFilter,
                null, true);
            if (results.hasMore()) {
              log.debug("Found user + authz filter via search");
            } else {
              throw new IllegalArgumentException("User not authorized [" + userDn + "]");
            }
          }

          // FINALLY!
          auth = true;
          log.info("User [{}] authenticated with LDAP in {}ms", userDn,
              System.currentTimeMillis() - timeStart);

          // provision & decorate the user
          Session session = repository.loginAdministrative();
          Authorizable authorizable = getJcrUser(session, sc.getUserID());

          if (authorizable != null && attrsProps != null) {
            log.debug("Decorating user [{}] with props from {}", userDn, USER_PROPS);
            decorateUser(session, authorizable, conn);
          }
          // if we made it this far, we can exit the retry loop
          didLdapSucceed = true;
          break;
        } catch (Exception e) {
          ldapTrouble = e;
          log.warn(e.getMessage(), e);
        } finally {
          log.debug("Returning LDAP connection to pool.");
          connMgr.returnConnection(conn);
        }
      }
      
      if (!didLdapSucceed) {
        log.error("Could not negotiate with LDAP even after retrying. Giving up. {}:{}", 
            ldapTrouble.getClass().getName(), ldapTrouble.getLocalizedMessage());
        return false;
      }
    }
    return auth;
  }

  private void bindAppUser(String appUser, String appPass, LDAPConnection conn)
      throws LDAPException, UnsupportedEncodingException {
    conn.bind(LDAPConnection.LDAP_V3, appUser, appPass.getBytes(UTF8));
    log.debug("Bound as application user");
  }

  private Authorizable getJcrUser(Session session, String userId) throws Exception {
	AuthorizableManager am = session.getAuthorizableManager(); 
    Authorizable auth = am.findAuthorizable(userId);
    if (auth == null && createAccount) {
      String password = RandomStringUtils.random(8);
      boolean created = am.createUser(userId, userId, password, null);
      if (created) {
    	  auth = am.findAuthorizable(userId);
    	  authorizablePostProcessService.process(auth, session, ModificationType.CREATE, null);
      }
      else {
    	  throw new Exception("Unable to create User for " + userId);
      }
    }
    return auth;
  }

  /**
   * Decorate the user with extra information.
   * 
   * @param session
   * @param user
 * @throws StorageClientException 
 * @throws AccessDeniedException 
   */
  private void decorateUser(Session session, Authorizable user, LDAPConnection conn)
      throws LDAPException, AccessDeniedException, StorageClientException {
    // fix up the user dn to search
    String userDn = LdapUtil
        .escapeLDAPSearchFilter(userFilter.replace("{}", user.getId()));

    // get a connection to LDAP
    String[] ldapAttrNames = attrsProps.keySet().toArray(new String[attrsProps.size()]);
    LDAPSearchResults results = conn.search(baseDn, LDAPConnection.SCOPE_SUB, userDn,
        ldapAttrNames, false);
    if (results.hasMore()) {
      LDAPEntry entry = results.next();

      for (String ldapAttrName : ldapAttrNames) {
        String jcrPropName = attrsProps.get(ldapAttrName);

        LDAPAttribute attr = entry.getAttribute(ldapAttrName);
        if (attr != null) {
          user.setProperty(jcrPropName, attr.getStringValue());
        }
      }
      session.getAuthorizableManager().updateAuthorizable(user);
    } else {
      log.warn("Can't find user [" + userDn + "]");
    }
  }
}
