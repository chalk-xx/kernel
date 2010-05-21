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
package org.sakaiproject.nakamura.sitetemplate;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 *
 */
public class TemplateBuilder {

  private static final String RT_GROUPS = "sakai/template-group";
  private static final String GROUPS_PROPERTY_PRINCIPAL_NAME = "sakai:principalName";
  private static final Object RT_ACE = "sakai/template-ace";

  private Node templateNode;
  private JSONObject json;
  private ResourceResolver resolver;
  private Map<Principal, Map<String, Object>> groups;
  private Map<String, Object> siteMap;
  private int[] loopIndexes = {};
  private int nestedLevel = 0;

  public TemplateBuilder(Node templateNode, JSONObject json, ResourceResolver resolver)
      throws RepositoryException {
    this.templateNode = templateNode;
    this.json = json;
    this.resolver = resolver;

    // Populate the groups map.
    readGroups();

    // Run over the site template.
    readSiteTemplate();
  }

  /**
   * @return The groups that should be created by matching the template and the provided
   *         JSON input.
   */
  public Map<Principal, Map<String, Object>> getGroups() {
    return this.groups;
  }

  /**
   * @return The site node structure represented as Map of Maps. A property value is
   *         either Value or Value[].
   */
  public Map<String, Object> getSiteMap() {
    return this.siteMap;
  }

  /**
   * Reads the groups
   * 
   * @throws RepositoryException
   */
  private void readGroups() throws RepositoryException {
    groups = new HashMap<Principal, Map<String, Object>>();
    Session session = templateNode.getSession();

    // Get the group nodes.
    StringBuilder query = new StringBuilder();
    query.append("/jcr:root").append(templateNode.getPath());
    query.append("//*[@sling:resourceType='").append(RT_GROUPS).append("']");
    Iterator<Resource> resources = resolver.findResources(query.toString(), "xpath");

    List<String> propertiesToIgnore = new ArrayList<String>();
    propertiesToIgnore.add("sakai:template-group");
    propertiesToIgnore.add("sakai:template-groups");
    propertiesToIgnore.add("jcr:primaryType");
    propertiesToIgnore.add("jcr:createdBy");
    propertiesToIgnore.add("jcr:created");
    propertiesToIgnore.add("jcr:lastModifedBy");
    propertiesToIgnore.add("jcr:lastModifed");

    // Loop over the groups and add them to the map.
    while (resources.hasNext()) {
      Map<String, Object> properties = new HashMap<String, Object>();
      Node node = resources.next().adaptTo(Node.class);
      Value principalName = node.getProperty(GROUPS_PROPERTY_PRINCIPAL_NAME).getValue();
      if (isPlaceHolder(principalName.getString())) {
        principalName = getValue(principalName, session);
      }
      properties.put(GROUPS_PROPERTY_PRINCIPAL_NAME, principalName);

      // Loop over all the properties of this group.
      // Each property needs to be added on the group authorizable eventually, we put this
      // in the hashmap.
      addProperties(node, properties, propertiesToIgnore);

      // Add this group and its properties to the map.
      final String groupName = principalName.getString();
      Principal principal = new Principal() {

        public String getName() {
          return groupName;
        }

      };
      groups.put(principal, properties);
    }
  }

  /**
   * @throws RepositoryException
   * 
   */
  private void readSiteTemplate() throws RepositoryException {
    Node siteNode = templateNode.getNode("site");
    siteMap = new HashMap<String, Object>();

    handleNode(siteNode, siteMap);

  }

  /**
   * Handles a node and by recursion it's child nodes.
   * 
   * @param node
   *          The node to check.
   * @param map
   *          The map to add the property values and child nodes.
   * @throws RepositoryException
   */
  private void handleNode(Node node, Map<String, Object> map) throws RepositoryException {
    // Check if this node is a special node.
    // ie: an ACE node.
    // This can be checked by looking at the sling:resourceType
    if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      if (node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString().equals(RT_ACE)) {
        handleACENode(node, map);
      }
    }

    // Handle the properties for this node.
    List<String> propertiesToIgnore = new ArrayList<String>();
    propertiesToIgnore.add("jcr:primaryType");
    propertiesToIgnore.add("jcr:createdBy");
    propertiesToIgnore.add("jcr:created");
    propertiesToIgnore.add("jcr:lastModifedBy");
    propertiesToIgnore.add("jcr:lastModifed");

