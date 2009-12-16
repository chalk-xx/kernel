package org.apache.sling.engine.impl.batch;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;

public class ResponseWrapper extends SlingHttpServletResponseWrapper {

  public ResponseWrapper(SlingHttpServletResponse wrappedResponse) {
    super(wrappedResponse);
  }

  ByteArrayOutputStream boas = new ByteArrayOutputStream();
  ServletOutputStream servletOutputStream = new ServletOutputStream() {
    @Override
    public void write(int b) throws IOException {
      boas.write(b);
    }
  };
  PrintWriter pw = new PrintWriter(servletOutputStream);
  private String type;
  private String charset;

  @Override
  public String getCharacterEncoding() {
    return charset;
  }

  @Override
  public String getContentType() {
    return type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletResponseWrapper#flushBuffer()
   */
  @Override
  public void flushBuffer() throws IOException {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletResponseWrapper#getOutputStream()
   */
  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return servletOutputStream;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.ServletResponseWrapper#getWriter()
   */
  @Override
  public PrintWriter getWriter() throws IOException {
    return pw;
  }

  @Override
  public void setCharacterEncoding(String charset) {
    this.charset = charset;
  }

  @Override
  public void setContentType(String type) {
    this.type = type;
  }

  public String getDataAsString() throws UnsupportedEncodingException {
    pw.close();
    return boas.toString("utf-8");
  }

  @Override
  public void reset() {
  }

  @Override
  public void resetBuffer() {
  }
  
}
