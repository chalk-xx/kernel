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
package org.sakaiproject.kernel.doc;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;

/**
 *
 */
public class ServletDocumentation implements Comparable<ServletDocumentation> {

  private String bundleName;
  private ServiceDocumentation serviceDocumetation;
  private String serviceName;
  private Servlet service;
  private ServiceReference reference;

  /**
   * @param reference
   * @param service
   */
  public ServletDocumentation(ServiceReference reference, Servlet service) {
    this.service = service;
    this.reference = reference;
    bundleName = getBundleName(reference);
    serviceDocumetation = (ServiceDocumentation) service.getClass().getAnnotation(
        ServiceDocumentation.class);
    serviceName = getServiceName(reference, service, serviceDocumetation);

  }

  /**
   * @param reference2
   * @return
   */
  private static String getBundleName(ServiceReference reference) {
    Bundle bundle = reference.getBundle();
    String bundleName = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
    if (bundleName == null) {
      bundleName = bundle.getSymbolicName();
      if (bundleName == null) {
        bundleName = bundle.getLocation();
      }
    }
    return bundleName;
  }

  /**
   * @param reference2
   * @param service2
   * @param serviceDocumetation2
   * @return
   */
  private static String getServiceName(ServiceReference reference, Servlet service,
      ServiceDocumentation serviceDocumetation) {
    String name = service.getClass().getName();
    int i = name.lastIndexOf('.');
    name = i > 0 ? name.substring(i + 1) : name;
    String bundleName = getBundleName(reference);
    if (serviceDocumetation == null) {
      return bundleName + ":" + name + ":"
          + String.valueOf(reference.getProperty(Constants.SERVICE_ID));
    } else {
      return bundleName + ":" + name + ":" + serviceDocumetation.name();
    }
  }

  /**
   * @param request
   * @param response
   * @throws IOException
   */
  public void send(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws IOException {
    PrintWriter writer = response.getWriter();

    if (serviceDocumetation == null) {
      writer
          .append("<p>No Service documentation has been provided by the developer, tut tut!</p>");
    } else {
      writer.append("<h3>Description</h3><p>");
      for (String desc : serviceDocumetation.description()) {
        writer.append(desc);
      }

      writer.append("</p><h3>Bindings</h3><ul>");

      for (ServiceBinding sb : serviceDocumetation.bindings()) {
        writer.append("<li>Type:");
        writer.append(sb.type().toString());
        writer.append("<ul>");
        for (String binding : sb.bindings()) {
          writer.append("<li>");
          writer.append(binding);
          writer.append("</li>");
        }
        writer.append("</ul></li>");

      }
      writer.append("</ul><h3>Methods</h3><ul>");
      for (ServiceMethod sm : serviceDocumetation.methods()) {
        writer.append("<li><h4>Method:");
        writer.append(sm.name());
        writer.append("</h4><p>");
        for (String desc : sm.description()) {
          writer.append(desc);
        }
        writer.append("</p><h4>Parameters</h4><ul>");
        for (ServiceParameter sp : sm.parameters()) {
          writer.append("<li><ul><li>");
          writer.append(sp.name());
          writer.append("</li><li>");
          for (String desc : sp.description()) {
            writer.append(desc);
          }
          writer.append("</li></ul></li>");
        }
        writer.append("</ul></li>");
      }
      writer.append("</ul>");
    }

    writer.append("<h2>Service Properties</h2><ul>");
    for (String k : reference.getPropertyKeys()) {
      writer.append("<li>");
      writer.append(k);
      writer.append(": ");
      writer.append(String.valueOf(reference.getProperty(k)));
      writer.append("</li>");

    }
    writer.append("</ul><h2>Other Services</h2><ul>");
    for (ServiceReference k : reference.getBundle().getRegisteredServices()) {
      Object service = k.getBundle().getBundleContext().getService(k);
      if (service instanceof Servlet) {
        ServletDocumentation sd = new ServletDocumentation(k, (Servlet) service);
        writer.append("<li><a href=\"");
        writer.append("?p=");
        writer.append(sd.getKey());
        writer.append("\">");
        writer.append(sd.getName());
        writer.append("</a>");
        writer.append("</li>");
      } else {
        writer.append("<li>");
        writer.append(service.getClass().getName());
        writer.append("</li>");

      }

    }
    writer.append("</ul>");

  }

  /**
   * @return
   */
  public String getName() {
    return serviceName;
  }

  /**
   * @return
   */
  public String getShortDescription() {
    if (serviceDocumetation == null) {
      return "<p>No documentation available</p>";
    } else {
      return serviceDocumetation.shortDescription();
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServletDocumentation) {
      return getName().equals(((ServletDocumentation) obj).getName());
    }
    return super.equals(obj);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(ServletDocumentation o) {
    return getName().compareTo(o.getName());
  }

  /**
   * @return
   */
  public String getKey() {
    return String.valueOf(service.getClass().getName());
  }

}
