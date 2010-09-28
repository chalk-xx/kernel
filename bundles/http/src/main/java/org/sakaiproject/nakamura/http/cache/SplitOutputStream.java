package org.sakaiproject.nakamura.http.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class SplitOutputStream extends ServletOutputStream {

  
  private ServletOutputStream baseStream;
  private ByteArrayOutputStream store;

  public SplitOutputStream(ServletOutputStream baseStream) {
    store = new ByteArrayOutputStream();
    this.baseStream = baseStream;
  }

  @Override
  public void write(int b) throws IOException {
    baseStream.write(b);
    store.write(b);
  }
  
  @Override
  public void flush() throws IOException {
    super.flush();
    baseStream.flush();
    store.flush();
  }
  
  @Override
  public void close() throws IOException {
    super.close();
    baseStream.flush();
  }

  public byte[] toByteArray() {
    return store.toByteArray();
  }

  
  

}
