/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.nakamura.proxy;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.proxy.ProxyClientException;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.api.proxy.ProxyMethod;
import org.sakaiproject.nakamura.api.proxy.ProxyNodeSource;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;
import org.sakaiproject.nakamura.proxy.velocity.JcrResourceLoader;
import org.sakaiproject.nakamura.proxy.velocity.VelocityLogger;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.StringUtils;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@Service
@Component(immediate = true)
public class ProxyClientServiceImpl implements ProxyClientService, ProxyNodeSource {

  /**
   * 
   */
  private static final String JCR_RESOURCE_LOADER_PATH = "jcr.resource.loader.";

  /**
   * Default content type of request bodies if none has been specified.
   */
  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  /**
   * The classname of the jcr resource loader class.
   */
  private static final String JCR_RESOURCE_LOADER_CLASS = JCR_RESOURCE_LOADER_PATH
      + "class";

  /**
   * The JCR resoruce loader prefix used in the velocity properties.
   */
  private static final String JCR_RESOURCE_LOADER = "jcr";

  /**
   * The shared velocity engine, which should cache all the templates. (need to sort out
   * how to invalidate).
   */
  private VelocityEngine velocityEngine;

  /**
   * A multi threaded connection manager to provide http connections with pooling.
   */
  private MultiThreadedHttpConnectionManager httpClientConnectionManager;

  /**
   * The http client for this component (multi threaded)
   */
  private HttpClient httpClient;

  /**
   * A Thread local holder to bind the resource being processed to this thread.
   */
  private ThreadLocal<Node> boundNode = new ThreadLocal<Node>();

  private Map<String, Object> configProperties;

  /**
   * Create resources used by this component.
   * 
   * @param ctx
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext ctx) throws Exception {
    if (ctx != null) {
      Dictionary<String, Object> props = ctx.getProperties();
      configProperties = new HashMap<String, Object>();
      for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
        String k = e.nextElement();
        configProperties.put(k, props.get(k));
      }
    } else {
      configProperties = new HashMap<String, Object>();
    }
    velocityEngine = new VelocityEngine();
    velocityEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new VelocityLogger(
        this.getClass()));

    velocityEngine.setProperty(VelocityEngine.RESOURCE_LOADER, JCR_RESOURCE_LOADER);
    velocityEngine.setProperty(JCR_RESOURCE_LOADER_CLASS, JcrResourceLoader.class
        .getName());
    ExtendedProperties configuration = new ExtendedProperties();
    configuration.addProperty(JCR_RESOURCE_LOADER_PATH
        + ProxyNodeSource.JCR_RESOURCE_LOADER_RESOURCE_SOURCE, this);
    velocityEngine.setExtendedProperties(configuration);
    velocityEngine.init();

    httpClientConnectionManager = new MultiThreadedHttpConnectionManager();
    HttpConnectionManagerParams params = new HttpConnectionManagerParams();
    // could set a whole load of connection properties
    httpClientConnectionManager.setParams(params);

    httpClient = new HttpClient(httpClientConnectionManager);
  }

  /**
   * Clean up resources used by this component
   * 
   * @param ctx
   * @throws Exception
   */
  protected void deactivate(ComponentContext ctx) throws Exception {
    httpClientConnectionManager.shutdown();
  }

