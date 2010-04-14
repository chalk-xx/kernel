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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.proxy.ProxyPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Map;

/**
 * This pre processor adds a header to the proxy request that is picked up by the far end
 * to identify the users. The far end has to a) share the same shared token and b) have
 * something to decode the token. The class was originally designed to work with a
 * TrustedTokenLoginFilter for Sakai 2, but the handshake protocol is so simple it could
 * be used with any end point. There is one configuration item, the sharedSecret that must
 * match the far end. At the moment this component is configured to be a singleton service
 * but if this mechanism of authenticating proxies becomes wide spread we may want this
 * class to be come a service factory so that we can support many trust relationships.
 * 
 */
@Service(value = ProxyPreProcessor.class)
@Component(description = "Pre processor for proxy requests to a Sakai 2 Instance with a Trusted Token filter", metatype = true, immediate = true, label = "TrustedTokenProxyPreProcessor")
@Properties(value = {
    @Property(name = "service.description", value = { "Pre processor for proxy requests to Sakai 2 instance with a trusted token filter." }),
    @Property(name = "service.vendor", value = { "The Sakai Foundation" }) })
public class TrustedLoginTokenProxyPreProcessor implements ProxyPreProcessor {
  
  public static final String HASH_ALGORITHM = "SHA1";
  public static final String SECURE_TOKEN_HEADER_NAME = "X-SAKAI-TOKEN";
  public static final String TOKEN_SEPARATOR = ";";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TrustedLoginTokenProxyPreProcessor.class);

  @Property(name = "sharedSecret", description = "This is the secret shared between the target http endpoint")
  private String sharedSecret = "e2KS54H35j6vS5Z38nK40";

  @Property(name = "port", description = "This is the port where sakai2 runs on (default = 80).", intValue = 80)
  private int port;
  
  @Property(name = "hostname", description = "This is the hostname where sakai2 runs on.", value = {"localhost"})
  private String hostname;

  public String getName() {
    return "trusted-token";
  }

  public void preProcessRequest(SlingHttpServletRequest request,
      Map<String, String> headers, Map<String, Object> templateParams) {

    String user = request.getRemoteUser();

    String other = "" + System.currentTimeMillis();
    String hash = sharedSecret + TOKEN_SEPARATOR + user + TOKEN_SEPARATOR + other;
    try {
      hash = byteArrayToHexStr(MessageDigest.getInstance(HASH_ALGORITHM).digest(
          hash.getBytes("UTF-8")));

    } catch (NoSuchAlgorithmException e1) {
      LOGGER.error(HASH_ALGORITHM +" Algorithm does not exist on this JVM", e1);
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("UTF8 Encoding does not exist on the JVM (as if!)", e);
    }
    String full = hash + TOKEN_SEPARATOR + user + TOKEN_SEPARATOR + other;
    headers.put("X-SAKAI-TOKEN", full);

    templateParams.put("port", port);
    templateParams.put("hostname", hostname);
  }

  protected String byteArrayToHexStr(byte[] data) {
    char[] chars = new char[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      byte current = data[i];
      int hi = (current & 0xF0) >> 4;
      int lo = current & 0x0F;
      chars[2 * i] = (char) (hi < 10 ? ('0' + hi) : ('A' + hi - 10));
      chars[2 * i + 1] = (char) (lo < 10 ? ('0' + lo) : ('A' + lo - 10));
    }
    return new String(chars);
  }

  /**
   * When the bundle gets activated we retrieve the OSGi properties.
   *
   * @param context
   */
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext context) {
    // Get the properties from the console.
    Dictionary props = context.getProperties();
    if (props.get("sharedSecret") != null) {
      sharedSecret = props.get("sharedSecret").toString();
    }
    if (props.get("hostname") != null) {
      hostname = props.get("hostname").toString();
      LOGGER.info("Sakai 2 hostname: " + hostname);
    }
    if (props.get("port") != null) {
      try {
        port = Integer.parseInt(props.get("port").toString());
        LOGGER.info("Sakai 2 port: " + port);
      } catch (NumberFormatException e) {
        LOGGER.warn("Failed to cast the sakai 2 port from the properties.", e);
      }
    }
  }

}
