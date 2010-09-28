package org.sakaiproject.nakamura.http.cache;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class SplitWriter extends Writer {

  
  private PrintWriter baseWriter;
  private StringWriter store;

  public SplitWriter(PrintWriter baseWriter) {
    this.baseWriter = baseWriter;
    this.store = new StringWriter();
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    baseWriter.write(cbuf, off, len);
    store.write(cbuf,off,len);
  }

  @Override
  public void flush() throws IOException {
    baseWriter.flush();
    store.flush();
  }

  @Override
  public void close() throws IOException {
    baseWriter.close();
  }

  public String getStringContent() { 
    return store.toString();
  }

  
  

}
