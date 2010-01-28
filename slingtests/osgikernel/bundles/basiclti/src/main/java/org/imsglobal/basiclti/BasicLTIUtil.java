/*
 * $URL: https://source.sakaiproject.org/svn/basiclti/trunk/basiclti-util/src/java/org/imsglobal/basiclti/BasicLTIUtil.java $
 * $Id: BasicLTIUtil.java 70091 2009-12-04 05:46:31Z csev@umich.edu $
 *
 * Copyright (c) 2008 IMS GLobal Learning Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.imsglobal.basiclti;

import static org.imsglobal.basiclti.BasicLTIConstants.CUSTOM_PREFIX;
import static org.imsglobal.basiclti.BasicLTIConstants.LTI_MESSAGE_TYPE;
import static org.imsglobal.basiclti.BasicLTIConstants.LTI_VERSION;
import static org.imsglobal.basiclti.BasicLTIConstants.TOOL_CONSUMER_INSTANCE_DESCRIPTION;
import static org.imsglobal.basiclti.BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID;
import static org.imsglobal.basiclti.BasicLTIConstants.TOOL_CONSUMER_INSTANCE_URL;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/* Leave out until we have JTidy 0.8 in the repository 
 import org.w3c.tidy.Tidy;
 import java.io.ByteArrayOutputStream;
 */

/**
 * Some Utility code for IMS Basic LTI
 * http://www.anyexample.com/programming/java
 * /java_simple_class_to_compute_sha_1_hash.xml
 */
public class BasicLTIUtil {

  // We use the built-in Java logger because this code needs to be very generic
  private static Logger M_log = Logger.getLogger(BasicLTIUtil.class.toString());

  /** To turn on really verbose debugging */
  private static boolean verbosePrint = false;

  public static final String BASICLTI_SUBMIT = "basiclti_submit";

  private static final Pattern CUSTOM_REGEX = Pattern.compile("[^A-Za-z0-9]");
  private static final String UNDERSCORE = "_";

  // Simple Debug Print Mechanism
  public static void dPrint(String str) {
    if (verbosePrint)
      System.out.println(str);
    M_log.fine(str);
  }

  public static String validateDescriptor(String descriptor) {
    if (descriptor == null)
      return null;
    if (descriptor.indexOf("<basic_lti_link") < 0)
      return null;

    Map<String, Object> tm = XMLMap.getFullMap(descriptor.trim());
    if (tm == null)
      return null;

    // We demand at least an endpoint
    String ltiSecureLaunch = XMLMap.getString(tm,
        "/basic_lti_link/secure_launch_url");
    // We demand at least an endpoint
    if (ltiSecureLaunch != null && ltiSecureLaunch.trim().length() > 0)
      return ltiSecureLaunch;
    String ltiLaunch = XMLMap.getString(tm, "/basic_lti_link/launch_url");
    if (ltiLaunch != null && ltiLaunch.trim().length() > 0)
      return ltiLaunch;
    return null;
  }

  /**
   * Any properties which are not well known (i.e. in
   * {@link BasicLTIConstants#validPropertyNames}) will be mapped to custom
   * properties per the specified semantics.
   * 
   * @param rawProperties
   *          A set of {@link Properties} that will be cleaned. Keys must be of
   *          type {@link String}.
   * @param blackList
   *          An array of {@link String}s which are considered unsafe to be
   *          included in launch data. Any matches will be removed from the
   *          return.
   * @return A cleansed version of {@link Properties}.
   */
  public static Properties cleanupProperties(final Properties rawProperties,
      final String[] blackList) {
    final Properties newProp = new Properties();
    for (Object okey : rawProperties.keySet()) {
      if (!(okey instanceof String)) { // not a String
        continue;
      }
      final String key = ((String) okey).trim();
      if (blackList != null) {
        boolean blackListed = false;
        for (String blackKey : blackList) {
          if (blackKey.equals(key)) {
            blackListed = true;
            break;
          }
        }
        if (blackListed) {
          continue;
        }
      }
      final String value = rawProperties.getProperty(key);
      if (value == null || "".equals(value)) {
        // remove null or empty values
        continue;
      }
      if (isSpecifiedPropertyName(key)) {
        // a well known property name
        newProp.setProperty(key, value);
      } else {
        // convert to a custom property name
        newProp.setProperty(adaptToCustomPropertyName(key), value);
      }
    }
    return newProp;
  }

