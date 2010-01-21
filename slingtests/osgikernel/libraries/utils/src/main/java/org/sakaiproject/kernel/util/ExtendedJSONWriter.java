package org.sakaiproject.kernel.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class ExtendedJSONWriter extends JSONWriter {

  private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  
  public ExtendedJSONWriter(Writer w) {
    super(w);
  }

  public void valueMap(ValueMap valueMap) throws JSONException {
    object();
    valueMapInternals(valueMap);
    endObject();
  }

  /**
   * This will output the key value pairs of a value map as JSON without opening and
   * closing braces, you will need to call object() and endObject() yourself but you
   * can use this to allow appending onto the end of the existing data
   * @param valueMap any ValueMap (cannot be null)
   * @throws JSONException on failure
   */
  public void valueMapInternals(ValueMap valueMap) throws JSONException {
    for (Entry<String, Object> entry : valueMap.entrySet()) {
      key(entry.getKey());
      Object entryValue = entry.getValue();
      if (entryValue instanceof Object[]) {
        array();
        Object[] objects = (Object[])entryValue;
        for (Object object : objects) {
          value(object);
        }
        endArray();
      }
      else {
        value(entry.getValue());
      }
    }
  }

  public static void writeNodeContentsToWriter(JSONWriter write, Node node) throws RepositoryException, JSONException {
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
  }
  
  public static void writeNodeToWriter(JSONWriter write, Node node) throws JSONException, RepositoryException {
    write.object();
    writeNodeContentsToWriter(write, node);
    write.endObject();    
  }

  private static Object stringValue(Value value) throws ValueFormatException,
      IllegalStateException, RepositoryException {
    switch (value.getType()) {
    case PropertyType.STRING:
    case PropertyType.NAME:
    case PropertyType.REFERENCE:
    case PropertyType.PATH:
      return value.getString();
    case PropertyType.BOOLEAN:
      return value.getBoolean();
    case PropertyType.LONG:
      return value.getLong();
    case PropertyType.DOUBLE:
      return value.getDouble();     
    case PropertyType.DATE:
      return format.format(value.getDate().getTime());
    default:
      return value.toString();
    }
  }

  public void node(Node node) throws JSONException, RepositoryException {
    writeNodeToWriter(this, node);
  }

}
