package org.sakaiproject.kernel2.osgi.jpaprovider;

import java.net.URL;
import java.util.Enumeration;

public class UrlEnumeration implements Enumeration<URL> {

  private URL url;

  UrlEnumeration(URL url) {
    this.url = url;
  }

  public boolean hasMoreElements() {
    return url != null;
  }

  public URL nextElement() {
    URL url2 = url;
    url = null;
    return url2;
  }

}
