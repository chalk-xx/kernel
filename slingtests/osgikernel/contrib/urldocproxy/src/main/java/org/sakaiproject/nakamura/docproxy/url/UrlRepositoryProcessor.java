/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The SF licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.docproxy.url;

import com.ctc.wstx.stax.WstxInputFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;

/**
 * URL based document repository interactions.
 * 
 * @author <a href="mailto:carl@hallwaytech.com">Carl Hall</a>
 */
public class UrlRepositoryProcessor implements ExternalRepositoryProcessor {
  public static final String TYPE = "url";

  private static final String DAV_NS_URI = "DAV:";

  @Property(value = "X-USER")
  protected static final String USER_HEADER = "user.param";
  private String userHeader;

  @Property(value = "http://localhost/search/", description = "URL to use via GET for searching.")
  protected static final String SEARCH_URL = "search.url";
  private String searchUrl;

  @Property(value = "http://localhost/doc?p=", description = "URL to use via GET for retrieving a document.")
  protected static final String DOCUMENT_URL = "document.url";
  private String documentUrl;

  @Property(value = "http://localhost/metadata?p=", description = "URL to use via GET for retrieving metadata of a document.")
  protected static final String METADATA_URL = "metadata.url";
  private String metadataUrl;

  @Property(value = "http://localhost/update?p=", description = "URL to use via POST for updating a document.")
  protected static final String UPDATE_URL = "update.url";
  private String updateUrl;

  @Property(value = "http://localhost/doc?p=", description = "URL to use via DELETE for deleting a document.")
  protected static final String REMOVE_URL = "remove.url";
  private String removeUrl;

  private XMLInputFactory xmlInputFactory;

  private HttpClient client;

  /**
   * Default constructor. Used by OSGi.
   */
  public UrlRepositoryProcessor() {
    client = new HttpClient();
  }

  /**
   * Constructor with all required dependencies. Used in testing.
   * 
   * @param client HttpClient to use.  Usually a mock in this case.
   */
  protected UrlRepositoryProcessor(HttpClient client) {
    this.client = client;
  }

  public HttpClient getHttpClient() {
    return client;
  }

  @Activate
  protected void activate(ComponentContext context) {
    xmlInputFactory = new WstxInputFactory();
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);

    // process properties into http methods
    Dictionary props = context.getProperties();