  /**
   * Any properties which are not well known (i.e. in
   * {@link BasicLTIConstants#validPropertyNames}) will be mapped to custom
   * properties per the specified semantics.
   * 
   * @param rawProperties
   *          A set of {@link Properties} that will be cleaned. Keys must be of
   *          type {@link String}.
   * @return A cleansed version of {@link Properties}.
   */
  public static Properties cleanupProperties(Properties rawProperties) {
    return cleanupProperties(rawProperties, null);
  }

  /**
   * Checks to see if the passed propertyName is equal to one of the Strings
   * contained in {@link BasicLTIConstants#validPropertyNames}. String matching
   * is case sensitive.
   * 
   * @param propertyName
   * @return true if propertyName is equal to one of the Strings contained in
   *         {@link BasicLTIConstants#validPropertyNames} ; else return false.
   */
  public static boolean isSpecifiedPropertyName(final String propertyName) {
    boolean found = false;
    for (String key : BasicLTIConstants.validPropertyNames) {
      if (key.equals(propertyName)) {
        found = true;
        break;
      }
    }
    return found;
  }

  /**
   * A simple utility method which implements the specified semantics of custom
   * properties.
   * <p>
   * i.e. The parameter names are mapped to lower case and any character that is
   * neither a number nor letter in a parameter name is replaced with an
   * "underscore".
   * <p>
   * e.g. Review:Chapter=1.2.56 would map to custom_review_chapter=1.2.56.
   * 
   * @param propertyName
   * @return
   */
  public static String adaptToCustomPropertyName(final String propertyName) {
    if (propertyName == null || "".equals(propertyName)) {
      throw new IllegalArgumentException("propertyName cannot be null");
    }
    String customName = propertyName.toLowerCase();
    customName = CUSTOM_REGEX.matcher(customName).replaceAll(UNDERSCORE);
    if (!customName.startsWith(CUSTOM_PREFIX)) {
      customName = CUSTOM_PREFIX + customName;
    }
    return customName;
  }

  // Add the necessary fields and sign
  public static Properties signProperties(Properties postProp, String url,
      String method, String oauth_consumer_key, String oauth_consumer_secret,
      String org_id, String org_desc, String org_url) {
    postProp = BasicLTIUtil.cleanupProperties(postProp);
    postProp.setProperty(LTI_VERSION, "LTI-1p0");
    postProp.setProperty(LTI_MESSAGE_TYPE, "basic-lti-launch-request");
    // Allow caller to internationalize this for us...
    if (postProp.getProperty(BASICLTI_SUBMIT) == null) {
      postProp.setProperty(BASICLTI_SUBMIT,
          "Launch Endpoint with BasicLTI Data");
    }
    if (org_id != null)
      postProp.setProperty(TOOL_CONSUMER_INSTANCE_GUID, org_id);
    if (org_desc != null)
      postProp.setProperty(TOOL_CONSUMER_INSTANCE_DESCRIPTION, org_desc);
    if (org_url != null)
      postProp.setProperty(TOOL_CONSUMER_INSTANCE_URL, org_url);

    if (postProp.getProperty("oauth_callback") == null)
      postProp.setProperty("oauth_callback", "about:blank");

    if (oauth_consumer_key == null || oauth_consumer_secret == null) {
      dPrint("No signature generated in signProperties");
      return postProp;
    }

    OAuthMessage oam = new OAuthMessage(method, url, postProp.entrySet());
    OAuthConsumer cons = new OAuthConsumer("about:blank", oauth_consumer_key,
        oauth_consumer_secret, null);
    OAuthAccessor acc = new OAuthAccessor(cons);
    try {
      oam.addRequiredParameters(acc);
      // System.out.println("Base Message String\n"+OAuthSignatureMethod.getBaseString(oam)+"\n");

      List<Map.Entry<String, String>> params = oam.getParameters();

      Properties nextProp = new Properties();
      // Convert to Properties
      for (Map.Entry<String, String> e : params) {
        nextProp.setProperty(e.getKey(), e.getValue());
      }
      return nextProp;
    } catch (net.oauth.OAuthException e) {
      M_log.warning("BasicLTIUtil.signProperties OAuth Exception "
          + e.getMessage());
      throw new Error(e);
    } catch (java.io.IOException e) {
      M_log.warning("BasicLTIUtil.signProperties IO Exception "
          + e.getMessage());
      throw new Error(e);
    } catch (java.net.URISyntaxException e) {
      M_log.warning("BasicLTIUtil.signProperties URI Syntax Exception "
          + e.getMessage());
      throw new Error(e);
    }

  }

