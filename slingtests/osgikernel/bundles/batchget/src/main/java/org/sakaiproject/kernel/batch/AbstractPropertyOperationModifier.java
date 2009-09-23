package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public abstract class AbstractPropertyOperationModifier extends
    AbstractSlingPostOperation {

  @SuppressWarnings("unchecked")
  @Override
  protected abstract void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException;

  @SuppressWarnings("unchecked")
  public void modifyProperties(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    // Get all the resources
    Iterator<Resource> res = getApplyToResources(request);
    String operation = request.getRequestParameter(":operation").getString();
    Map<String, String[]> params = request.getParameterMap();

    if (res == null) {
      Resource resource = request.getResource();
      Item item = resource.adaptTo(Item.class);
      if (item == null) {
        throw new ResourceNotFoundException("Missing source " + resource + " for tagging");
      }
      modifyProperties(operation, item, params);
      changes.add(Modification.onModified(resource.getPath()));
    } else {
      while (res.hasNext()) {
        Resource resource = res.next();
        Item item = resource.adaptTo(Item.class);
        if (item != null) {
          changes.add(Modification.onModified(resource.getPath()));
        }
      }
    }
  }

  /**
   * Loops over the properties of a node and modifies all those that are provided in the
   * parameters map.
   * 
   * @param operation
   * @param item
   * @param params
   * @throws RepositoryException
   */
  protected void modifyProperties(String operation, Item item,
      Map<String, String[]> params) throws RepositoryException {
    if (item.isNode()) {
      Node node = (Node) item;
      for (String prop : params.keySet()) {
        // We skip the :operation property.
        if (!prop.equals(":operation")) {

          // Get existing values out of JCR.
          List<String> oldValues = new ArrayList<String>();
          if (node.hasProperty(prop)) {
            try {
              Value[] vals = node.getProperty(prop).getValues();
              for (Value v : vals) {
                oldValues.add(v.getString());
              }
            } catch (ValueFormatException e) {
              oldValues.add(node.getProperty(prop).getValue().getString());
            }
          }

          // Add or delete the vales.
          for (String s : params.get(prop)) {
            if (operation.equals("addProperty") && !oldValues.contains(s)) {
              oldValues.add(s);
            } else if (operation.equals("removeProperty") && oldValues.contains(s)) {
              oldValues.remove(s);
            }
          }

          // Write the properties to the node and save them.
          String[] newValues = new String[oldValues.size()];
          for (int i = 0; i < oldValues.size(); i++) {
            newValues[i] = oldValues.get(i);
          }
          node.setProperty(prop, newValues);
          node.save();
        }
      }
    }
  }
}