    userHeader = (String) props.get(USER_HEADER);
    searchUrl = (String) props.get(SEARCH_URL);
    documentUrl = (String) props.get(DOCUMENT_URL);
    updateUrl = (String) props.get(UPDATE_URL);
    metadataUrl = (String) props.get(METADATA_URL);
    removeUrl = (String) props.get(REMOVE_URL);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getType()
   */
  public String getType() {
    return UrlRepositoryProcessor.TYPE;
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
    PostMethod method = new PostMethod(updateUrl + path);
    for (Entry<String, Object> entry : properties.entrySet()) {
      method.addParameter(entry.getKey(), entry.getValue().toString());
    }
    method.setRequestEntity(new InputStreamRequestEntity(documentStream));
    executeMethod(method, node);
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
    try {
      GetMethod method = new GetMethod(documentUrl + path);
      executeMethod(method, node);
      ExternalDocumentResult result = parseDocument(method.getResponseBodyAsStream());
      return result;
    } catch (XMLStreamException e) {
      throw new DocProxyException(500, e.getMessage());
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
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
      GetMethod method = new GetMethod(metadataUrl + path);
      executeMethod(method, node);
      ExternalDocumentResultMetadata result = parseDocument(method
          .getResponseBodyAsStream());
      return result;
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    } catch (XMLStreamException e) {
      throw new DocProxyException(500, e.getMessage());
    }
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
      PostMethod method = new PostMethod(searchUrl);
      for (Entry<String, Object> entry : searchProperties.entrySet()) {
        method.addParameter(entry.getKey(), entry.getValue().toString());
      }
      executeMethod(method, node);
      List<ExternalDocumentResult> results = parseSearch(method.getResponseBodyAsStream());
      return results.iterator();
    } catch (XMLStreamException e) {
      throw new DocProxyException(500, e.getMessage());
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#removeDocument
   * (javax.jcr.Node, java.lang.String)
   */
  public void removeDocument(Node node, String path) throws DocProxyException {
    DeleteMethod method = new DeleteMethod(removeUrl + path);
    executeMethod(method, node);
  }

  /**
   * Execute an http method and return the status code.
   * 
   * @param method
   * @return
   * @throws IOException
   * @throws DocProxyException
   */
  private int executeMethod(HttpMethod method, Node node) throws DocProxyException {
    try {
      String currentUserId = node.getSession().getUserID();
//      method.addRequestHeader(userHeader, currentUserId);
      int returnCode = client.executeMethod(method);
      if (returnCode < 200 || returnCode >= 300) {
        throw new DocProxyException(returnCode, "Error occurred while executing method ["
            + method.getName() + ":" + returnCode + "]");
      }
      return returnCode;
    } catch (IOException e) {
      throw new DocProxyException(500, e.getMessage());
    } catch (RepositoryException e) {
      throw new DocProxyException(500, e.getMessage());
    }
  }

  /**
   * Parse search results information into documents.
   * <p>
   * The expected format of the returned information is:<br/>
   * <code>
   * <search>
   *   <documents>
   *     <document contentLength="%s" contentType="%s" uri="%s">
   *       <properties>
   *         <key1>value1</key1>
   *         <key2>value2</key2>
   *       </properties>
   *     </document>
   *   </documents>
   * </search>
   * </code>
   * </p>
   * 
   * @param body
   * @return
   * @throws JSONException
   */
  protected List<ExternalDocumentResult> parseSearch(InputStream body)
      throws XMLStreamException {
    ArrayList<ExternalDocumentResult> results = new ArrayList<ExternalDocumentResult>();

    XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(body);
    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();
      // process the event if we're starting an element
      if (event.isStartElement()) {

        StartElement startEl = event.asStartElement();
        QName startElName = startEl.getName();
        String startElLocalName = startElName.getLocalPart();

        // process the href element
        if ("document".equalsIgnoreCase(startElLocalName)) {
          results.add(parseDocument(startEl, null));
          event = eventReader.nextEvent();
          continue;
        }
      }
    }

    return results;
  }

  /**
   * Parse information about a doc from JSON to an {@link ExternalDocumentResult} object.<br/>
   * XML is expected to follow the format of:
   * <p>
   * <code>
   * <document contentLength="%s" contentType="%s" uri="%s">
   *   <properties>
   *     <key1>value1</key1>
   *     <key2>value2</key2>
   *   </properties>
   * </document>
   * </code>
   * </p>
   * 
   * @param is
   * @return
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  protected ExternalDocumentResult parseDocument(InputStream body)
      throws XMLStreamException {
    UrlDocumentResult doc = new UrlDocumentResult();
    XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(body);
    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      // process the event if we're starting an element
      if (event.isStartElement()) {
        StartElement startEl = event.asStartElement();
        QName startElName = startEl.getName();
        String startElLocalName = startElName.getLocalPart();

        if ("document".equalsIgnoreCase(startElLocalName)) {
          parseDocument(startEl, doc);
          continue;
        }

        if ("properties".equalsIgnoreCase(startElLocalName)) {
          parseProperties(eventReader, doc);
          continue;
        }
      }
    }
    return doc;
  }

  /**
   * Parse the attributes of an XML 'document' element into a {@link UrlDocumentResult}.
   * 
   * @param startEl
   *          The 'document' element to process.
   * @param doc
   *          The document to set attributes into. If null, a new
   *          {@link UrlDocumentResult} is created.
   * @return A {@link UrlDocumentResult} containing values found in {@link startEl}. This
   *         will be the provided doc, if it is not null, or a newly created one.
   * @throws XMLStreamException
   *           If there is a problem processing the element.
   */
  private UrlDocumentResult parseDocument(StartElement startEl, UrlDocumentResult doc)
      throws XMLStreamException {

    UrlDocumentResult _doc = null;
    if (doc != null) {
      _doc = doc;
    } else {
      _doc = new UrlDocumentResult();
    }

    Iterator<Attribute> attrs = startEl.getAttributes();

    while (attrs.hasNext()) {
      Attribute attr = attrs.next();
      QName attrName = attr.getName();
      String attrLocalName = attrName.getLocalPart();

      // collect the uri
      if ("uri".equalsIgnoreCase(attrLocalName)) {
        _doc.setUri(attr.getValue());
        continue;
      }

      // collect the content length
      if ("contentLength".equalsIgnoreCase(attrLocalName)) {
        _doc.setContentLength(Long.parseLong(attr.getValue()));
        continue;
      }

      // collect the content length
      if ("contentType".equalsIgnoreCase(attrLocalName)) {
        _doc.setContentType(attr.getValue());
        continue;
      }
    }

    return _doc;
  }

