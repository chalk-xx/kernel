package org.sakaiproject.nakamura.http.cache;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class TFilter implements FilterChain {

  private boolean useOutputStream;
  
  public TFilter(boolean userOutputStream) {
    this.useOutputStream = userOutputStream;
  }

  public void doFilter(ServletRequest request, ServletResponse response)
      throws IOException, ServletException {
    HttpServletResponse sresponse = (HttpServletResponse) response;
    sresponse.addDateHeader("Date", System.currentTimeMillis());
    sresponse.setDateHeader("Last-Modified", System.currentTimeMillis());
    sresponse.setCharacterEncoding("URF-8");
    sresponse.setContentLength(10);
    sresponse.setContentType("test/plain");
    sresponse.setHeader("Cache-Control", "max-age=3600");
    sresponse.setIntHeader("Age", 1000);
    sresponse.setLocale(new Locale("en","GB"));
    sresponse.setStatus(200);
    sresponse.setStatus(200, "Ok");
    sresponse.addHeader("Cache-Control", " public");
    sresponse.addIntHeader("Age", 101);
    if ( useOutputStream ) {
      sresponse.getOutputStream().write(new byte[1024]);
    } else {
      sresponse.getWriter().write("ABCDEF");
          
    }
  }

}
