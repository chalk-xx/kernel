package org.sakaiproject.nakamura.resource.lite.servlet.post.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

public class DeleteOperation extends AbstractSlingPostOperation {

  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes)
      throws StorageClientException, AccessDeniedException, IOException {
    VersioningConfiguration versioningConfiguration = getVersioningConfiguration(request);

    Iterator<Resource> res = getApplyToResources(request);
    if (res == null) {

      Resource resource = request.getResource();
      Content contentItem = resource.adaptTo(Content.class);
      if (contentItem == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND, "Missing source " + resource
            + " for delete");
        return;
      }
      contentManager.delete(contentItem.getPath());
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
