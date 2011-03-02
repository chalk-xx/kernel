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
package org.sakaiproject.nakamura.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

public class ExtendedJSONWriter extends JSONWriter {

  public ExtendedJSONWriter(Writer w) {
    super(w);
  }


  public void valueMap(Map<String, Object> valueMap) throws JSONException {
    ExtendedJSONWriter.writeValueMap(this, valueMap);
  }


  public static void writeValueMap(JSONWriter writer, Map<String, ?> valueMap) throws JSONException {
    writer.object();
    writeValueMapInternals(writer, valueMap);
    writer.endObject();
  }

  /**
   * This will output the key value pairs of a value map as JSON without opening and
   * closing braces, you will need to call object() and endObject() yourself but you can
   * use this to allow appending onto the end of the existing data
   *
   * @param valueMap
   *          any ValueMap (cannot be null)
   * @throws JSONException
   *           on failure
   */
  public void valueMapInternals(Map<String, Object> valueMap) throws JSONException {
    ExtendedJSONWriter.writeValueMapInternals(this,valueMap);
  }
  public static void writeValueMapInternals(JSONWriter writer, Map<String, ?> valueMap) throws JSONException {
    if (valueMap != null) {
      for (Entry<String, ?> entry : valueMap.entrySet()) {
        writer.key(entry.getKey());
        writeValueInternal(writer, entry.getValue());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void writeValueInternal(JSONWriter writer, Object entryValue) throws JSONException {
    if (entryValue instanceof Object[]) {
      writer.array();
      Object[] objects = (Object[]) entryValue;
      for (Object object : objects) {
        writeValueInternal(writer, object);
      }
      writer.endArray();
    } else if (entryValue instanceof Collection<?>) {
      Collection<Object> c = (Collection<Object>) entryValue;
      if ( c.size() == 1) {
        writeValueInternal(writer, c.iterator().next());
      } else{
        writer.array();
        for (Object object : c) {
          writeValueInternal(writer, object);
        }
        writer.endArray();
      }
    } else if (entryValue instanceof ValueMap || entryValue instanceof Map<?, ?>) {
      ExtendedJSONWriter.writeValueMap(writer, (Map<String, ?>) entryValue);
    }
    else {
      writer.value(entryValue);
    }
  }

  public static void writeNodeContentsToWriter(JSONWriter write, Node node)
      throws RepositoryException, JSONException {
    // Since removal of bigstore we add in jcr:path and jcr:name
    write.key("jcr:path");
    write.value(PathUtils.translateAuthorizablePath(node.getPath()));
    write.key("jcr:name");
    write.value(node.getName());

    PropertyIterator properties = node.getProperties();
    while (properties.hasNext()) {
      Property prop = properties.nextProperty();
      String name = prop.getName();
      write.key(name);
      PropertyDefinition propertyDefinition = prop.getDefinition();
      int propertyType = prop.getType();
      if ( PropertyType.BINARY == propertyType ) {
        if (propertyDefinition.isMultiple()) {
          write.array();
          for (long l : prop.getLengths()) {
            write.value("binary-length:"+String.valueOf(l));
          }
          write.endArray();
        } else {
          write.value("binary-length:"+String.valueOf(prop.getLength()));
        }
      } else {
        if (propertyDefinition.isMultiple()) {
          Value[] values = prop.getValues();
          write.array();
          for (Value value : values) {
            Object ovalue = stringValue(value);
            if (isUserPath(name, ovalue)) {
              write.value(PathUtils.translateAuthorizablePath(ovalue));
            } else {
              write.value(ovalue);
            }
          }
          write.endArray();
        } else {
          Object value = stringValue(prop.getValue());
          if (isUserPath(name, value)) {
            write.value(PathUtils.translateAuthorizablePath(value));
          } else {
            write.value(value);
          }
        }
      }
    }
  }

  public static void writeNodeContentsToWriter(JSONWriter write, Content content)
      throws JSONException {
    // Since removal of bigstore we add in jcr:path and jcr:name
    write.key("jcr:path");
    write.value(PathUtils.translateAuthorizablePath(content.getPath()));
    write.key("jcr:name");
    write.value(StorageClientUtils.getObjectName(content.getPath()));

    Map<String, Object> props = content.getProperties();
    for (Entry<String, Object> prop : props.entrySet()) {
      String propName = prop.getKey();
      Object propValue = prop.getValue();

      write.key(propName);
      if (propValue instanceof Object[]) {
        write.array();
        for (Object value : (Object[]) propValue) {
          if (isUserPath(propName, value)) {
            write.value(PathUtils.translateAuthorizablePath(value));
          } else {
            write.value(value);
          }
        }
        write.endArray();
      } else if (propValue instanceof java.util.Calendar){
          write.value(DateUtils.iso8601((java.util.Calendar)propValue));
      } else {
        if (isUserPath(propName, propValue)) {
          write.value(PathUtils.translateAuthorizablePath(propValue));
        } else {
          write.value(propValue);
        }
      }
    }
  }

  @Override
  public JSONWriter value(Object object) throws JSONException {
    if ( object instanceof Object[]) {
      Object[] oarray = (Object[]) object;
      if (  oarray.length > 0 ) {
        if ( oarray.length == 1) {
          value(oarray[0]);
        } else {
          array();
          for ( Object o : oarray) {
            value(o);
          }
          endArray();
        }
      }
      return this;
    } else {
      return super.value(object);
    }
  }

  private static boolean isUserPath(String name, Object value) {
    if ("jcr:path".equals(name) || "path".equals(name) || "userProfilePath".equals(name)) {
      String s = String.valueOf(value);
      if (s != null && s.length() > 4) {
        if (s.charAt(0) == '/' && s.charAt(1) == '_') {
          if (s.startsWith("/_user/") || s.startsWith("/_group/") || s.startsWith("a:")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static void writeNodeToWriter(JSONWriter write, Node node) throws JSONException,
      RepositoryException {
    writeNodeTreeToWriter(write, node, false, 0);
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
      return DateUtils.iso8601(value.getDate());
    default:
      return value.toString();
    }
  }

  public void node(Node node) throws JSONException, RepositoryException {
    ExtendedJSONWriter.writeNodeToWriter(this, node);
  }

  /**
   * Represent an entire JCR tree in JSON format. Convenience method for
   * writeNodeTreeToWriter(write, node, false, -1, -1).
   *
   * @param write
   *          The {@link JSONWriter writer} to send the data to.
   * @param node
   *          The node and it's subtree to output. Note: The properties of this node will
   *          be outputted as well.
   * @throws RepositoryException
   * @throws JSONException
   */
  public static void writeNodeTreeToWriter(JSONWriter write, Node node)
      throws RepositoryException, JSONException {
      writeNodeTreeToWriter(write, node, false, -1, -1);
  }

  /**
   * Represent an entire JCR tree in JSON format. Convenience method for
   * writeNodeTreeToWriter(write, node, objectInProgress, -1, -1).
   *
   * @param write
   *          The {@link JSONWriter writer} to send the data to.
   * @param node
   *          The node and it's subtree to output. Note: The properties of this node will
   *          be outputted as well.
   * @param objectInProgress
   *          use true if you don't want the method to enclose the output in fresh object braces
   * @throws RepositoryException
   * @throws JSONException
   */
  public static void writeNodeTreeToWriter(JSONWriter write, Node node, boolean objectInProgress)
      throws RepositoryException, JSONException {
    writeNodeTreeToWriter(write, node, objectInProgress, -1, -1);
  }

  /**
   * Represent an entire JCR tree in JSON format. Convenience method for
   * writeNodeTreeToWriter(write, node, false, maxDepth, 0).
   *
   * @param write
   *          The {@link JSONWriter writer} to send the data to.
   * @param node
   *          The node and it's subtree to output. Note: The properties of this node will
   *          be outputted as well.
   * @param maxDepth
   *          Maximum depth of subnodes to traverse. The properties on {@link node} are
   *          processed before this is taken into account.
   * @throws RepositoryException
   * @throws JSONException
   */
  public static void writeNodeTreeToWriter(JSONWriter write, Node node, int maxDepth)
      throws RepositoryException, JSONException {
    writeNodeTreeToWriter(write, node, false, maxDepth, 0);
  }
  public static void writeContentTreeToWriter(JSONWriter write, Content content, int maxDepth)
      throws JSONException {
    writeNodeTreeToWriter(write, content, false, maxDepth, 0);
  }

  /**
   * Represent an entire JCR tree in JSON format.
   * <p>
   * if maxDepth == 0 and objectInProgress == false, same as calling
   * {@link #writeNodeToWriter(JSONWriter, Node).
   * <p>
   * if maxDepth == 0 and objectInfProgress == true, same as calling
   * {@link #writeNodeContentsToWriter(JSONWriter, Node).
   *
   * @param write
   *          The {@link JSONWriter writer} to send the data to.
   * @param node
   *          The node and it's subtree to output. Note: The properties of this node will
   *          be outputted as well.
   * @param objectInProgress
   *          use true if you don't want the method to enclose the output in fresh object
   *          braces
   * @param maxDepth
   *          Maximum depth of subnodes to traverse. The properties on {@link node} are
   *          processed before this is taken into account.
   * @throws RepositoryException
   * @throws JSONException
   */
  public static void writeNodeTreeToWriter(JSONWriter write, Node node,
      boolean objectInProgress, int maxDepth) throws RepositoryException, JSONException {
    writeNodeTreeToWriter(write, node, objectInProgress, maxDepth, 0);
  }

  public static void writeContentTreeToWriter(JSONWriter write, Content content,
      boolean objectInProgress, int maxDepth) throws JSONException {
    writeNodeTreeToWriter(write, content, objectInProgress, maxDepth, 0);
  }

  /**
   * Represent an entire JCR tree in JSON format.
   *
   * @param write
   *          The {@link JSONWriter writer} to send the data to.
   * @param node
   *          The node and it's subtree to output. Note: The properties of this node will
   *          be outputted as well.
   * @param objectInProgress
   *          use true if you don't want the method to enclose the output in fresh object
   *          braces
   * @param maxDepth
   *          Maximum depth of subnodes to traverse. The properties on {@link node} are
   *          processed before this is taken into account.
   * @param currentLevel
   *          Internal parameter to track the current processing level.
   * @throws RepositoryException
   * @throws JSONException
   */
  protected static void writeNodeTreeToWriter(JSONWriter write, Node node,
      boolean objectInProgress, int maxDepth, int currentLevel)
      throws RepositoryException, JSONException {
    // Write this node's properties.
    if (!objectInProgress) {
      write.object();
    }
    writeNodeContentsToWriter(write, node);

    if (maxDepth == -1 || currentLevel < maxDepth) {
      // Write all the child nodes.
      NodeIterator iterator = node.getNodes();
      while (iterator.hasNext()) {
        Node childNode = iterator.nextNode();
        write.key(childNode.getName());
        writeNodeTreeToWriter(write, childNode, false, maxDepth, currentLevel + 1);
      }
    }

    if (!objectInProgress) {
      write.endObject();
    }
  }

  protected static void writeNodeTreeToWriter(JSONWriter write, Content content,
      boolean objectInProgress, int maxDepth, int currentLevel)
      throws JSONException {
    // Write this node's properties.
    if (!objectInProgress) {
      write.object();
    }
    writeNodeContentsToWriter(write, content);

    if (maxDepth == -1 || currentLevel < maxDepth) {
      // Write all the child nodes.
      for (Content child : content.listChildren()) {
        write.key(child.getPath());
        writeNodeTreeToWriter(write, child, false, maxDepth, currentLevel + 1);
      }
    }

    if (!objectInProgress) {
      write.endObject();
    }
  }


}
