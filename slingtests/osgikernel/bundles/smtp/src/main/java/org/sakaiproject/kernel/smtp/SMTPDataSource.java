package org.sakaiproject.kernel.smtp;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.activation.DataSource;

public class SMTPDataSource implements DataSource {

  private String contentType;
  private InputStream inputStream;

  public SMTPDataSource(Map<String,Object> properties, String body) {
    this.contentType = (String) properties.get("content-type");
    this.inputStream = IOUtils.toInputStream(body);
  }
  
  public String getContentType() {
    return contentType;
  }

  public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  public String getName() {
    return "SMTP stream";
  }

  public OutputStream getOutputStream() throws IOException {
    return null;
  }

}
