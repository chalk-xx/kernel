package org.sakaiproject.nakamura.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * @scr.component metatype="no" immediate="true"
 * @scr.service
 * @scr.property name="sling.post.operation" value="tag"
 */
public class TagOperation extends AbstractSlingPostOperation {

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {

    Iterator<Resource> res = getApplyToResources(request);
    RequestParameter[] params = request.getRequestParameters("sakai:tags");

    List<String> newTags = new ArrayList<String>();
    for (RequestParameter rp : params) {
      newTags.add(rp.getString());
    }

    if (res == null) {

      Resource resource = request.getResource();
      Item item = resource.adaptTo(Item.class);
      if (item == null) {
        throw new ResourceNotFoundException("Missing source " + resource + " for tagging");
      }

      setTags(item, newTags);
      changes.add(Modification.onModified(resource.getPath()));

    } else {

      while (res.hasNext()) {
        Resource resource = res.next();
        Item item = resource.adaptTo(Item.class);
        if (item != null) {
          setTags(item, newTags);
          changes.add(Modification.onModified(resource.getPath()));
        }
      }

    }
  }

  /**
   * Writes a set of tags to a node.
   * 
   * @param item
   * @param newTags
   * @throws ValueFormatException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  private void setTags(Item item, List<String> newTags) throws ValueFormatException,
      PathNotFoundException, RepositoryException {
    Node n = (Node) item;
    // Get the old properties
    List<String> oldTags = new ArrayList<String>();
    if (n.hasProperty("sakai:tags")) {
      Value[] tags = n.getProperty("sakai:tags").getValues();
      for (Value v : tags) {
        oldTags.add(v.getString());
      }
    }

    // Add our new ones.
    for (String tag : newTags) {
      if (!oldTags.contains(tag)) {
        oldTags.add(tag);
      }
    }

    // Write them back.
    String[] writeTags = new String[oldTags.size()];
    for (int i = 0; i < oldTags.size(); i++) {
      writeTags[i] = oldTags.get(i);
    }
    n.setProperty("sakai:tags", writeTags);
    n.save();
  }

}
