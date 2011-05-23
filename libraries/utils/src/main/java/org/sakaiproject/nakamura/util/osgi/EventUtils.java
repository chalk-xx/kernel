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
package org.sakaiproject.nakamura.util.osgi;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public class EventUtils {

  /**
   * Send an OSGi event based on a JCR Observation Event.
   *
   * @param resource
   *          The resource we should send an event for.
   * @param properties
   *          A set of properties that should be sent. By default the following properties
   *          will be set:
   *          <ul>
   *          <li>path - The path of the resource trough resource.getPath();</li>
   *          <li>resourceType - The resource type of the resource, if not null.</li>
   *          <li>resourceSuperType - The super resource type of the resource, if not null
   *          <li>
   *          </ul>
   * @param topic
   *          The topic that should be used for the OSGi event.
   * @param eventAdmin
   *          The OSGi Event admin that can be used to post events.
   */
  public static void sendOsgiEvent(Resource resource,
      final Dictionary<String, String> properties, final String topic,
      final EventAdmin eventAdmin) {
    if (resource != null) {
      String path = resource.getPath();
      // check for nt:file nodes
      if (path.endsWith("/jcr:content")) {
        final Node node = resource.adaptTo(Node.class);
        if (node != null) {
          try {
            if (node.getParent().isNodeType("nt:file")) {
              final Resource parentResource = resource.getParent();
              if (parentResource != null) {
                resource = parentResource;
              }
            }
          } catch (RepositoryException re) {
            // ignore this
          }
        }
      }
      // The path is always included in the properties we send along.
      properties.put(SlingConstants.PROPERTY_PATH, resource.getPath());
      final String resourceType = resource.getResourceType();
      if (resourceType != null) {
        properties.put(SlingConstants.PROPERTY_RESOURCE_TYPE, resource.getResourceType());
      }
      final String resourceSuperType = resource.getResourceSuperType();
      if (resourceSuperType != null) {
        properties.put(SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE, resource
            .getResourceSuperType());
      }
      sendOsgiEvent(properties, topic, eventAdmin);
    }
  }

  /**
   *
   * @param properties
   *          A set of properties that should be sent.f
   * @param topic
   *          The topic that should be used for the OSGi event.
   * @param eventAdmin
   *          The OSGi Event admin that can be used to post events.
   */
  public static void sendOsgiEvent(final Dictionary<String, String> properties,
      final String topic, final EventAdmin eventAdmin) {
    eventAdmin.postEvent(new Event(topic, properties));
  }
  
  /**
   * Clean an event property value in order to send it as a property on a JMS Message
   * Primitive types, Strings, and Lists are returned unmodified
   * Arrays are converted to Lists. Nested Lists are not converted.
   * The values in Maps are inspected and converted using cleanProperty(obj)
   */
  @SuppressWarnings("unchecked")
  public static Object cleanProperty(Object obj){
    if (obj instanceof Byte || obj instanceof Boolean || obj instanceof Character
	     || obj instanceof Number || obj instanceof String
	     || obj instanceof List) {
      return obj;
    }
    if (obj instanceof Object[]){
      return Arrays.asList((Object[])obj);
    }
    if (obj instanceof Map){
      Map<String,Object> oldMap = (Map<String,Object>)obj;
      Builder<String, Object> newMap = ImmutableMap.builder();
      for (String key: oldMap.keySet()){
        Object newVal = cleanProperty(oldMap.get(key));
        if (newVal != null){
          newMap.put(key, newVal);
        }
      }
      return newMap.build();
    }
    return null;
  }
}
