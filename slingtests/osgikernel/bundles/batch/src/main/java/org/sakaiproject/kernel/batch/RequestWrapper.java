package org.sakaiproject.kernel.batch;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RequestWrapper extends HttpServletRequestWrapper {

  private RequestData requestData;

  public RequestWrapper(HttpServletRequest request) {
    super(request);
  }

  public void setRequestData(RequestData requestData) {
    this.requestData = requestData;
  }

  private Hashtable<String, String[]> getParameters() {
    return requestData.getParameters();
  }

  //
  // Default servlet getParameters
  //

  @Override
  public String getParameter(String name) {
    String[] param = getParameters().get(name);
    if (param != null && param.length > 0) {
      return param[0];
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map getParameterMap() {
    return getParameters();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Enumeration getParameterNames() {
    return getParameters().keys();
  }

  @Override
  public String[] getParameterValues(String name) {
    return getParameters().get(name);
  }
  
  @Override
  public String getMethod() {
    return (requestData.getMethod() == null) ? "GET" : requestData.getMethod();
  }

}
