package org.sakaiproject.kernel.persondirectory.providers;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.kernel.api.ldap.LdapConnectionBroker;
import org.sakaiproject.kernel.api.ldap.LdapException;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;
import org.sakaiproject.kernel.persondirectory.PersonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;

/**
 * Person provider implementation that gets its information from an LDAP store.
 *
 * @author Carl Hall
 */
@Component(metatype = true, enabled = false)
@Service
public class LdapPersonProvider implements PersonProvider {
  private static final Logger LOG = LoggerFactory.getLogger(LdapPersonProvider.class);
  private static final String LDAP_BROKER_NAME = "LdapUserProvider";

  /** Default LDAP access timeout in milliseconds */
  public static final int DEFAULT_OPERATION_TIMEOUT_MILLIS = 5000;

  /** Default referral following behavior */
  public static final boolean DEFAULT_IS_FOLLOW_REFERRALS = false;

  @Property(value = "usr")
  protected static final String PROP_USER = "ldap.provider.user";

  @Property(value = "passwd")
  protected static final String PROP_PASSWORD = "ldap.provider.password";

  @Property(value = "uid={},ou=Local Accounts,dc=sakai")
  protected static final String PROP_BASE_DN_PATTERN = "ldap.provider.baseDn.pattern";

  @Property(value = "uid={}")
  protected static final String PROP_FILTER_PATTERN = "ldap.provider.filter.pattern";

  @Property(value = { "key1=>key1", "key2*=>key2" })
  protected static final String PROP_ATTRIBUTES = "ldap.provider.attributes";

  @Reference
  private LdapConnectionBroker ldapBroker;

  private String user;
  private String password;
  private String baseDnPattern;
  private String filterPattern;
  private HashMap<String, String> attributesMap = new HashMap<String, String>();
  private String[] attributes;

  /**
   * Default constructor.
   */
  public LdapPersonProvider() {
  }

  /**
   * Constructor for injecting dependencies. Targeted for tests.
   *
   * @param ldapBroker
   */
  protected LdapPersonProvider(LdapConnectionBroker ldapBroker) {
    this.ldapBroker = ldapBroker;
  }

  @Activate
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext ctx) {
    Dictionary props = ctx.getProperties();
    user = (String) props.get(PROP_USER);
    password = (String) props.get(PROP_PASSWORD);
    baseDnPattern = (String) props.get(PROP_BASE_DN_PATTERN);
    filterPattern = (String) props.get(PROP_FILTER_PATTERN);
    String[] attributeMapping = (String[]) props.get(PROP_ATTRIBUTES);
    if (attributeMapping != null) {
      for (String mapping : attributeMapping) {
        int splitIndex = mapping.indexOf("=>");
        if (splitIndex < 1) {
          // make sure the splitter is found after at least 1 character.
          throw new ComponentException("Improperly formatted key mapping [" + mapping
              + "]. Should be fromKey=>toKey.");
        }
        String key0 = mapping.substring(0, splitIndex).trim();
        String key1 = mapping.substring(splitIndex + 2).trim();
        if (key0.length() == 0 || key1.length() == 0) {
          // make sure we have 2 usable keys
          throw new ComponentException("Improperly formatted key mapping [" + mapping
              + "]. Should be fromKey=>toKey.");
        }
        attributesMap.put(key0, key1);
      }
    }

    // create an attribute array for looking things up
    Set<String> attrKeys = attributesMap.keySet();
    String[] attrs = new String[attrKeys.size()];
    attrKeys.toArray(attrs);
  }

  protected Map<String, String> getAttributesMap() {
    return attributesMap;
  }

  @SuppressWarnings("unchecked")
  public Person getPerson(String uid, Node profileNode) throws PersonProviderException {
    try {
      // set the constraints
      LDAPSearchConstraints constraints = new LDAPSearchConstraints();
      constraints.setDereference(LDAPSearchConstraints.DEREF_ALWAYS);
      constraints.setTimeLimit(DEFAULT_OPERATION_TIMEOUT_MILLIS);
      constraints.setReferralFollowing(DEFAULT_IS_FOLLOW_REFERRALS);
      constraints.setBatchSize(0);

      // set the properties
      String baseDn = baseDnPattern.replace("{}", uid);
      String filter = filterPattern.replace("{}", uid);

      LOG.debug("searchDirectory(): [baseDN = {}][filter = {}][return attribs = {}]", new Object[] {
          baseDn, filter, attributes });

      PersonImpl ldapPerson = null;
      // get a connection
      LDAPConnection conn = ldapBroker.getBoundConnection(LDAP_BROKER_NAME, user, password);
      LDAPSearchResults searchResults = conn.search(baseDn, LDAPConnection.SCOPE_SUB, filter,
          attributes, false, constraints);
      if (searchResults.hasMore()) {
        // create the person to populate
        ldapPerson = new PersonImpl(uid);

        // pick off the first result returned
        LDAPEntry entry = searchResults.next();

        // get the attributes from the entry and loop through them
        LDAPAttributeSet attrs = entry.getAttributeSet();
        Iterator attrIter = attrs.iterator();
        while (attrIter.hasNext()) {
          // get the key and values from the attribute
          LDAPAttribute attr = (LDAPAttribute) attrIter.next();
          String name = attr.getName();
          String[] vals = attr.getStringValueArray();

          // check for an aliased name
          String mappingName = name;
          if (attributesMap.containsKey(name)) {
            mappingName = attributesMap.get(name);
          }

          // add the values under the appropriate key
          ldapPerson.addAttribute(mappingName, vals);
        }
      }

      return ldapPerson;
    } catch (LdapException e) {
      throw new PersonProviderException(e.getMessage(), e);
    } catch (LDAPException e) {
      throw new PersonProviderException(e.getMessage(), e);
    }
  }
}
