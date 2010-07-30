package org.sakaiproject.nakamura.util;

import org.apache.sling.api.request.RequestPathInfo;

import java.util.Arrays;

public class RequestPathInfoWrapper implements RequestPathInfo {

  private String suffix;
  private String selectorString;
  private String[] selectors;
  private String extension;
  private String resourcePath;
  private final static String[] NO_SELECTORS = new String[0];


  public RequestPathInfoWrapper(String pathToParse) {
    if (pathToParse == null) {
      pathToParse = "";
    }
    resourcePath = pathToParse;
    
    int last = resourcePath.lastIndexOf('/');
    int dot = -1;
    if ( last > 0 ) {
      dot = resourcePath.indexOf('.', last);
    } else {
      dot = resourcePath.indexOf('.');
    }

    if (dot > 0) {
      int extDot = resourcePath.lastIndexOf('.');
      if (extDot >= resourcePath.length() - 1) {
        selectorString = resourcePath.substring(dot + 1);
        selectors = StringUtils.split(selectorString, '.');
        extension = null;
      } else if (extDot > dot) {
        selectorString = resourcePath.substring(dot + 1, extDot);
        selectors = StringUtils.split(selectorString, '.');
        extension = resourcePath.substring(extDot + 1);
      } else {
        selectors = NO_SELECTORS;
        selectorString = null;
        extension = resourcePath.substring(dot + 1);
      }
      resourcePath = resourcePath.substring(0, dot);
    } else {
      selectors = NO_SELECTORS;
      selectorString = null;
      extension = null;
    }
    if ( selectorString != null && selectorString.length() == 0 ) {
      selectors = NO_SELECTORS;
      selectorString = null;
    }
  }
  public String getResourcePath() {
    return resourcePath;
  }

  public String getExtension() {
    return extension;
  }

  public String getSelectorString() {
    return selectorString;
  }

  public String[] getSelectors() {
    return selectors;
  }

  public String getSuffix() {
    return suffix;
  }

}
