package org.sakaiproject.nakamura.http.cache;

import org.apache.sling.api.SlingHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Locale;

public class ResponseReplay {

  private DataInputStream redoLog;
  private String stringContent;
  private byte[] byteContent;

  public ResponseReplay(byte[] redoLogBytes, byte[] byteContent, String stringContent) {
    redoLog = new DataInputStream(new ByteArrayInputStream(redoLogBytes));
    this.byteContent = byteContent;
    this.stringContent = stringContent;
  }

  /**
   * Replay the cached request
   * @param sresponse
   * @throws IOException
   */
  @SuppressWarnings("deprecation")
  public void replay(SlingHttpServletResponse sresponse) throws IOException {
    int op = redoLog.readUnsignedByte();
    while (op == ResponseCapture.MARKER) {
      op = redoLog.readUnsignedByte();
      String name, value;
      long d;
      int i;
      switch (op) {
      case ResponseCapture.ADD_DATE_HEADER:
        name = redoLog.readUTF();
        d = redoLog.readLong();
        sresponse.addDateHeader(name,d);
        break;
      case ResponseCapture.ADD_HEADER:
        name = redoLog.readUTF();
        value = redoLog.readUTF();
        sresponse.addHeader(name, value);
        break;
      case ResponseCapture.ADD_INT_HEADER:
        name = redoLog.readUTF();
        i = redoLog.readInt();
        sresponse.addIntHeader(name, i);
        break;
      case ResponseCapture.SET_CHARACTER_ENCODING:
        sresponse.setCharacterEncoding(redoLog.readUTF());
        break;
      case ResponseCapture.SET_CONTENT_LENGTH:
        sresponse.setContentLength(redoLog.readInt());
        break;
      case ResponseCapture.SET_CONTENT_TYPE:
        sresponse.setContentType(redoLog.readUTF());
        break;
      case ResponseCapture.SET_DATE_HEADER:
        name = redoLog.readUTF();
        d = redoLog.readLong();
        sresponse.setDateHeader(name,d);        
        break;
      case ResponseCapture.SET_HEADER:
        name = redoLog.readUTF();
        value = redoLog.readUTF();
        sresponse.setHeader(name, value);
        break;
      case ResponseCapture.SET_INT_HEADER:
        name = redoLog.readUTF();
        i = redoLog.readInt();
        sresponse.setIntHeader(name, i);
        break;
      case ResponseCapture.SET_LOCALE:
        String language = redoLog.readUTF();
        String country = redoLog.readUTF();
        sresponse.setLocale(new Locale(language,country));
        break;
      case ResponseCapture.SET_STATUS:
        i = redoLog.readInt();
        sresponse.setStatus(i);
        break;
      case ResponseCapture.SET_STATUS_WITH_MESSAGE:
        value = redoLog.readUTF();
        i = redoLog.readInt();
        sresponse.setStatus(i,value);
        break;
      default:
        op = redoLog.readUnsignedByte();
        while(op != ResponseCapture.MARKER && op != ResponseCapture.END_OF_MARKER ) {
          op = redoLog.readUnsignedByte();
        }
        continue;
      }
      op = redoLog.readUnsignedByte();
    }

     if ( stringContent != null  ) {
       sresponse.getWriter().write(stringContent);
     } else if ( byteContent != null ){
       sresponse.getOutputStream().write(byteContent);
     }

  }


}
