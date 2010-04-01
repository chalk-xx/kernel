package org.sakaiproject.nakamura.persondirectory.providers;

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
import org.sakaiproject.nakamura.api.ldap.LdapConnectionBroker;
import org.sakaiproject.nakamura.api.ldap.LdapException;
import org.sakaiproject.nakamura.api.persondirectory.Person;
import org.sakaiproject.nakamura.api.persondirectory.PersonProvider;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;
import org.sakaiproject.nakamura.persondirectory.PersonImpl;
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

  @Property(value = "ou=accounts,dc=sakai")
  protected static final String PROP_BASE_DN = "ldap.provider.baseDn.pattern";

  @Property(value = "uid={}")
  protected static final String PROP_FILTER_PATTERN = "ldap.provider.filter.pattern";

  // @Property(cardinality = Integer.MAX_VALUE)
  @Property(value = { "attr1", "attr2" })
  protected static final String PROP_ATTRIBUTES = "ldap.provider.attributes";

  // @Property(cardinality = Integer.MAX_VALUE)
  @Property(value = { "oldkey1 => newkey1", "oldkey2 => newkey2" })
  protected static final String PROP_ATTRIBUTES_MAP = "ldap.provider.attributes.map";

  @Property(boolValue = false)
  protected static final String PROP_ALLOW_ADMIN_LOOKUP = "ldap.provider.admin.lookup";

  @Reference
  private LdapConnectionBroker ldapBroker;

  private boolean allowAdminLookup;
  private String baseDn;
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
    allowAdminLookup = (Boolean) props.get(PROP_ALLOW_ADMIN_LOOKUP);
    baseDn = (String) props.get(PROP_BASE_DN);
    filterPattern = (String) props.get(PROP_FILTER_PATTERN);
    // make sure the area isn't length 1 and empty. Felix admin will send this.
    attributes = (String[]) props.get(PROP_ATTRIBUTES);
    if (attributes != null && attributes.length == 1 && "".equals(attributes[0])) {
      attributes = null;
    }
    String[] attributeMapping = (String[]) props.get(PROP_ATTRIBUTES_MAP);
    if (attributeMapping != null
        && !(attributeMapping.length == 1 && "".equals(attributeMapping[0]))) {
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
    } else {
      attributesMap = new HashMap<String, String>();
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
      PersonImpl ldapPerson = null;
      if (allowAdminLookup || (!allowAdminLookup && !"admin".equals(uid))) {
        // set the constraints
        LDAPSearchConstraints constraints = new LDAPSearchConstraints();
        constraints.setDereference(LDAPSearchConstraints.DEREF_ALWAYS);
        constraints.setTimeLimit(DEFAULT_OPERATION_TIMEOUT_MILLIS);
        constraints.setReferralFollowing(DEFAULT_IS_FOLLOW_REFERRALS);
        constraints.setBatchSize(0);

        // set the properties
        String filter = filterPattern.replace("{}", uid);

        LOG.debug("searchDirectory(): [baseDN = {}][filter = {}][return attribs = {}]",
            new Object[] { baseDn, filter, attributes });

        // get a connection
        LDAPConnection conn = ldapBroker.getConnection(LDAP_BROKER_NAME);
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
      }

      return ldapPerson;
    } catch (LdapException e) {
      throw new PersonProviderException(e.getMessage(), e);
    } catch (LDAPException e) {
      throw new PersonProviderException(e.getMessage(), e);
    }
  }
}