    addProperties(node, map, propertiesToIgnore);
    handleChildNodes(node, map);
  }

  /**
   * 
   * @param node
   * @param map
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void handleACENode(Node node, Map<String, Object> map)
      throws RepositoryException {
    // The name of the principial we should set an ACE for.
    Value principalName = node.getProperty("sakai:template-ace-principal").getValue();
    if (isPlaceHolder(principalName)) {
      principalName = getValue(principalName, node.getSession());
    }
    final String pName = principalName.getString();
    Principal principal = new Principal() {

      public String getName() {
        return pName;
      }
    };

    // The permissions
    // TODO allow permissions to be filled in from the json.
    Value[] grantedValue = node.getProperty("sakai:template-ace-granted").getValues();
    String[] granted = new String[grantedValue.length];
    for (int i = 0; i < grantedValue.length; i++) {
      granted[i] = grantedValue[i].getString();
    }

    Value[] deniedValue = node.getProperty("sakai:template-ace-denied").getValues();
    String[] denied = new String[deniedValue.length];
    for (int i = 0; i < deniedValue.length; i++) {
      denied[i] = deniedValue[i].getString();
    }

    // Fill in the object.
    ACE ace = new ACE();
    ace.setPrincipal(principal);
    ace.setGrantedPrivileges(granted);
    ace.setDeniedPrivileges(denied);

    // Add it to the list.
    if (map.containsKey("rep:policy")) {
      // This is not the first ACE for this path.
      ((List<ACE>) map.get("rep:policy")).add(ace);
    } else {
      // This is the first ACE for this path.
      List<ACE> lst = new ArrayList<ACE>();
      lst.add(ace);
      map.put("rep:policy", lst);
    }
  }

  /**
   * @param node
   * @param map
   * @throws RepositoryException
   */
  private void handleChildNodes(Node node, Map<String, Object> map)
      throws RepositoryException {
    // Handle the child nodes for this node.
    NodeIterator childNodes = node.getNodes();
    while (childNodes.hasNext()) {
      Node child = childNodes.nextNode();

      String name = child.getName();
      // Node names that are of @@foo.bar?@@ are if structures.
      // The properties on these kind of nodes need to be set on the parent node.
      // Child nodes need to be added on the parent node.
      if (isIfStructure(name)) {
        Value val = getValue(name, node.getSession());
        if (isValueTrue(val)) {
          handleNode(child, map);
        }
      }
      // Node names that are of the form @@foo.bar(...)@@ are loop structures.
      // This means that bar should be an array in the json object.
      // This node's children will then be "copied" as many times as there are items in
      // the bar array sent over from the UI.
      else if (isLoopStructure(name)) {
        int[] newIndexes = new int[nestedLevel + 1];
        for (int i : loopIndexes) {
          newIndexes[i] = loopIndexes[i];
        }
        loopIndexes = newIndexes;
        nestedLevel++;
        for (;;) {
          try {
            NodeIterator loopChildNodes = child.getNodes();
            while (loopChildNodes.hasNext()) {
              Node loopChildNode = loopChildNodes.nextNode();
              Map<String, Object> childMap = new HashMap<String, Object>();
              handleNode(loopChildNode, childMap);

              // Maybe the name for this childnode needs to be filled in.
              String childName = loopChildNode.getName();
              if (isPlaceHolder(childName)) {
                childName = getValue(childName, loopChildNode.getSession()).getString();
              }

              // Add it to the map.
              map.put(childName, childMap);

              loopIndexes[nestedLevel]++;

            }
            loopIndexes[nestedLevel] = 0;
          } catch (Throwable t) {
            break;
          }
        }
        nestedLevel--;
      }

      else {

        // Handle this node.
        Map<String, Object> childMap = new HashMap<String, Object>();
        handleNode(child, childMap);

        // Maybe the name for this childnode needs to be filled in.
        String childName = child.getName();
        if (isPlaceHolder(childName)) {
          childName = getValue(childName, child.getSession()).getString();
        }

        // Add it to the map.
        map.put(childName, childMap);
      }
    }
  }

  /**
   * Returns true if the value equals true, "true", "1", or 1.
   * 
   * @param val
   * @return
   * @throws RepositoryException
   */
  private boolean isValueTrue(Value val) throws RepositoryException {
    return ((val.getType() == PropertyType.BOOLEAN && val.getBoolean())
        || (val.getType() == PropertyType.STRING && (val.getString().toLowerCase()
            .equals("true") || val.getString().equals("1"))) || ((val.getType() == PropertyType.LONG
        || val.getType() == PropertyType.DOUBLE || val.getType() == PropertyType.DECIMAL) && val
        .getLong() == 1));
  }

  /**
   * Handles a node's properties.
   * 
   * @param node
   *          The node to get the properties for.
   * @param map
   *          The map to add the name-values to
   * @throws RepositoryException
   */
  private void addProperties(Node node, Map<String, Object> map,
      List<String> propertiesToIgnore) throws RepositoryException {
    Session session = node.getSession();
    PropertyIterator pi = node.getProperties();
    while (pi.hasNext()) {
      Property p = pi.nextProperty();
      String pName = p.getName();

      // We ignore some properties.
      if (propertiesToIgnore != null
          && (propertiesToIgnore.contains(pName) || pName
              .equals(SLING_RESOURCE_TYPE_PROPERTY))) {
        continue;
      }

      // If the name is a place holder, we fill in the correct value.
      if (isPlaceHolder(pName)) {
        pName = getValue(pName, session).getString();
      }

      // Get the value(s) for this property and add it to the map.
      Object value = getPropertyValue(p);
      map.put(pName, value);

    }
  }

  /**
   * 
   * @param p
   * @return
   * @throws RepositoryException
   */
  private Object getPropertyValue(Property p) throws RepositoryException {
    if (p.getDefinition().isMultiple()) {
      Value[] values = p.getValues();
      for (int i = 0; i < values.length; i++) {
        if (isPlaceHolder(values[i])) {
          values[i] = getValue(values[i], p.getSession());
        }
      }
      return values;
    } else {
      Value value = p.getValue();
      if (isPlaceHolder(value)) {
        value = getValue(value, p.getSession());
      }
      return value;
    }
  }

  /**
   * @param name
   * @param siteJSON
   * @param session
   *          Needed to create a {@link Value value}.
   * @return
   * @throws RepositoryException
   */
  protected Value getValue(String name, Session session) throws RepositoryException {
    name = org.apache.commons.lang.StringUtils.remove(name, '@');
    name = org.apache.commons.lang.StringUtils.remove(name, '?');

    char[] characters = name.toCharArray();

    StringBuilder key = new StringBuilder();
    JSONObject j = json;
    Object o = json;
    boolean openParenthesis = false;
    int arrIndex = 0;
    boolean isArray = false;
    try {
      for (int i = 0; i <= characters.length; i++) {
        char character = (i < characters.length) ? characters[i] : '.';
        if (character == '(') {
          openParenthesis = true;
        } else if (character == ')') {
          if (openParenthesis) {
            isArray = true;
          }
          openParenthesis = false;
        }

        // Encountered a . in a string like: foo.bar
        // The string in the key variable will contain the key we should use.
        if (!isArray && !openParenthesis && character == '.') {
          o = j.get(key.toString());
          key.delete(0, key.length());
          if (o instanceof JSONObject) {
            j = (JSONObject) o;
          }
        }
        // Encountered a . after (...) like foo(...).bar .
        // Get the JSON array via the key 'foo' .
        else if (isArray && !openParenthesis && character == '.') {
          // Remove the '(...)' from the key.
          String k = key.substring(0, key.length() - 5);
          o = j.getJSONArray(k).get(loopIndexes[arrIndex]);
          arrIndex++;
          key.delete(0, key.length());
          if (o instanceof JSONObject) {
            j = (JSONObject) o;
          }
          isArray = false;
        } else {
          key.append(character);
        }
      }
    } catch (JSONException e) {
      throw new IllegalArgumentException(
          "Provided JSON does not compute with the template.");
    }

    return JcrUtils.createValue(o, session);
  }

  /**
   * Gets the value for a placeholder.
   * 
   * @param value
   *          The placeholder value of a template.
   * @param siteJSON
   *          JSON sent by the UI.
   * @param session
   * @return A Value with the correct value from the UI's JSON object.
   * @throws RepositoryException
   */
  protected Value getValue(Value value, Session session) throws RepositoryException {
    String placeHolder = value.getString();
    return getValue(placeHolder, session);

  }

  /**
   * @param value
   * @return
   * @throws RepositoryException
   */
  protected boolean isPlaceHolder(Value value) throws RepositoryException {
    return (value.getType() == PropertyType.STRING && isPlaceHolder(value.getString()));
  }

  /**
   * Checks if a String is a placeholder. Will only be true if the string starts with @@
   * and ends with @@.
   * 
   * @param name
   * @return
   */
  protected boolean isPlaceHolder(String name) {
    return (name.startsWith("@@") && name.endsWith("@@"));
  }

  /**
   * 
   * @param value
   * @return
   * @throws RepositoryException
   */
  protected boolean isIfStructure(Value value) throws RepositoryException {
    return (value.getType() == PropertyType.STRING && isIfStructure(value.getString()));
  }

  /**
   * @param string
   * @return
   */
  private boolean isIfStructure(String name) {
    return (name.startsWith("@@") && name.endsWith("?@@"));
  }

  /**
   * 
   * @param value
   * @return
   * @throws RepositoryException
   */
  protected boolean isLoopStructure(Value value) throws RepositoryException {
    return (value.getType() == PropertyType.STRING && isLoopStructure(value.getString()));
  }

  /**
   * @param name
   * @return
   */
  private boolean isLoopStructure(String name) {
    return (name.startsWith("@@") && name.endsWith("(...)@@"));
  }

}
