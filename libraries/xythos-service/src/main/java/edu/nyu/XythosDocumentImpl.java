package edu.nyu;

import java.util.Map;


public class XythosDocumentImpl implements XythosDocument {
  
  /**
   * 
   */
  private static final long serialVersionUID = -1731394605637101909L;
  private long contentLength;
  private String contentType;
  private byte[] documentContent;
  private Map<String, Object> properties;
  private String uri;
  
  public XythosDocumentImpl(long contentLength, String contentType, byte[] documentContent, Map<String, Object> properties, String uri) {
    this.contentLength = contentLength;
    this.contentType = contentType;
    this.documentContent = documentContent;
    this.properties = properties;
    this.uri = uri;
  }
  public XythosDocumentImpl() {
    super();
  }

  public long getContentLength() {
    return contentLength;
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getDocumentContent() {
    return documentContent;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public String getUri() {
    return uri;
  }

}
