package org.sakaiproject.nakamura.http.cache;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import javax.servlet.http.Cookie;

public class FilterResponseWrapper extends SlingHttpServletResponseWrapper {

  private boolean withCookies;
  private boolean withLastModified;

  public FilterResponseWrapper(SlingHttpServletResponse wrappedResponse, boolean withLastModfied, boolean withCookies) {
    super(wrappedResponse);
    this.withCookies = withCookies;
    this.withLastModified = withLastModfied;
  }
  
  @Override
  public void setDateHeader(String name, long date) {
    if ( withLastModified || !HttpConstants.HEADER_LAST_MODIFIED.equals(name)) {
      super.setDateHeader(name, date);
    }
  }
  
  @Override
  public void addCookie(Cookie cookie) {
    if ( withCookies ) {
      super.addCookie(cookie);
    }
  }

}
