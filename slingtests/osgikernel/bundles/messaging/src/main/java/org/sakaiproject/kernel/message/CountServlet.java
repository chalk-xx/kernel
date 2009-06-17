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
package org.sakaiproject.kernel.message;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * Will count all the messages under the user his message store. The user can
 * specify what messages should be counted by specifying parameters in comma
 * seperated values. ex:
 * messages.count.json?filters=sakai:messagebox,read,to&values=inbox,true,user1&groupedby=sakai:messagebox
 * The following are optional:
 *  - filters: only nodes with the properties in filters and the values in values get travers
 *  - groupedby: group the results by the values of this parameter.
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="count"
 */
public class CountServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -5714446506015596037L;

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    // Get this node so we can get the session off it.
    Node node = (Node) request.getResource().adaptTo(Node.class);

    try {
      // Do the query
      // We do the query on the user his messageStore's path.
      String messageStorePath = ISO9075.encodePath(node.getPath());
      //String messageStorePath = node.getPath();
      String queryString = "/jcr:root" + messageStorePath
          + "//*[@sling:resourceType='sakai/message'";

      // Get the filters
      if (request.getRequestParameter("filters") != null
          && request.getRequestParameter("values") != null) {
        // The user wants to filter some things.
        String[] filters = request.getRequestParameter("filters").getString()
            .split(",");
        String[] values = request.getRequestParameter("values").getString()
            .split(",");
        if (filters.length != values.length) {
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "The amount of values doesn't match the amount of keys.");
        }

        for (int i = 0; i < filters.length; i++) {
          queryString += " and @" + filters[i] + "='" + values[i] + "'";
        }
      }

      queryString += "]";

      // Do the query and output how many results we have.
      QueryManager queryManager = node.getSession().getWorkspace()
          .getQueryManager();
      Query query = queryManager.createQuery(queryString, "xpath");
      QueryResult result = query.execute();
      JSONWriter write = new JSONWriter(response.getWriter());
      NodeIterator resultNodes = result.getNodes();

      if (request.getRequestParameter("groupedby") == null) {
        write.object();
        write.key("count");
        // TODO: getSize iterates over all the nodes, add a JackRabbit service to fetch this number.
        write.value("" + resultNodes.getSize());
        write.endObject();
      } else {
        // The user want to group the count by a specified set.
        // We will have to traverse each node, get that property and count each value for it.
        String groupedby = request.getRequestParameter("groupedby").getString();
        Map<String, Integer> mapCount = new HashMap<String, Integer>();
        while (resultNodes.hasNext()) {
          Node n = resultNodes.nextNode();
          if (n.hasProperty(groupedby)) {
            String key = n.getProperty(groupedby).getString();
            int val = 1;
            if (mapCount.containsKey(key)) {
              val = mapCount.get(key) + 1;
            }
            mapCount.put(key, val);
          }
        }

        write.object();
        write.key("count");
        write.array();
        for (String k : mapCount.keySet()) {
          write.object();

          write.key("group");
          write.value(k);
          write.key("count");
          write.value(mapCount.get(k));

          write.endObject();
        }
        write.endArray();
        write.endObject();

      }

    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
}
