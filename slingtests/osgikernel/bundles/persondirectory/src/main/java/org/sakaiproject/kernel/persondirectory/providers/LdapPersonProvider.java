package org.sakaiproject.kernel.persondirectory.providers;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.kernel.api.ldap.LdapConnectionBroker;
import org.sakaiproject.kernel.api.ldap.LdapException;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;
import org.sakaiproject.kernel.persondirectory.PersonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.jcr.Node;

/**
 * Person provider implementation that gets its information from an LDAP store.
 *
 * @author Carl Hall
 */
@Component
@Service
public class LdapPersonProvider implements PersonProvider {
  private static final Logger LOG = LoggerFactory.getLogger(LdapPersonProvider.class);
  private static final String LDAP_NAME = "LdapUserProvider";

  private static final String UID_ATTR = "uid";
  private static final String SEARCH_BASE_DN = ",ou=Local Accounts,dc=gted,dc=gatech,dc=edu";

  /** Default LDAP access timeout in milliseconds */
  public static final int DEFAULT_OPERATION_TIMEOUT_MILLIS = 5000;

  /** Default referral following behavior */
  public static final boolean DEFAULT_IS_FOLLOW_REFERRALS = false;

  @Reference
  private LdapConnectionBroker ldapBroker;

  /**
   * Default constructor.
   */
  public LdapPersonProvider() {
  }

  /**
   * Constructor for injecting dependencies. Targetted for tests.
   *
   * @param ldapBroker
   */
  protected LdapPersonProvider(LdapConnectionBroker ldapBroker) {
    this.ldapBroker = ldapBroker;
  }

  @SuppressWarnings("unchecked")
  public Person getPerson(String uid, Node profileNode) throws PersonProviderException {
    try {
      String filter = "gtuserid=" + uid;
      LDAPConnection conn = ldapBroker.getConnection(LDAP_NAME);

      LDAPSearchConstraints constraints = new LDAPSearchConstraints();
      constraints.setDereference(LDAPSearchConstraints.DEREF_ALWAYS);
      constraints.setTimeLimit(DEFAULT_OPERATION_TIMEOUT_MILLIS);
      constraints.setReferralFollowing(DEFAULT_IS_FOLLOW_REFERRALS);
      constraints.setBatchSize(0);

      String[] attributes = null;
      LOG.debug("searchDirectory(): [baseDN = {}][filter = {}][return attribs = {}]", new Object[] {
          UID_ATTR + "=" + uid + SEARCH_BASE_DN, filter, attributes });

      PersonImpl ldapPerson = null;
      LDAPSearchResults searchResults = conn.search(SEARCH_BASE_DN, LDAPConnection.SCOPE_SUB,
          filter, attributes, false, constraints);
      if (searchResults.hasMore()) {
        ldapPerson = new PersonImpl(uid);
        LDAPEntry entry = searchResults.next();
        LDAPAttributeSet attrs = entry.getAttributeSet();
        Iterator attrIter = attrs.iterator();
        while (attrIter.hasNext()) {
          LDAPAttribute attr = (LDAPAttribute) attrIter.next();
          ldapPerson.addAttribute(attr.getName(), attr.getStringValueArray());
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
