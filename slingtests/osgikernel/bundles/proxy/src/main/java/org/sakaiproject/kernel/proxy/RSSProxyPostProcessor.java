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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.proxy.ProxyPostProcessor;
import org.sakaiproject.kernel.api.proxy.ProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Will check if the response we get from an RSS file is valid. It will do basic checks
 * such as checking if the Content-Length is < 10K and Content-Type is a valid type.
 */
@Service(value = ProxyPostProcessor.class)
@Component(name = "RSSProxyPostProcessor", label = "ProxyPostProcessor for RSS", description = "Post processor who checks if requests are valid RSS requests.", immediate = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai foundation"),
    @Property(name = "service.description", value = "Post processor who checks if requests are valid RSS requests.") })
public class RSSProxyPostProcessor implements ProxyPostProcessor {

  private XMLInputFactory xmlInputFactory;

  private static final int MAX_RSS_LENGTH = 10000000;
  public static final Logger logger = LoggerFactory
      .getLogger(RSSProxyPostProcessor.class);

  private List<String> contentTypes;

  protected void activate(ComponentContext ctxt) {
    xmlInputFactory = XMLInputFactory.newInstance();
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    
    contentTypes = new ArrayList<String>();
    contentTypes.add("application/rss+xml");
    contentTypes.add("application/rdf+xml");
    contentTypes.add("application/atom+xml");
    contentTypes.add("text/xml");
    contentTypes.add("application/xhtml+xml");
    contentTypes.add("application/xml");
    contentTypes.add("text/plain");
  }

  protected void deactivate(ComponentContext ctxt) {
    this.xmlInputFactory = null;

    contentTypes = null;
  }

  public String getName() {
    return "rss";
  }

  public static final Logger log = LoggerFactory.getLogger(RSSProxyPostProcessor.class);

  public void process(SlingHttpServletResponse response, ProxyResponse proxyResponse)
      throws IOException {
    Map<String, String[]> headers = proxyResponse.getResponseHeaders();

    // Check if the content-length is smaller than the maximum.
    String[] header = headers.get("Content-Length");
    // if (header == null) {
    // response.sendError(HttpServletResponse.SC_FORBIDDEN,
    // "No Content-Length header found, rejecting request.");
    // return;
    // }
    if (header != null) {
      int length = Integer.parseInt(header[0]);
      if (length > MAX_RSS_LENGTH) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "This RSS feed is too big. The maximum for a feed is: " + MAX_RSS_LENGTH);
        return;
      }
    }

    // Check if the Content-Type we get is valid.
    String contentType = headers.get("Content-Type")[0];
    if (header != null) {
      if (contentType.contains(";")) {
        contentType = contentType.substring(0, contentType.indexOf(';'));
      }
      if (!contentTypes.contains(contentType)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "This URL doesn't send a proper Content-Type back");
        return;
      }
    }

    boolean isValid = false;
    InputStream in = proxyResponse.getResponseBodyAsInputStream();
    InputStreamReader reader = new InputStreamReader(in);

    // XMLStreamWriter writer = null;
    XMLEventWriter writer = null;

    try {
      // XMLStreamReader streamReader = xmlInputFactory.createXMLStreamReader(reader);
      XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(reader);

      int i = 0;
      Map<String, Boolean> checkedElements = new HashMap<String, Boolean>();
      checkedElements.put("rss", false);
      checkedElements.put("channel", false);
      checkedElements.put("title", false);
      checkedElements.put("link", false);
      checkedElements.put("item", false);
      checkedElements.put("title", false);
      checkedElements.put("link", false);

      XMLOutputFactory outputFactory =  XMLOutputFactory.newInstance();
      //outputFactory.configureForRobustness();
      //outputFactory.setProperty(WstxOutputProperties.P_OUTPUT_INVALID_CHAR_HANDLER,
      //    new InvalidCharHandler.ReplacingHandler(' '));
      //outputFactory.setProperty(WstxOutputProperties.P_OUTPUT_FIX_CONTENT, true);

      // writer = outputFactory.createXMLStreamWriter(response.getOutputStream());
      writer = outputFactory.createXMLEventWriter(response.getOutputStream());

      while (eventReader.hasNext()) {
        XMLEvent e = eventReader.nextEvent();
        // Stream it to the user.
        // e.writeAsEncodedUnicode(response.getWriter());
        writer.add(e);

        if (!isValid) {
          if (e.getEventType() == XMLEvent.START_ELEMENT) {
            StartElement el = e.asStartElement();
            String name = el.getName().toString().toLowerCase();
            if (checkedElements.containsKey(name)) {
              checkedElements.put(name, true);
            }

            boolean all = true;
            for (String s : checkedElements.keySet()) {
              if (!checkedElements.get(s)) {
                all = false;
                break;
              }
            }
            if (all)
              isValid = true;

          }

          if (i > 100) {
            response.reset();
            response.sendError(500, "Invalid RSS.");
            break;
          }
          i++;
        }
      }

      if (!isValid) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid RSS file.");
        return;
      }
      for (Entry<String, String[]> h : proxyResponse.getResponseHeaders().entrySet()) {
        for (String v : h.getValue()) {
          response.setHeader(h.getKey(), v);
        }
      }
      int code = proxyResponse.getResultCode();
      response.setStatus(code);

    } catch (XMLStreamException e) {
      logger.warn("Exception reading RSS feed.");
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid RSS file.");
    } catch (Exception e) {
      logger.warn("Exception reading RSS feed.");
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid RSS file.");
    } finally {
      try {
        writer.close();
      } catch (XMLStreamException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      reader.close();
    }

  }
}
