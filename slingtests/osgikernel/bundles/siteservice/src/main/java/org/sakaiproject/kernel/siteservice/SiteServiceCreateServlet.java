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
package org.sakaiproject.kernel.siteservice;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SiteServiceCreateServlet</code>
 * @scr.service interface="javax.servlet.Servlet"
 *
 * @scr.component immediate="true" label="SiteServiceCreateServlet"
 *                description="Create servlet for site service" metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Processes site creation (POSTs to 'createsite' selector)"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/nonexisting"
 * @scr.property name="sling.servlet.methods" value="POST"
 * @scr.property name="sling.servlet.selectors" value="createsite"
 */
public class SiteServiceCreateServlet extends SlingAllMethodsServlet {

  private static final Logger LOG = LoggerFactory.getLogger(SiteServiceCreateServlet.class);
  private static final long serialVersionUID = 4874392318687088747L;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    LOG.info("Got post to SiteServiceCreateServlet. Creating site node");
    HtmlResponse htmlResponse = new HtmlResponse();
    htmlResponse.setReferer(request.getHeader("referer"));

    /* Path has '.createsite.html' attached. Remove that */
    String path = removeSelectorSuffix(request.getResource().getPath());
    
    htmlResponse.setPath(path);

    Session session = request.getResourceResolver().adaptTo(Session.class);
    Map<String, RequestProperty> reqProperties = collectContent(request, htmlResponse);
    List<Modification> changes = new ArrayList<Modification>();
    try {
      Node siteNode = deepGetOrCreateNode(session, path, reqProperties, changes);
      siteNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, SiteServiceGetServlet.SITE_RESOURCE_TYPE);
      
      //
      
