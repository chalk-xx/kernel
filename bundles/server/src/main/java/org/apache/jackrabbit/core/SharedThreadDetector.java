package org.apache.jackrabbit.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Session;

public class SharedThreadDetector {

  private Exception bindingTrace;
  private Thread initThread;
  private int i;
  private Session session;
  
  public SharedThreadDetector(Session sesison) {
    initThread = Thread.currentThread();
    bindingTrace = new Exception("Bound At");
    this.session = sesison;
  }
  
  public void check(Session session) {
    Thread cthread = Thread.currentThread();
    if ( initThread != cthread ) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.append("Violation Bound to ").append(String.valueOf(initThread));
      pw.append(" called by ").append(String.valueOf(cthread)).append("\n");
      pw.append(" Session ").append(String.valueOf(this.session)).append("\n");
      if (session != this.session ){
        pw.append(" Calling Session Changed to ").append(String.valueOf(session)).append("\n");
        this.session = session;
      }
      bindingTrace.printStackTrace(pw);
      Exception e = new Exception("Rebound "+i+" at");
      e.printStackTrace(pw);
      i++;
      initThread = Thread.currentThread();
      bindingTrace = e;
      pw.flush();
      System.err.println(sw.toString());
    }
  }
  
}
