package org.sakaiproject.nakamura.persondirectory.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.personal.PersonalConstants;
import org.sakaiproject.nakamura.api.persondirectory.Person;
import org.sakaiproject.nakamura.api.persondirectory.PersonProvider;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;
import org.sakaiproject.nakamura.persondirectory.PersonImpl;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

@Component
@Service
public class JcrPersonProvider implements PersonProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(JcrPersonProvider.class);
  protected static final String SLING_RESOURCE_TYPE = "sling:resourceType";
  protected static final String SAKAI_USER_PROFILE = "sakai/user-profile";

  public Person getPerson(String uid, Node personNode) throws PersonProviderException {
    PersonImpl jcrPerson = new PersonImpl(uid);
    try {
      Node authProfileNode = null;
      if (personNode.hasProperty(SLING_RESOURCE_TYPE)) {
        Property resourceTypeProp = personNode.getProperty(SLING_RESOURCE_TYPE);
        if (SAKAI_USER_PROFILE.equals(resourceTypeProp.getString())) {
          authProfileNode = personNode;
        }
      }
      if (authProfileNode == null) {
        authProfileNode = personNode.getNode(PersonalConstants.AUTH_PROFILE);
      }
      if (authProfileNode != null) {
        PropertyIterator props = authProfileNode.getProperties();

        if (props != null) {
          while (props.hasNext()) {
            Property property = props.nextProperty();
            String name = property.getName();
            Value[] values = JcrUtils.getValues(authProfileNode, name);
            String[] attrValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
              attrValues[i] = values[i].getString();
            }
            jcrPerson.addAttribute(name, attrValues);
          }
        }
      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return jcrPerson;
  }
}
