package org.sakaiproject.nakamura.doc;

import static org.sakaiproject.nakamura.api.doc.DocumentationConstants.CSS_CLASS_PATH;
import static org.sakaiproject.nakamura.api.doc.DocumentationConstants.CSS_CLASS_SHORT_DESCRIPTION;

import org.sakaiproject.nakamura.api.doc.DocumentationConstants;

import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

public class DocumentationWriter {

  /**
   * Write out a list of nodes.
   * 
   * @param session
   *          The current JCR session.
   * @param writer
   *          The writer where the response should go to.
   * @param query
   *          The query to use to retrieve the nodes.
   * @throws InvalidQueryException
   * @throws RepositoryException
   */
  public static void writeNodes(Session session, PrintWriter writer,
      String query, String servlet) throws InvalidQueryException, RepositoryException {
    // Write the HTML header.
    writer.append(DocumentationConstants.HTML_HEADER);

    // Begin list
    writer.append("<h1>Search nodes</h1>");
    writer.append("<ul class=\"").append(
        DocumentationConstants.CSS_CLASS_DOCUMENTATION_LIST).append("\">");

    QueryManager qm = session.getWorkspace().getQueryManager();
    Query q = qm.createQuery(query, Query.XPATH);
    QueryResult result = q.execute();
    NodeIterator iterator = result.getNodes();
    while (iterator.hasNext()) {
      Node node = iterator.nextNode();
      NodeDocumentation doc = new NodeDocumentation(node);

      writer.append("<li><a href=\"");
      writer.append(servlet);
      writer.append("?p=");
      writer.append(doc.getPath());
      writer.append("\">");
      if (doc.getTitle() != null && !doc.getTitle().equals("")) {
        writer.append(doc.getTitle());
      } else {
        writer.append(doc.getPath());
      }
      writer.append("</a><span class=\"").append(CSS_CLASS_PATH).append("\">");
      writer.append(doc.getPath());
      writer.append("</span><p class=\"").append(CSS_CLASS_SHORT_DESCRIPTION)
          .append("\">");
      writer.append(doc.getShortDescription());
      writer.append("</p></li>");

    }

    // End list
    writer.append("</ul>");

    // Footer
    writer.append(DocumentationConstants.HTML_FOOTER);
  }

  /**
   * Write info for a specific node.
   * @param path The path to the node.
   * @param session The current JCR session.
   * @param writer The writer to send the response to.
   * @throws RepositoryException
   */
  public static void writeSearchInfo(String path, Session session,
      PrintWriter writer) throws RepositoryException {
    Node node = (Node) session.getItem(path);
    NodeDocumentation doc = new NodeDocumentation(node);
    writer.append(DocumentationConstants.HTML_HEADER);
    writer.append("<h1>Search node: ");
    writer.append(doc.getTitle());
    writer.append("</h1>");
    doc.send(writer);
    writer.append(DocumentationConstants.HTML_FOOTER);
  }
}