  /**
   * Executes a HTTP call using a path in the JCR to point to a template and a map of
   * properties to populate that template with. An example might be a SOAP call.
   * 
   * <pre>
   * {http://www.w3.org/2001/12/soap-envelope}Envelope:{
   *  {http://www.w3.org/2001/12/soap-envelope}Body:{
   *   {http://www.example.org/stock}GetStockPriceResponse:{
   *    &gt;body:[       ]
   *    {http://www.example.org/stock}Price:{
   *     &gt;body:[34.5]
   *    }
   *   }
   *   &gt;body:[  ]
   *  }
   *  &gt;body:[   ]
   *  {http://www.w3.org/2001/12/soap-envelope}encodingStyle:[http://www.w3.org/2001/12/soap-encoding]
   * }
   * 
   * </pre>
   * 
   * @param resource
   *          the resource containing the proxy end point specification.
   * @param headers
   *          a map of headers to set int the request.
   * @param input
   *          a map of parameters for all templates (both url and body)
   * @param requestInputStream
   *          containing the request body (can be null if the call requires no body or the
   *          template will be used to generate the body)
   * @param requestContentLength
   *          if the requestImputStream is specified, the length specifies the lenght of
   *          the body.
   * @param requerstContentType
   *          the content type of the request, if null the node property
   *          sakai:proxy-request-content-type will be used.
   * @throws ProxyClientException
   */
  public ProxyResponse executeCall(Node node, Map<String, String> headers,
      Map<String, Object> input, InputStream requestInputStream,
      long requestContentLength, String requestContentType) throws ProxyClientException {
    try {
      bindNode(node);

      if (node != null && node.hasProperty(SAKAI_REQUEST_PROXY_ENDPOINT)) {

        VelocityContext context = new VelocityContext(input);

        // add in the config properties from the bundle overwriting everythign else.
        context.put("config", configProperties);

        // setup the post request
        String endpointURL = JcrUtils.getMultiValueString(node
            .getProperty(SAKAI_REQUEST_PROXY_ENDPOINT));
        Reader urlTemplateReader = new StringReader(endpointURL);
        StringWriter urlWriter = new StringWriter();
        velocityEngine.evaluate(context, urlWriter, "urlprocessing", urlTemplateReader);
        endpointURL = urlWriter.toString();

        ProxyMethod proxyMethod = ProxyMethod.GET;
        if (node.hasProperty(SAKAI_REQUEST_PROXY_METHOD)) {
          try {
            proxyMethod = ProxyMethod.valueOf(node
                .getProperty(SAKAI_REQUEST_PROXY_METHOD).getString());
          } catch (Exception e) {

          }
        }
        HttpMethod method = null;
        switch (proxyMethod) {
        case GET:
          if (node.hasProperty(SAKAI_LIMIT_GET_SIZE)) {
            long maxSize = node.getProperty(SAKAI_LIMIT_GET_SIZE).getLong();
            method = new HeadMethod(endpointURL);
            HttpMethodParams params = new HttpMethodParams(method.getParams());
            // make certain we reject the body of a head
            params.setBooleanParameter("http.protocol.reject-head-body", true);
            method.setParams(params);
            method.setFollowRedirects(true);
            populateMethod(method, node, headers);
            int result = httpClient.executeMethod(method);
            if (result == 200) {
              // Check if the content-length is smaller than the maximum (if any).
              Header contentLengthHeader = method.getResponseHeader("Content-Length");
              if (contentLengthHeader != null) {
                long length = Long.parseLong(contentLengthHeader.getValue());
                if (length > maxSize) {
                  return new ProxyResponseImpl(
                      HttpServletResponse.SC_PRECONDITION_FAILED, "Response too large",
                      method);
                }
              }
            } else {
              return new ProxyResponseImpl(result, method);
            }
          }
          method = new GetMethod(endpointURL);
          // redirects work automatically for get, options and head, but not for put and
          // post
          method.setFollowRedirects(true);
          break;
        case HEAD:
          method = new HeadMethod(endpointURL);
          HttpMethodParams params = new HttpMethodParams(method.getParams());
          // make certain we reject the body of a head
          params.setBooleanParameter("http.protocol.reject-head-body", true);
          method.setParams(params);
          // redirects work automatically for get, options and head, but not for put and
          // post
          method.setFollowRedirects(true);
          break;
        case OPTIONS:
          method = new OptionsMethod(endpointURL);
          // redirects work automatically for get, options and head, but not for put and
          // post
          method.setFollowRedirects(true);
          break;
        case POST:
          method = new PostMethod(endpointURL);
          break;
        case PUT:
          method = new PutMethod(endpointURL);
          break;
        default:
          method = new GetMethod(endpointURL);
          // redirects work automatically for get, options and head, but not for put and
          // post
          method.setFollowRedirects(true);

        }

        populateMethod(method, node, headers);

        if (requestInputStream == null && !node.hasProperty(SAKAI_PROXY_REQUEST_TEMPLATE)) {
          if (method instanceof PostMethod) {
            PostMethod postMethod = (PostMethod) method;
            for (Entry<String, Object> param : input.entrySet()) {
              Object v = param.getValue();
              if (v instanceof String[]) {
                for (String val : (String[]) v) {
                  postMethod.addParameter(param.getKey(), val);
                }
              } else {
                postMethod.addParameter(param.getKey(), String.valueOf(v));

              }
            }
          }
        } else {

          if (method instanceof EntityEnclosingMethod) {
            String contentType = requestContentType;
            if (contentType == null && node.hasProperty(SAKAI_REQUEST_CONTENT_TYPE)) {
              contentType = node.getProperty(SAKAI_REQUEST_CONTENT_TYPE).getString();

            }
            if (contentType == null) {
              contentType = APPLICATION_OCTET_STREAM;
            }
            EntityEnclosingMethod eemethod = (EntityEnclosingMethod) method;
            if (requestInputStream != null) {
              eemethod.setRequestEntity(new InputStreamRequestEntity(requestInputStream,
                  requestContentLength, contentType));
            } else {
              // build the request
              Template template = velocityEngine.getTemplate(node.getPath());
              StringWriter body = new StringWriter();
              template.merge(context, body);
              byte[] soapBodyContent = body.toString().getBytes("UTF-8");
              eemethod.setRequestEntity(new ByteArrayRequestEntity(soapBodyContent,
                  contentType));

            }
          }
        }

        int result = httpClient.executeMethod(method);
        if (result == 302 && method instanceof EntityEnclosingMethod) {
          // handle redirects on post and put
          String url = method.getResponseHeader("Location").getValue();
          method = new GetMethod(url);
          method.setFollowRedirects(true);
          method.setDoAuthentication(false);
          result = httpClient.executeMethod(method);
        }

        return new ProxyResponseImpl(result, method);
      }

    } catch (Exception e) {
      throw new ProxyClientException("The Proxy request specified by  " + node
          + " failed, cause follows:", e);
    } finally {
      unbindNode();
    }
    throw new ProxyClientException("The Proxy request specified by " + node
        + " does not contain a valid endpoint specification ");
  }

  /**
   * @param method
   * @throws RepositoryException
   */
  private void populateMethod(HttpMethod method, Node node, Map<String, String> headers)
      throws RepositoryException {
    // follow redirects, but dont auto process 401's and the like.
    // credentials should be provided
    method.setDoAuthentication(false);

    for (Entry<String, String> header : headers.entrySet()) {
      method.addRequestHeader(header.getKey(), header.getValue());
    }

    Value[] additionalHeaders = JcrUtils.getValues(node, SAKAI_PROXY_HEADER);
    for (Value v : additionalHeaders) {
      String header = v.getString();
      String[] keyVal = StringUtils.split(header, ':', 2);
      method.addRequestHeader(keyVal[0].trim(), keyVal[1].trim());
    }

  }

  public HttpConnectionManager getHttpConnectionManager() {
    return httpClientConnectionManager;
  }

  /**
   * 
   */
  private void unbindNode() {
    boundNode.set(null);
  }

  /**
   * @param resource
   */
  private void bindNode(Node resource) {
    boundNode.set(resource);
  }

  /**
   * {@inheritDoc}
   * 
   * @see au.edu.csu.sakai.integration.api.soapclient.ResourceSource#getResource()
   */
  public Node getNode() {
    return boundNode.get();
  }

}
