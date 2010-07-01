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
package org.sakaiproject.nakamura.profile;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.json.jcr.JsonItemWriter;

import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class MergingJSONWriter extends JsonItemWriter {

  /**
   * @param propertyNamesToIgnore
   */
  public MergingJSONWriter(Set<String> propertyNamesToIgnore) {
    super(propertyNamesToIgnore);
  }

  /**
   * @param baseNode
   * @param providedNodeData
   * @param w
   * @throws RepositoryException
   * @throws JSONException
   * @throws ValueFormatException
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void dump(Node baseNode,
      Map<Node, Future<Map<String, Object>>> providedNodeData, Writer w)
      throws JSONException, RepositoryException, InterruptedException, ExecutionException {
    JSONWriter jw = new JSONWriter(w);
    dump(jw, baseNode, providedNodeData);
  }

  protected void dump(JSONWriter jw, Node baseNode,
      Map<Node, Future<Map<String, Object>>> providedNodeData) throws JSONException,
      RepositoryException, InterruptedException, ExecutionException {
    jw.object();
    if (providedNodeData.containsKey(baseNode)) {
      jw.key(baseNode.getName());
      dump(jw, providedNodeData.get(baseNode).get());
    } else {
      System.err.println("Base Node is "+baseNode);
      PropertyIterator pi = baseNode.getProperties();
      System.err.println("Property Iterator is  "+pi);
      
      for (; pi.hasNext();) {
        writeProperty(jw, pi.nextProperty());
      }
      NodeIterator ni = baseNode.getNodes();
      for (; ni.hasNext();) {
        Node node = ni.nextNode();
        if (providedNodeData.containsKey(node)) {
          jw.key(node.getName());
          dump(jw, providedNodeData.get(node).get());
        } else {
          jw.key(node.getName());
          dump(jw, node, providedNodeData);
        }
      }
    }
    jw.endObject();
  }

  /**
   * @param map
   * @param w
   * @throws JSONException
   */
  @SuppressWarnings("unchecked")
  private void dump(JSONWriter jw, Map<String, Object> map) throws JSONException {
    jw.object();
    for (Entry<String, Object> e : map.entrySet()) {
      Object v = e.getValue();
      if (v instanceof Map) {
        jw.key(e.getKey());
        dump(jw, (Map<String, Object>) e.getValue());
      } else {
        jw.key(e.getKey());
        jw.value(e.getValue());
      }
    }
    jw.endObject();
  }

}
