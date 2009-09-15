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

package org.sakaiproject.kernel.proxy;

import org.apache.commons.collections.ExtendedProperties;
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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.proxy.ProxyClientException;
import org.sakaiproject.kernel.api.proxy.ProxyClientService;
import org.sakaiproject.kernel.api.proxy.ProxyMethod;
import org.sakaiproject.kernel.api.proxy.ProxyNodeSource;
import org.sakaiproject.kernel.api.proxy.ProxyResponse;
import org.sakaiproject.kernel.proxy.velocity.JcrResourceLoader;
import org.sakaiproject.kernel.proxy.velocity.VelocityLogger;
import org.sakaiproject.kernel.util.JcrUtils;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;

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

  /**
   * Create resources used by this component.
   * 
   * @param ctx
   * @throws Exception
   */
  public void activate(ComponentContext ctx) throws Exception {
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
  public void deactivate(ComponentContext ctx) throws Exception {
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
      Map<String, String> input, InputStream requestInputStream,
      long requestContentLength, String requestContentType) throws ProxyClientException {
    try {
      bindNode(node);
    
      if (node != null && node.hasProperty(SAKAI_REQUEST_PROXY_ENDPOINT)) {

        VelocityContext context = new VelocityContext(input);

        // setup the post request
        String endpointURL = JcrUtils.getMultiValueString(node.getProperty(SAKAI_REQUEST_PROXY_ENDPOINT));
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
          method = new GetMethod(endpointURL);
          // redirects work automatically for get, options and head, but not for put and
          // post
          method.setFollowRedirects(true);
          break;
        case HEAD:
          method = new HeadMethod(endpointURL);
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
        // follow redirects, but dont auto process 401's and the like.
        // credentials should be provided
        method.setDoAuthentication(false);

        for (Entry<String, String> header : headers.entrySet()) {
          method.addRequestHeader(header.getKey(), header.getValue());
        }

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
