package org.sakaiproject.nakamura.http.cache;

import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;

/**
  A pojo to contain the response redo log and content.
 */
public class CachedResponse {

  private long expires;
  private byte[] redoLog;
  private byte[] byteContent;
  private String stringContent;

  public CachedResponse(ResponseCapture responseOperation, int cacheAge) throws IOException {
    expires = System.currentTimeMillis() + cacheAge*1000L;
    redoLog = responseOperation.getRedoLog();
    byteContent = responseOperation.getByteContent();
    stringContent = responseOperation.getStringContent();
  }

  public boolean isValid() {
    return expires > System.currentTimeMillis();
  }

  public void replay(SlingHttpServletResponse sresponse) throws IOException {
    ResponseReplay responseOperation = new ResponseReplay(redoLog, byteContent, stringContent);
    responseOperation.replay(sresponse);
  }
  
  @Override
  public String toString() {
    return "redo "+redoLog.length+" content "+String.valueOf(stringContent==null?byteContent.length:stringContent.length());
  }

}
