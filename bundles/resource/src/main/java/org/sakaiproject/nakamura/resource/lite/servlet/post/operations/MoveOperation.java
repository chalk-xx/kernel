package org.sakaiproject.nakamura.resource.lite.servlet.post.operations;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.ActionRecord;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.AbstractSparsePostOperation;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.util.ArrayList;
import java.util.List;

public class MoveOperation extends AbstractSparsePostOperation {

  private static final String DEST = ":dest";

  public void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes, String contentPath)
      throws StorageClientException, AccessDeniedException {

    String from = contentPath;
    String to = LitePersonalUtils.expandHomeDirectory(request.getParameter(DEST));
    List<ActionRecord> moves = (ArrayList<ActionRecord>) contentManager.moveWithChildren(
        from, to);
    for (int i = 0; i < moves.size(); i++) {
      ActionRecord move = moves.get(i);
      changes.add(Modification.onMoved(move.getFrom(), move.getTo()));
    }
  }
}
