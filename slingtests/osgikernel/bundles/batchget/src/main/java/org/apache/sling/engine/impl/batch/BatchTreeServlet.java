package org.apache.sling.engine.impl.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONString;
import org.apache.sling.servlets.post.impl.SlingPostServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows for bundled POST requests.
 * 
 * @scr.component immediate="true" label="BatchTreeServlet"
 *                description="Create an entire tree of nodes in the repository in 1 request"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description"
 *               value="Create an entire tree of nodes in the repository in 1 request."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/batch/tree"
 * @scr.property name="sling.servlet.methods" value="POST"
 */
public class BatchTreeServlet extends SlingPostServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 9207596135556346980L;
  private static final String TREE_PARAM = "tree";
  private static final String PATH_PARAM = "path";
  private final Logger LOG = LoggerFactory.getLogger(BatchTreeServlet.class);

  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {

    // Check parameters
    RequestParameter treeParam = request.getRequestParameter(TREE_PARAM);
    RequestParameter pathParam = request.getRequestParameter(PATH_PARAM);
    if (treeParam == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter '"
          + TREE_PARAM + "' not found.");
      return;
    }
    if (pathParam == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter '"
          + PATH_PARAM + "' not found.");
      return;
    }

    String path = pathParam.getString();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    JSONObject json = null;
    Node node = null;

    // Get the node where we start the tree from.
    try {
      if (!session.itemExists(path)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "path not found.");
        return;
      }
      node = (Node) session.getItem(path);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Item not found.");
      return;
    }

    // Get the JSON Object.
    try {
      json = new JSONObject(treeParam.getString());
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter '"
          + TREE_PARAM + "' is not valid json format.");
      return;
    }

    try {
      // Start creating the tree.
      createTree(session, json, node);

      // Save the session.
      if (session.hasPendingChanges()) {
        session.save();
      }
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Unable to create tree");
      LOG.warn("Unable to create tree: {}", e.getMessage());
      e.printStackTrace();
    }
  }

  private void createTree(Session session, JSONObject json, Node node)
      throws JSONException, ItemExistsException, PathNotFoundException,
      VersionException, ConstraintViolationException, LockException,
      RepositoryException {

    Iterator<String> keys = json.keys();
    while (keys.hasNext()) {

      String key = keys.next();
      Object obj = json.get(key);

      if (obj instanceof JSONObject) {
        // This represents a child node.
        Node childNode = addNode(node, key);
        createTree(session, (JSONObject) obj, childNode);
      } else if (obj instanceof JSONArray) {
        // This represents a multivalued property

        JSONArray arr = (JSONArray) obj;
        String[] values = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
          values[i] = (String) arr.get(i);
        }
        node.setProperty(key, values);

      } else {
        // Single property
        // Be smart, and check Number etc
        if (obj instanceof JSONString) {
          Object o;
          try {
            o = ((JSONString) obj).toJSONString();
          } catch (Exception e) {
            throw new JSONException(e);
          }
          if (o instanceof String) {
            node.setProperty(key, (String) obj);
          }
        }
        if (obj instanceof Number) {
          node.setProperty(key, (Long) obj);
        }
        if (obj instanceof Boolean) {
          node.setProperty(key, (Boolean) obj);
        }

      }
    }
  }

  private Node addNode(Node node, String key) throws ItemExistsException,
      PathNotFoundException, VersionException, ConstraintViolationException,
      LockException, RepositoryException {
    if (node.hasNode(key)) {
      return node.getNode(key);
    }

    return node.addNode(key);
  }
}
