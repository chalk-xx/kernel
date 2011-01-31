package org.sakaiproject.nakamura.resource.lite.servlet.post.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.AbstractSparsePostOperation;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

public class DeleteOperation extends AbstractSparsePostOperation {

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes, String contentPath)
      throws StorageClientException, AccessDeniedException, IOException {

    Iterator<Resource> res = getApplyToResources(request);
    if (res == null) {

      Resource resource = request.getResource();
      if (contentPath == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND, "Missing source " + contentPath
            + " for delete");
        return;
      }
      contentManager.delete(contentPath);
      changes.add(Modification.onDeleted(resource.getPath()));

    } else {

      while (res.hasNext()) {
        Resource resource = res.next();
        Content contentItem = resource.adaptTo(Content.class);
        if (contentItem != null) {
          contentManager.delete(contentItem.getPath());
          changes.add(Modification.onDeleted(resource.getPath()));
        }
      }

    }

  }
}
