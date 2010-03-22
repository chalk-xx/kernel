package org.sakaiproject.nakamura.docproxy.url;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;

/**
 * 
 * @author <a href="mailto:carl@hallwaytech.com">Carl Hall</a>
 */
@Service
@Component
public class UrlRepositoryProcessor implements ExternalRepositoryProcessor {

  @Property(value = "GET|http://localhost/search")
  private static final String SEARCH_URL = "search.url";
  private HttpMethod searchMethod;

  @Property(value = "GET|http://localhost/doc")
  private static final String DOCUMENT_URL = "document.url";
  private HttpMethod documentMethod;

  @Property(value = "POST|http://localhost")
  private static final String UPDATE_URL = "update.url";
  private HttpMethod updateMethod;

  @Property(value = "GET|http://localhost")
  private static final String METADATA_URL = "metadata.url";
  private HttpMethod metadataMethod;

  @Property(value = "DELETE|http://localhost")
  private static final String REMOVE_URL = "remove.url";
  private HttpMethod removeMethod;

  @Activate
  protected void activate(ComponentContext context) {
    // process properties into http methods
    Dictionary props = context.getProperties();

    String search = (String) props.get(SEARCH_URL);
    searchMethod = parseUrlish(search);

    String document = (String) props.get(DOCUMENT_URL);
    documentMethod = parseUrlish(document);

    String update = (String) props.get(UPDATE_URL);
    updateMethod = parseUrlish(update);

    String metadata = (String) props.get(METADATA_URL);
    metadataMethod = parseUrlish(metadata);

    String remove = (String) props.get(REMOVE_URL);
    removeMethod = parseUrlish(remove);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#updateDocument
   * (javax.jcr.Node, java.lang.String, java.util.Map, java.io.InputStream, long)
   */
  public Map<String, Object> updateDocument(Node node, String path,
      Map<String, Object> properties, InputStream documentStream, long streamLength)
      throws DocProxyException {
    try {
      executeMethod(updateMethod);
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getDocument(javax
   * .jcr.Node, java.lang.String)
   */
  public ExternalDocumentResult getDocument(Node node, String path)
      throws DocProxyException {
    ExternalDocumentResult result = null;
    try {
      executeMethod(documentMethod);
      result = parseDocument(documentMethod.getResponseBodyAsStream());
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getDocumentMetadata
   * (javax.jcr.Node, java.lang.String)
   */
  public ExternalDocumentResultMetadata getDocumentMetadata(Node node, String path)
      throws DocProxyException {
    try {
      executeMethod(metadataMethod);
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#search(javax.jcr
   * .Node, java.util.Map)
   */
  public Iterator<ExternalDocumentResult> search(Node node,
      Map<String, Object> searchProperties) throws DocProxyException {
    try {
      executeMethod(searchMethod);
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#removeDocument
   * (javax.jcr.Node, java.lang.String)
   */
  public void removeDocument(Node node, String path) throws DocProxyException {
    try {
      executeMethod(removeMethod);
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getType()
   */
  public String getType() {
    return "url";
  }

  private HttpMethod parseUrlish(String urlish) {
    if (urlish == null || urlish.length() == 0) {
      return null;
    }

    int pipePos = urlish.indexOf("|");
    int colonPos = urlish.indexOf(":");

    String method = "GET";
    String url = null;
    if (pipePos < colonPos) {
      method = urlish.substring(0, pipePos);
      url = urlish.substring(pipePos + 1);
    } else {
      url = urlish;
    }

    return new HttpAnyMethod(method, url);
  }

  private int executeMethod(HttpMethod method) throws IOException, DocProxyException {
    HttpClient client = new HttpClient();
    int returnCode = client.executeMethod(method);
    if (returnCode < 200 || returnCode >= 300) {
      throw new DocProxyException(returnCode, "Error occurred while executing method ["
          + method.getName() + "]");
    }
    return returnCode;
  }

  protected ExternalDocumentResult parseSearch() {
    return null;
  }

  protected ExternalDocumentResult parseDocument(InputStream is) {
    return null;
  }
}