  // Create the HTML to render a POST form and then automatically submit it
  // Make sure to call cleanupProperties before signing
  public static String postLaunchHTML(Properties newMap, String endpoint,
      boolean debug) {
    if (endpoint == null)
      return null;
    StringBuilder text = new StringBuilder();
    text.append("<div id=\"ltiLaunchFormSubmitArea\">\n");
    text
        .append("<form action=\""
            + endpoint
            + "\" name=\"ltiLaunchForm\" id=\"ltiLaunchForm\" method=\"post\" encType=\"application/x-www-form-urlencoded\">\n");
    for (Object okey : newMap.keySet()) {
      if (!(okey instanceof String))
        continue;
      String key = (String) okey;
      if (key == null)
        continue;
      String value = newMap.getProperty(key);
      if (value == null)
        continue;
      // This will escape the contents pretty much - at least
      // we will be safe and not generate dangerous HTML
      key = htmlspecialchars(key);
      value = htmlspecialchars(value);
      if (key.equals(BASICLTI_SUBMIT)) {
        text.append("<input type=\"submit\" name=\"");
      } else {
        text.append("<input type=\"hidden\" name=\"");
      }
      text.append(key);
      text.append("\" value=\"");
      text.append(value);
      text.append("\"/>\n");
    }
    text.append("</form>\n" + "</div>\n");
    if (debug) {
      text.append("<pre>\n");
      text.append("<b>BasicLTI Endpoint</b>\n");
      text.append(endpoint);
      text.append("\n\n");
      text.append("<b>BasicLTI Parameters:</b>\n");
      for (Object okey : newMap.keySet()) {
        if (!(okey instanceof String))
          continue;
        String key = (String) okey;
        if (key == null)
          continue;
        String value = newMap.getProperty(key);
        if (value == null)
          continue;
        text.append(key);
        text.append("=");
        text.append(value);
        text.append("\n");
      }
      text.append("</pre>\n");
    } else {
      text
          .append(" <script language=\"javascript\"> \n"
              + "    document.getElementById(\"ltiLaunchFormSubmitArea\").style.display = \"none\";\n"
              + "    nei = document.createElement('input');\n"
              + "    nei.setAttribute('type', 'hidden');\n"
              + "    nei.setAttribute('name', '"
              + BASICLTI_SUBMIT
              + "');\n"
              + "    nei.setAttribute('value', '"
              + newMap.getProperty(BASICLTI_SUBMIT)
              + "');\n"
              + "    document.getElementById(\"ltiLaunchForm\").appendChild(nei);\n"
              + "    document.ltiLaunchForm.submit(); \n" + " </script> \n");
    }

    String htmltext = text.toString();
    return htmltext;
  }

  public static boolean parseDescriptor(Properties launch_info,
      Properties postProp, String descriptor) {
    Map<String, Object> tm = null;
    try {
      tm = XMLMap.getFullMap(descriptor.trim());
    } catch (Exception e) {
      M_log.warning("BasicLTIUtil exception parsing BasicLTI descriptor"
          + e.getMessage());
      e.printStackTrace();
      return false;
    }
    if (tm == null) {
      M_log.warning("Unable to parse XML in parseDescriptor");
      return false;
    }

    String launch_url = toNull(XMLMap.getString(tm,
        "/basic_lti_link/launch_url"));
    String secure_launch_url = toNull(XMLMap.getString(tm,
        "/basic_lti_link/secure_launch_url"));
    if (launch_url == null && secure_launch_url == null)
      return false;

    setProperty(launch_info, "launch_url", launch_url);
    setProperty(launch_info, "secure_launch_url", secure_launch_url);

    // Extensions for hand-authored placements - The export process should scrub
    // these
    setProperty(launch_info, "key", toNull(XMLMap.getString(tm,
        "/basic_lti_link/x-secure/launch_key")));
    setProperty(launch_info, "secret", toNull(XMLMap.getString(tm,
        "/basic_lti_link/x-secure/launch_secret")));

    List<Map<String, Object>> theList = XMLMap.getList(tm,
        "/basic_lti_link/custom/parameter");
    for (Map<String, Object> setting : theList) {
      dPrint("Setting=" + setting);
      String key = XMLMap.getString(setting, "/!key"); // Get the key atribute
      String value = XMLMap.getString(setting, "/"); // Get the value
      if (key == null || value == null)
        continue;
      key = "custom_" + mapKeyName(key);
      dPrint("key=" + key + " val=" + value);
      postProp.setProperty(key, value);
    }
    return true;
  }

