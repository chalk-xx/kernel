package org.sakaiproject.nakamura.api.resource.lite;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class LiteJsonImporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(LiteJsonImporter.class);
  
  public void importContent(ContentManager contentManager, JSONObject json,
      String path, boolean continueIfExists, boolean replaceProperties) throws JSONException, StorageClientException, AccessDeniedException  {
    if ( !continueIfExists && contentManager.exists(path)) {
      LOGGER.debug("replace=false and path exists, so discontinuing JSON import: " + path);
      return;
    }
    internalImportContent(contentManager, json, path, replaceProperties);
  }
  public void internalImportContent(ContentManager contentManager, JSONObject json,
      String path, boolean replaceProperties) throws JSONException, StorageClientException, AccessDeniedException {
    Iterator<String> keys = json.keys();
    Map<String, Object> properties = new HashMap<String, Object>();
    while (keys.hasNext()) {

      String key = keys.next();
      if (!key.startsWith("jcr:")) {
        Object obj = json.get(key);

        if (obj instanceof JSONObject) {
          internalImportContent(contentManager, (JSONObject) obj, path + "/" + key, replaceProperties);
        } else if (obj instanceof JSONArray) {
          // This represents a multivalued property
          JSONArray arr = (JSONArray) obj;
          String[] values = new String[arr.length()];
          for (int i = 0; i < arr.length(); i++) {
            values[i] = arr.getString(i);
          }
          properties.put(key, values);
        } else {
          properties.put(key, obj);
        }
      }
    }
    Content content = contentManager.get(path);
    if (content == null) {
      contentManager.update(new Content(path, properties));
      LOGGER.info("Created Node {} {}",path,properties);
    } else {
      for (Entry<String, Object> e : properties.entrySet()) {
        if ( replaceProperties || !content.hasProperty(e.getKey())) {
          LOGGER.info("Updated Node {} {} {} ",new Object[]{path,e.getKey(), e.getValue()});
          content.setProperty(e.getKey(), e.getValue());
        }
      }
      contentManager.update(content);
    }
  }

}
