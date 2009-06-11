package org.sakaiproject.kernel.search;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryResult;

/**
 * Formats user profile node search results
 * 
 * @scr.component immediate="true" label="NodeSearchResultProcessor"
 *                description="Formatter for user search results"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Node"
 * @scr.service 
 *              interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class NodeSearchResultProcessor implements SearchResultProcessor {

  public void output(JSONWriter write, QueryResult result, int nitems) throws RepositoryException,
      JSONException {
    NodeIterator resultNodes = result.getNodes();
    while (resultNodes.hasNext()) {
      Node resultNode = resultNodes.nextNode();
      write.object();
      PropertyIterator properties = resultNode.getProperties();
      while (properties.hasNext()) {
        Property prop = properties.nextProperty();
        write.key(prop.getName());
        if (prop.getDefinition().isMultiple()) {
          Value[] values = prop.getValues();
          write.array();
          for (Value value : values) {
            write.value(stringValue(value));
          }
          write.endArray();
        } else {
          write.value(stringValue(prop.getValue()));
        }
      }
      write.endObject();
    }
  }

  private String stringValue(Value value) throws ValueFormatException, IllegalStateException, RepositoryException {
    switch (value.getType())
    {
      case PropertyType.STRING:
      case PropertyType.NAME:
        return value.getString();
      default:
        return value.toString();
    }
  }

}
