package org.sakaiproject.nakamura.docproxy.url;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.MetadataRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.DocumentRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.RemoveRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.SearchRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.UpdateRequestHandler;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Session;

@RunWith(MockitoJUnitRunner.class)
public class UrlDocumentRepositoryProcessorTest {

  @Mock
  private Node node;

  @Mock
  private Session session;

  @Mock
  private HttpClient httpClient;

  private LocalTestServer server;

  private UrlRepositoryProcessor processor;

  private DocumentRequestHandler docHandler;
  private MetadataRequestHandler metadataHandler;
  private RemoveRequestHandler removeHandler;
  private SearchRequestHandler searchHandler;
  private UpdateRequestHandler updateHandler;

  private String docPath = "myDoc.ext";
  private UrlDocumentResult docResult;

  @Before
  public void setUp() throws Exception {
    when(node.getSession()).thenReturn(session);
    when(session.getUserID()).thenReturn("ch1411");

    docResult = new UrlDocumentResult(docPath, "text/plain", docPath.length(), null);
    docHandler = new DocumentRequestHandler(docResult);
    metadataHandler = new MetadataRequestHandler(docResult);
    removeHandler = new RemoveRequestHandler();
    searchHandler = new SearchRequestHandler();
    updateHandler = new UpdateRequestHandler();

    // setup the local test server
    server = new LocalTestServer(null, null);
    server.register("/document", docHandler);
    server.register("/metadata", metadataHandler);
    server.register("/remove", removeHandler);
    server.register("/search", searchHandler);
    server.register("/update", updateHandler);
    server.start();

    // setup & activate the url doc processor
    String serverUrl = "http://" + server.getServiceHostName() + ":"
        + server.getServicePort();

    Properties props = new Properties();
    props.put(UrlRepositoryProcessor.DOCUMENT_URL, serverUrl + "/document?p=");
    props.put(UrlRepositoryProcessor.METADATA_URL, serverUrl + "/metadata?p=");
    props.put(UrlRepositoryProcessor.REMOVE_URL, serverUrl + "/remove?p=");
    props.put(UrlRepositoryProcessor.SEARCH_URL, serverUrl + "/search");
    props.put(UrlRepositoryProcessor.UPDATE_URL, serverUrl + "/update?p=");

    ComponentContext context = mock(ComponentContext.class);
    when(context.getProperties()).thenReturn(props);

    when(httpClient.executeMethod(Mockito.any(HttpMethod.class))).thenReturn(200);

    processor = new UrlRepositoryProcessor();
    processor.activate(context);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void getType() {
    assertEquals(processor.getType(), UrlRepositoryProcessor.TYPE);
  }

  @Test
  public void getDocument() throws Exception {
    ExternalDocumentResult doc = processor.getDocument(node, "myDoc.ext");
    assertEquals(docResult, doc);
  }

  @Test
  public void getDocumentMetadata() throws Exception {
    ExternalDocumentResultMetadata metadata = processor
        .getDocumentMetadata(node, docPath);
    assertEquals(docResult, metadata);
  }

  @Test
  public void removeDocument() throws Exception {
    processor.removeDocument(node, docPath);
  }

  @Test
  public void searchDocument() throws Exception {
    HashMap<String, Object> props = new HashMap<String, Object>();
    processor.search(node, props);
  }

  @Test
  public void updateDocument() throws Exception {
    HashMap<String, Object> props = new HashMap<String, Object>();
    String output = "Data for document";
    ByteArrayInputStream bais = new ByteArrayInputStream(output.getBytes("UTF-8"));
    long streamLength = output.length();
    processor.updateDocument(node, docPath, props, bais, streamLength);
  }
}
