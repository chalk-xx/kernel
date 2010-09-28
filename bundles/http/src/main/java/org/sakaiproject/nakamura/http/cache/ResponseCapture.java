package org.sakaiproject.nakamura.http.cache;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;

public class ResponseCapture {

  public static final int MARKER = 0xff;
  public static final int END_OF_MARKER = 0xfe;
  public static final int ADD_INT_HEADER = 0x01;
  public static final int ADD_HEADER = 0x02;
  public static final int SET_STATUS = 0x03;
  public static final int SET_STATUS_WITH_MESSAGE = 0x04;
  public static final int SET_INT_HEADER = 0x05;
  public static final int SET_HEADER = 0x06;
  public static final int SET_LOCALE = 0x07;
  public static final int SET_CONTENT_TYPE = 0x08;
  public static final int SET_CONTENT_LENGTH = 0x09;
  public static final int SET_CHARACTER_ENCODING = 0x0A;
  public static final int ADD_DATE_HEADER = 0x0B;
  public static final int SET_DATE_HEADER = 0x0C;
  private PrintWriter writer;
  private SplitOutputStream outputStream;
  private boolean cacheable;
  private ByteArrayOutputStream rawBuffer;
  private DataOutputStream redoLog;
  private SplitWriter splitWriter;

  public ResponseCapture() {
    cacheable = true;
    resetRedoLog();
  }


  public PrintWriter getWriter(PrintWriter baseWriter) {
    if (outputStream != null) {
      throw new IllegalStateException();
    }
    if (writer == null) {
      splitWriter = new SplitWriter(baseWriter);
      writer = new PrintWriter(splitWriter);
    }
    return writer;
  }

  public ServletOutputStream getOutputStream(ServletOutputStream baseStream) {
    if (writer != null) {
      throw new IllegalStateException();
    }
    if (outputStream == null) {
      outputStream = new SplitOutputStream(baseStream);
    }
    return outputStream;
  }

  public void sendRedirect(String location) {
    dropCache();
  }

  public void sendError(int sc, String msg) {
    dropCache();
  }

  public void setDateHeader(String name, long date) {
    appendLog(SET_DATE_HEADER, name, date);
  }

  public void addDateHeader(String name, long date) {
    appendLog(ADD_DATE_HEADER, name, date);
  }

  public void setCharacterEncoding(String charset) {
    appendLog(SET_CHARACTER_ENCODING, charset);
  }

  public void setContentLength(int len) {
    appendLog(SET_CONTENT_LENGTH, len);
  }

  public void setContentType(String type) {
    appendLog(SET_CONTENT_TYPE, type);
  }

  public void setLocale(Locale loc) {
    appendLog(SET_LOCALE, loc.getLanguage(), loc.getCountry());
  }

  public void setHeader(String name, String value) {
    appendLog(SET_HEADER, name, value);
  }

  public void setIntHeader(String name, int value) {
    appendLog(SET_INT_HEADER, name, value);
  }

  public void setStatus(int sc) {
    appendLog(SET_STATUS, sc);
  }


  public void setStatus(int sc, String sm) {
    appendLog(SET_STATUS_WITH_MESSAGE, sm, sc);
  }

  public void addHeader(String name, String value) {
    appendLog(ADD_HEADER, name, value);
  }


  public void addIntHeader(String name, int value) {
    appendLog(ADD_INT_HEADER, name, value);
  }

  private void appendLog(int op, int sc) {
    if (cacheable) {
      try {
        redoLog.writeByte(MARKER);
        redoLog.writeByte(op);
        redoLog.writeInt(sc);
      } catch (IOException e) {
        dropCache();
      }
    }
  }
  private void appendLog(int op, String value) {
    if (cacheable) {
      try {
        redoLog.writeByte(MARKER);
        redoLog.writeByte(op);
        redoLog.writeUTF(value);
      } catch (IOException e) {
        dropCache();
      }
    }
  }
  private void appendLog(int op, String name, String value) {
    if (cacheable) {
      try {
        redoLog.writeByte(MARKER);
        redoLog.writeByte(op);
        redoLog.writeUTF(name);
        redoLog.writeUTF(value);
      } catch (IOException e) {
        dropCache();
      }
    }
  }

  private void appendLog(int op, String name, int value) {
    if (cacheable) {
      try {
        redoLog.writeByte(MARKER);
        redoLog.writeByte(op);
        redoLog.writeUTF(name);
        redoLog.writeInt(value);
      } catch (IOException e) {
        dropCache();
      }
    }
  }

  private void appendLog(int op, String name, long value) {
    if (cacheable) {
      try {
        redoLog.writeByte(MARKER);
        redoLog.writeByte(op);
        redoLog.writeUTF(name);
        redoLog.writeLong(value);
      } catch (IOException e) {
        dropCache();
      }
    }
  }

  public void reset() {
    resetRedoLog();
  }

  public void resetBuffer() {
    dropCache();
  }

  public void sendError(int sc) {
    dropCache();
  }

  private void dropCache() {
    cacheable = false;
    resetRedoLog();
  }

  private void resetRedoLog() {
    rawBuffer = new ByteArrayOutputStream();
    redoLog = new DataOutputStream(rawBuffer);
  }

  public byte[] getRedoLog() throws IOException {
    redoLog.writeByte(END_OF_MARKER);
    redoLog.flush();
    return rawBuffer.toByteArray();
  }

  public byte[] getByteContent() throws IOException {
    if ( outputStream != null  ) {
      outputStream.flush();
      return outputStream.toByteArray();
    }
    return null;
  }

  public String getStringContent() {
    if ( writer != null ) {
      writer.flush();
      return splitWriter.getStringContent();
    }
    return null;
  }


}