      LOG.info("Created node: " + siteNode.getPath());
      session.save();
    } catch (RepositoryException e) {
      LOG.error("Unable to create node", e);
      htmlResponse.setError(e);
      htmlResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to create site");
    }
    htmlResponse.send(response, false);
  }

  private String removeSelectorSuffix(String path) {
    int suffix = path.lastIndexOf(".createsite");
    if (suffix > -1)
    {
      path = path.substring(0, suffix);
    }
    if (path.endsWith("/"))
    {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  /**
   * Deep gets or creates a node, parent-padding with default nodes nodes. If
   * the path is empty, the given parent node is returned.
   * 
   * @param path
   *          path to node that needs to be deep-created
   * @return node at path
   * @throws RepositoryException
   *           if an error occurs
   * @throws IllegalArgumentException
   *           if the path is relative and parent is <code>null</code>
   */
  private Node deepGetOrCreateNode(Session session, String path,
      Map<String, RequestProperty> reqProperties, List<Modification> changes)
      throws RepositoryException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Deep-creating Node '{}'", path);
    }
    if (path == null || !path.startsWith("/")) {
      throw new IllegalArgumentException("path must be an absolute path.");
    }
    // get the starting node
    String startingNodePath = path;
    Node startingNode = null;
    while (startingNode == null) {
      if (startingNodePath.equals("/")) {
        startingNode = session.getRootNode();
      } else if (session.itemExists(startingNodePath)) {
        startingNode = (Node) session.getItem(startingNodePath);
      } else {
        int pos = startingNodePath.lastIndexOf('/');
        if (pos > 0) {
          startingNodePath = startingNodePath.substring(0, pos);
        } else {
          startingNodePath = "/";
        }
      }
    }
    // is the searched node already existing?
    if (startingNodePath.length() == path.length()) {
      return startingNode;
    }
    // create nodes
    int from = (startingNodePath.length() == 1 ? 1 : startingNodePath.length() + 1);
    Node node = startingNode;
    while (from > 0) {
      final int to = path.indexOf('/', from);
      final String name = to < 0 ? path.substring(from) : path.substring(from, to);
      // although the node should not exist (according to the first test
      // above)
      // we do a sanety check.
      if (node.hasNode(name)) {
        node = node.getNode(name);
      } else {
        final String tmpPath = to < 0 ? path : path.substring(0, to);
        // check for node type
        final String nodeType = getPrimaryType(reqProperties, tmpPath);
        if (nodeType != null) {
          node = node.addNode(name, nodeType);
        } else {
          node = node.addNode(name);
        }
        // check for mixin types
        final String[] mixinTypes = getMixinTypes(reqProperties, tmpPath);
        if (mixinTypes != null) {
          for (String mix : mixinTypes) {
            node.addMixin(mix);
          }
        }
        changes.add(Modification.onCreated(node.getPath()));
      }
      from = to + 1;
    }
    return node;
  }

  /**
   * Checks the collected content for a jcr:mixinTypes property at the specified
   * path.
   * 
   * @param path
   *          path to check
   * @return the mixin types or <code>null</code>
   */
  private String[] getMixinTypes(Map<String, RequestProperty> reqProperties, String path) {
    RequestProperty prop = reqProperties.get(path + "/jcr:mixinTypes");
    return (prop == null) || !prop.hasValues() ? null : prop.getStringValues();
  }

  /**
   * Checks the collected content for a jcr:primaryType property at the
   * specified path.
   * 
   * @param path
   *          path to check
   * @return the primary type or <code>null</code>
   */
  private String getPrimaryType(Map<String, RequestProperty> reqProperties, String path) {
    RequestProperty prop = reqProperties.get(path + "/jcr:primaryType");
    return prop == null ? null : prop.getStringValues()[0];
  }

  /**
   * Returns the <code>paramName</code> as an absolute (unnormalized)
   * property path by prepending the response path (<code>response.getPath</code>)
   * to the parameter name if not already absolute.
   */
  private String toPropertyPath(String paramName, HtmlResponse response) {
      if (!paramName.startsWith("/")) {
          paramName = ResourceUtil.normalize(response.getPath() + '/' + paramName);
      }

      return paramName;
  }

  /**
   * Collects the properties that form the content to be written back to the
   * repository.
   * 
   * @throws RepositoryException
   *           if a repository error occurs
   * @throws ServletException
   *           if an internal error occurs
   */
  private Map<String, RequestProperty> collectContent(SlingHttpServletRequest request,
      HtmlResponse response) {

    boolean requireItemPrefix = requireItemPathPrefix(request);

    // walk the request parameters and collect the properties
    Map<String, RequestProperty> reqProperties = new HashMap<String, RequestProperty>();
    for (Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap().entrySet()) {
      final String paramName = e.getKey();

      // do not store parameters with names starting with sling:post
      if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
        continue;
      }
      // SLING-298: skip form encoding parameter
      if (paramName.equals("_charset_")) {
        continue;
      }
      // skip parameters that do not start with the save prefix
      if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
        continue;
      }

      // ensure the paramName is an absolute property name
      String propPath = toPropertyPath(paramName, response);

      // @TypeHint example
      // <input type="text" name="./age" />
      // <input type="hidden" name="./age@TypeHint" value="long" />
      // causes the setProperty using the 'long' property type
      if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.TYPE_HINT_SUFFIX);

        final RequestParameter[] rp = e.getValue();
        if (rp.length > 0) {
          prop.setTypeHintValue(rp[0].getString());
        }

        continue;
      }

      // @DefaultValue
      if (propPath.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.DEFAULT_VALUE_SUFFIX);

        prop.setDefaultValues(e.getValue());

        continue;
      }

      // SLING-130: VALUE_FROM_SUFFIX means take the value of this
      // property from a different field
      // @ValueFrom example:
      // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
      // causes the JCR Text property to be set to the value of the
      // fulltext form field.
      if (propPath.endsWith(SlingPostConstants.VALUE_FROM_SUFFIX)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.VALUE_FROM_SUFFIX);

        // @ValueFrom params must have exactly one value, else ignored
        if (e.getValue().length == 1) {
          String refName = e.getValue()[0].getString();
          RequestParameter[] refValues = request.getRequestParameters(refName);
          if (refValues != null) {
            prop.setValues(refValues);
          }
        }

        continue;
      }

      // SLING-458: Allow Removal of properties prior to update
      // @Delete example:
      // <input name="./Text@Delete" type="hidden" />
      // causes the JCR Text property to be deleted before update
      if (propPath.endsWith(SlingPostConstants.SUFFIX_DELETE)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_DELETE);

        prop.setDelete(true);

        continue;
      }

      // SLING-455: @MoveFrom means moving content to another location
      // @MoveFrom example:
      // <input name="./Text@MoveFrom" type="hidden" value="/tmp/path" />
      // causes the JCR Text property to be set by moving the /tmp/path
      // property to Text.
      if (propPath.endsWith(SlingPostConstants.SUFFIX_MOVE_FROM)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_MOVE_FROM);

        // @MoveFrom params must have exactly one value, else ignored
        if (e.getValue().length == 1) {
          String sourcePath = e.getValue()[0].getString();
          prop.setRepositorySource(sourcePath, true);
        }

        continue;
      }

      // SLING-455: @CopyFrom means moving content to another location
      // @CopyFrom example:
      // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
      // causes the JCR Text property to be set by copying the /tmp/path
      // property to Text.
      if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
        RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath,
            SlingPostConstants.SUFFIX_COPY_FROM);

        // @MoveFrom params must have exactly one value, else ignored
        if (e.getValue().length == 1) {
          String sourcePath = e.getValue()[0].getString();
          prop.setRepositorySource(sourcePath, false);
        }

        continue;
      }

      // plain property, create from values
      RequestProperty prop = getOrCreateRequestProperty(reqProperties, propPath, null);
      prop.setValues(e.getValue());
    }

    return reqProperties;
  }

  /**
   * Returns true if any of the request parameters starts with
   * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>}. In
   * this case only parameters starting with either of the prefixes
   * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
   * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>} and
   * {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>} are
   * considered as providing content to be stored. Otherwise all parameters not
   * starting with the command prefix <code>:</code> are considered as
   * parameters to be stored.
   */
  protected final boolean requireItemPathPrefix(SlingHttpServletRequest request) {

    boolean requirePrefix = false;

    Enumeration<?> names = request.getParameterNames();
    while (names.hasMoreElements() && !requirePrefix) {
      String name = (String) names.nextElement();
      requirePrefix = name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT);
    }

    return requirePrefix;
  }

  /**
   * Returns <code>true</code> if the <code>name</code> starts with either of
   * the prefixes {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT
   * <code>./</code>}, {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT
   * <code>../</code>} and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE
   * <code>/</code>}.
   */
  protected boolean hasItemPathPrefix(String name) {
    return name.startsWith(SlingPostConstants.ITEM_PREFIX_ABSOLUTE)
        || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT)
        || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_PARENT);
  }

  /**
   * Orders the given node according to the specified command. The following
   * syntax is supported: <xmp> | first | before all child nodes | before A |
   * before child node A | after A | after child node A | last | after all nodes
   * | N | at a specific position, N being an integer </xmp>
   * 
   * @param item
   *          node to order
   * @throws RepositoryException
   *           if an error occurs
   */
  protected void orderNode(SlingHttpServletRequest request, Item item, List<Modification> changes)
      throws RepositoryException {

    String command = request.getParameter(SlingPostConstants.RP_ORDER);
    if (command == null || command.length() == 0) {
      // nothing to do
      return;
    }

    if (!item.isNode()) {
      return;
    }

    Node parent = item.getParent();

    String next = null;
    if (command.equals(SlingPostConstants.ORDER_FIRST)) {

      next = parent.getNodes().nextNode().getName();

    } else if (command.equals(SlingPostConstants.ORDER_LAST)) {

      next = "";

    } else if (command.startsWith(SlingPostConstants.ORDER_BEFORE)) {

      next = command.substring(SlingPostConstants.ORDER_BEFORE.length());

    } else if (command.startsWith(SlingPostConstants.ORDER_AFTER)) {

      String name = command.substring(SlingPostConstants.ORDER_AFTER.length());
      NodeIterator iter = parent.getNodes();
      while (iter.hasNext()) {
        Node n = iter.nextNode();
        if (n.getName().equals(name)) {
          if (iter.hasNext()) {
            next = iter.nextNode().getName();
          } else {
            next = "";
          }
        }
      }

    } else {
      // check for integer
      try {
        // 01234
        // abcde move a -> 2 (above 3)
        // bcade move a -> 1 (above 1)
        // bacde
        int newPos = Integer.parseInt(command);
        next = "";
        NodeIterator iter = parent.getNodes();
        while (iter.hasNext() && newPos >= 0) {
          Node n = iter.nextNode();
          if (n.getName().equals(item.getName())) {
            // if old node is found before index, need to
            // inc index
            newPos++;
          }
          if (newPos == 0) {
            next = n.getName();
            break;
          }
          newPos--;
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("provided node ordering command is invalid: " + command);
      }
    }

    if (next != null) {
      if (next.equals("")) {
        next = null;
      }
      parent.orderBefore(item.getName(), next);
      changes.add(Modification.onOrder(item.getPath(), next));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node {} moved '{}'", item.getPath(), command);
      }
    } else {
      throw new IllegalArgumentException("provided node ordering command is invalid: " + command);
    }
  }

  /**
   * Returns the request property for the given property path. If such a request
   * property does not exist yet it is created and stored in the
   * <code>props</code>.
   * 
   * @param props
   *          The map of already seen request properties.
   * @param paramName
   *          The absolute path of the property including the
   *          <code>suffix</code> to be looked up.
   * @param suffix
   *          The (optional) suffix to remove from the <code>paramName</code>
   *          before looking it up.
   * @return The {@link RequestProperty} for the <code>paramName</code>.
   */
  private RequestProperty getOrCreateRequestProperty(Map<String, RequestProperty> props,
      String paramName, String suffix) {
    if (suffix != null && paramName.endsWith(suffix)) {
      paramName = paramName.substring(0, paramName.length() - suffix.length());
    }

    RequestProperty prop = props.get(paramName);
    if (prop == null) {
      prop = new RequestProperty(paramName);
      props.put(paramName, prop);
    }

    return prop;
  }

}
