package org.sakaiproject.nakamura.proxy;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.proxy.ProxyPreProcessor;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Map;

@Service(ProxyPreProcessor.class)
@Component(name = "org.sakaiproject.nakamura.proxy.SlideshareProxyPreProcessor", label = "ProxyPreProcessor for Slideshare", description = "Pre processor for Slideshare.", immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai foundation"),
    @Property(name = "service.description", value = "Pre processor that adds the api-key and hash to the template params.") })
public class SlideshareProxyPreProcessor implements ProxyPreProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SlideshareProxyPreProcessor.class);

  @Property(value = "")
  static final String slideshareApiKey = "slideshare.apiKey";

  @Property(value = "")
  static final String slideshareSharedSecret = "slideshare.sharedSecret";

  private static String APIKEY = "";
  private static String SHAREDSECRET = "";

  public void preProcessRequest(SlingHttpServletRequest request,
      Map<String, String> headers, Map<String, Object> templateParams) {

    Long ts = System.currentTimeMillis() / 1000;
    try {
      String hash = StringUtils.sha1Hash(SHAREDSECRET + ts);
      String keyHash = "api_key=" + APIKEY + "&ts=" + ts + "&hash=" + hash;
      templateParams.put("keyHash", keyHash);
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Hashing error", e);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("Hashing error", e);
    }
  }

  public String getName() {
    return "slideshare";
  }

  public void activate(ComponentContext context) {
    @SuppressWarnings("rawtypes")
    Dictionary props = context.getProperties();

    String _apiKey = PropertiesUtil.toString(props.get(slideshareApiKey), null);
    if (_apiKey != null) {
      if (diff(APIKEY, _apiKey)) {
        APIKEY = _apiKey;
      }
    } else {
      LOGGER.error("Slideshare API key not set.");
    }

    String _sharedSecret = PropertiesUtil.toString(props.get(slideshareSharedSecret), null);
    if (_sharedSecret != null) {
      if (diff(SHAREDSECRET, _sharedSecret)) {
        SHAREDSECRET = _sharedSecret;
      }
    } else {
      LOGGER.error("Slideshare shared secret not set.");
    }
  }

  /**
   * Determine if there is a difference between two objects.
   * 
   * @param obj1
   * @param obj2
   * @return true if the objects are different (only one is null or !obj1.equals(obj2)).
   *         false otherwise.
   */
  private boolean diff(Object obj1, Object obj2) {
    boolean diff = true;

    boolean bothNull = obj1 == null && obj2 == null;
    boolean neitherNull = obj1 != null && obj2 != null;

    if (bothNull || (neitherNull && obj1.equals(obj2))) {
      diff = false;
    }
    return diff;
  }
}
