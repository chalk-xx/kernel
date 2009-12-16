package org.apache.sling.engine.impl.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.engine.impl.parameters.ParameterSupport;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

public class RequestWrapper extends SlingHttpServletRequestWrapper {

  private PostOperation operation;

  public RequestWrapper(SlingHttpServletRequest wrappedRequest) {
    super(wrappedRequest);
  }

  public void setPostOperation(PostOperation operation) {
    this.operation = operation;
  }

  private Hashtable<String, String[]> getParameters() {
    return operation.getParameters();
  }

  //
  // Sling RequestParameters
  //

  @Override
  public RequestParameter getRequestParameter(String name) {
    ParameterSupport parameterSupport = ParameterSupport.getInstance(this);
    return parameterSupport.getRequestParameter(name);
  }

  @Override
  public RequestParameterMap getRequestParameterMap() {
    ParameterSupport parameterSupport = ParameterSupport.getInstance(this);
    return parameterSupport.getRequestParameterMap();
  }

  @Override
  public RequestParameter[] getRequestParameters(String name) {
    ParameterSupport parameterSupport = ParameterSupport.getInstance(this);
    return parameterSupport.getRequestParameters(name);
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

  @Override
  public Map getParameterMap() {
    return getParameters();
  }

  @Override
  public Enumeration getParameterNames() {
    return getParameters().keys();
  }

  @Override
  public String[] getParameterValues(String name) {
    return getParameters().get(name);
  }

}
