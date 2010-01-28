package org.sakaiproject.kernel.batch;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.sakaiproject.kernel.batch.parameters.ContainerRequestParameter;
import org.sakaiproject.kernel.batch.parameters.ParameterMap;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

public class RequestWrapper extends SlingHttpServletRequestWrapper {

  private RequestInfo requestInfo;
  private ParameterMap postParameterMap;
  private SlingHttpServletRequest request;

  public RequestWrapper(SlingHttpServletRequest request) {
    super(request);
    this.request = request;
  }

  public void setRequestInfo(RequestInfo requestInfo) {
    this.requestInfo = requestInfo;
  }

  private Hashtable<String, String[]> getParameters() {
    return requestInfo.getParameters();
  }

  // 
  // Sling Request parameters
  //

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestParameter(java.lang.String)
   */
  @Override
  public RequestParameter getRequestParameter(String name) {
    return getRequestParameterMapInternal().getValue(name);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestParameterMap()
   */
  @Override
  public RequestParameterMap getRequestParameterMap() {
    return getRequestParameterMapInternal();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestParameters(java.lang.String)
   */
  @Override
  public RequestParameter[] getRequestParameters(String name) {
    return getRequestParameterMapInternal().getValues(name);
  }

  private ParameterMap getRequestParameterMapInternal() {
    if (this.postParameterMap == null) {

      // SLING-152 Get parameters from the servlet Container
      ParameterMap parameters = new ParameterMap();
      getContainerParameters(parameters);

      // apply any form encoding (from '_charset_') in the parameter map
      // Util.fixEncoding(parameters);

      this.postParameterMap = parameters;
    }
    return this.postParameterMap;
  }

  private void getContainerParameters(ParameterMap parameters) {

    // SLING-508 Try to force servlet container to decode parameters
    // as ISO-8859-1 such that we can recode later
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "ISO-8859-1";
      try {
        this.setCharacterEncoding(encoding);
      } catch (UnsupportedEncodingException uee) {
        throw new RuntimeException(uee);
      }
    }

    final Map<?, ?> pMap = getParameterMap();
    for (Map.Entry<?, ?> entry : pMap.entrySet()) {

      final String name = (String) entry.getKey();
      final String[] values = (String[]) entry.getValue();

      for (int i = 0; i < values.length; i++) {
        parameters.addParameter(name, new ContainerRequestParameter(values[i],
            encoding));
      }

    }
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
    return (requestInfo.getMethod() == null) ? "GET" : requestInfo.getMethod();
  }

}
