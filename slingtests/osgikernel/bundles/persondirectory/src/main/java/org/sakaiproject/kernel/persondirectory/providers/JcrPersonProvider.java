package org.sakaiproject.kernel.persondirectory.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProvider;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;
import org.sakaiproject.kernel.persondirectory.PersonImpl;
import org.sakaiproject.kernel.util.JcrUtils;
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

  public Person getPerson(String uid, Node authProfileNode)
      throws PersonProviderException {
    PersonImpl jcrPerson = new PersonImpl(uid);
    try {
      PropertyIterator props = authProfileNode.getProperties();

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

    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return jcrPerson;
  }
}