  // Remove fields that should not be exported
  public static String prepareForExport(String descriptor) {
    Map<String, Object> tm = null;
    try {
      tm = XMLMap.getFullMap(descriptor.trim());
    } catch (Exception e) {
      M_log.warning("BasicLTIUtil exception parsing BasicLTI descriptor"
          + e.getMessage());
      e.printStackTrace();
      return null;
    }
    if (tm == null) {
      M_log.warning("Unable to parse XML in prepareForExport");
      return null;
    }
    XMLMap.removeSubMap(tm, "/basic_lti_link/x-secure");
    String retval = XMLMap.getXML(tm, true);
    return retval;
  }

  /**
   * The parameter name is mapped to lower case and any character that is
   * neither a number or letter is replaced with an "underscore". So if a custom
   * entry was as follows:
   * 
   * <parameter name="Vendor:Chapter">1.2.56</parameter>
   * 
   * Would map to: custom_vendor_chapter=1.2.56
   */
  public static String mapKeyName(String keyname) {
    StringBuffer sb = new StringBuffer();
    if (keyname == null)
      return null;
    keyname = keyname.trim();
    if (keyname.length() < 1)
      return null;
    for (int i = 0; i < keyname.length(); i++) {
      Character ch = Character.toLowerCase(keyname.charAt(i));
      if (Character.isLetter(ch) || Character.isDigit(ch)) {
        sb.append(ch);
      } else {
        sb.append('_');
      }
    }
    return sb.toString();
  }

  public static String toNull(String str) {
    if (str == null)
      return null;
    if (str.trim().length() < 1)
      return null;
    return str;
  }

  public static void setProperty(Properties props, String key, String value) {
    if (value == null)
      return;
    if (value.trim().length() < 1)
      return;
    props.setProperty(key, value);
  }

  // Basic utility to encode form text - handle the "safe cases"
  public static String htmlspecialchars(String input) {
    if (input == null)
      return null;
    String retval = input.replace("&", "&amp;");
    retval = retval.replace("\"", "&quot;");
    retval = retval.replace("<", "&lt;");
    retval = retval.replace(">", "&gt;");
    retval = retval.replace(">", "&gt;");
    retval = retval.replace("=", "&#61;");
    return retval;
  }

}

/*
 * Sample Descriptor
 * 
 * <?xml version="1.0" encoding="UTF-8"?> <basic_lti_link
 * xmlns="http://www.imsglobal.org/xsd/imsbasiclti_v1p0"
 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"> <title>generated by
 * tp+user</title> <description>generated by tp+user</description> <custom>
 * <parameter key="keyname">value</parameter> </custom> <extensions
 * platform="www.lms.com"> <parameter key="keyname">value</parameter>
 * </extensions> <launch_url>url to the basiclti launch URL</launch_url>
 * <secure_launch_url>url to the basiclti launch URL</secure_launch_url>
 * <icon>url to an icon for this tool (optional)</icon> <secure_icon>url to an
 * icon for this tool (optional)</secure_icon> <cartridge_icon
 * identifierref="BLTI001_Icon" /> <vendor> <code>vendor.com</code> <name>Vendor
 * Name</name> <description> This is a Grade Book that supports many column
 * types. </description> <contact> <email>support@vendor.com</email> </contact>
 * <url>http://www.vendor.com/product</url> </vendor> </basic_lti_link>
 */
