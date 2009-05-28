package org.sakaiproject.kernel.site.servlet;

import org.apache.sling.api.request.RequestParameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class DummyRequestParameter implements RequestParameter {

  private String string;

  public DummyRequestParameter(String string) {
    this.string = string;
  }

  public byte[] get() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getFileName() {
    // TODO Auto-generated method stub
    return null;
  }

  public InputStream getInputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  public long getSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getString() {
    return string;
  }

  public String getString(String encoding) throws UnsupportedEncodingException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isFormField() {
    // TODO Auto-generated method stub
    return false;
  }

}
