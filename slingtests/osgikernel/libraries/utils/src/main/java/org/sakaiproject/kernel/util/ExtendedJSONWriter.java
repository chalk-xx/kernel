package org.sakaiproject.kernel.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import java.io.Writer;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class ExtendedJSONWriter extends JSONWriter {

  public ExtendedJSONWriter(Writer w) {
    super(w);
  }

  public void valueMap(ValueMap valueMap) throws JSONException {
    object();
    for (Entry<String, Object> entry : valueMap.entrySet()) {
      key(entry.getKey());
      value(entry.getValue());
    }
    endObject();
  }

  public static void writeNodeToWriter(JSONWriter write, Node node) throws JSONException, RepositoryException {
    write.object();
    PropertyIterator properties = node.getProperties();
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

  private static String stringValue(Value value) throws ValueFormatException,
      IllegalStateException, RepositoryException {
    switch (value.getType()) {
    case PropertyType.STRING:
    case PropertyType.NAME:
      return value.getString();
    default:
      return value.toString();
    }
  }

  public void node(Node node) throws JSONException, RepositoryException {
    writeNodeToWriter(this, node);
  }

}