  /**
   * Parse subelements of a 'properties' element into properties on a
   * {@link UrlDocumentResult}.
   * 
   * @param eventReader
   *          Source to process elements from. Is expected that calls to
   *          eventReader.nextEvent() will yield elements to be added to doc as
   *          properties. Nested elements are not considered.
   * @param doc
   *          The document to set attributes into.
   * @throws XMLStreamException
   *           If there is a problem processing the element.
   */
  private void parseProperties(XMLEventReader eventReader, UrlDocumentResult doc)
      throws XMLStreamException {
    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      // process the event if we're starting an element
      if (event.isStartElement()) {
        StartElement startEl = event.asStartElement();
        QName startElName = startEl.getName();
        String startElLocalName = startElName.getLocalPart();

        // collect simple elements as properties
        event = eventReader.nextEvent();
        if (event.isCharacters()) {
          Characters chars = event.asCharacters();
          doc.addProperty(startElLocalName, chars.getData());
        }
      }
    }
  }

  /**
   * Parse the results of PROPFIND on a resource in DAV into a
   * {@link ExternalDocumentResultMetadata}.
   * 
   * @param body
   * @return
   * @throws IOException
   */
  protected ExternalDocumentResultMetadata parseDavPropFind(InputStream body)
      throws IOException {
    try {
      boolean inProp = false;
      UrlDocumentResult doc = new UrlDocumentResult();
      XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(body);
      for (XMLEvent event = eventReader.nextEvent(); eventReader.hasNext(); event = eventReader
          .nextEvent()) {
        // process the event if we're starting an element
        if (event.isStartElement()) {

          StartElement startEl = event.asStartElement();
          QName startElName = startEl.getName();
          String startElLocalName = startElName.getLocalPart();

          // process the href element
          if ("href".equalsIgnoreCase(startElLocalName)) {
            event = eventReader.nextEvent();
            Characters chars = event.asCharacters();
            doc.setUri(chars.getData());
            continue;
          }

          // collect the content length
          if ("getcontentlength".equalsIgnoreCase(startElLocalName)) {
            event = eventReader.nextEvent();
            Characters chars = event.asCharacters();
            doc.setContentLength(Long.parseLong(chars.getData()));
            continue;
          }

          // collect the content type
          if ("getcontenttype".equalsIgnoreCase(startElLocalName)) {
            event = eventReader.nextEvent();
            Characters chars = event.asCharacters();
            doc.setContentType(chars.getData());
            continue;
          }

          // mark that we're in the props so we can collect the random ones
          if ("prop".equalsIgnoreCase(startElLocalName)) {
            inProp = true;
            continue;
          }

          // if in the prop tag, collect the properties we don't explicitly track
          if (inProp) {
            event = eventReader.nextEvent();
            Characters chars = event.asCharacters();

            // if part of
            if (DAV_NS_URI.equals(startElName.getNamespaceURI())) {
              doc.addProperty(startElLocalName, chars.getData());
            } else {

            }
          }
        } else if (event.isEndElement()) {
          EndElement endEl = event.asEndElement();
          if ("prop".equalsIgnoreCase(endEl.getName().getLocalPart())) {
            inProp = false;
          }
        }
      }
      return doc;
    } catch (XMLStreamException e) {
      e.printStackTrace();
    }
    return null;
  }
}
