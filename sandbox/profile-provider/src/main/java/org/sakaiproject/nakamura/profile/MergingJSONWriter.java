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
  public void dump(Node baseNode, Map<Node, Future<Map<String, Object>>> providedNodeData,
      Writer w) throws JSONException, RepositoryException, InterruptedException, ExecutionException {
    if (providedNodeData.containsKey(baseNode)) {
      dump(providedNodeData.get(baseNode).get(), w);
    } else {
      for (PropertyIterator pi = baseNode.getProperties(); pi.hasNext();) {
        dump(pi.nextProperty(), w);
      }
      for (NodeIterator ni = baseNode.getNodes(); ni.hasNext();) {
        dump(ni.nextNode(), providedNodeData, w);
      }
    }
  }

  /**
   * @param map
   * @param w
   * @throws JSONException 
   */
  @SuppressWarnings("unchecked")
  private void dump(Map<String, Object> map, Writer w) throws JSONException {
    JSONWriter jw = new JSONWriter(w);
    jw.object();
    for(Entry<String,Object> e : map.entrySet()) {
      Object v = e.getValue();
      if ( v instanceof Map) {
        jw.key(e.getKey());
        dump((Map<String, Object>)e.getValue(),w);
      } else {
        jw.key(e.getKey());
        jw.value(e.getValue());
      }
    }
    jw.endObject();
  }

}
