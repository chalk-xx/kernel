package org.sakaiproject.nakamura.resource.lite.servlet.post.operations;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SimpleJsonImporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJsonImporter.class);
  
  public void importContent(ContentManager contentManager, JSONObject json,
      String path, SimpleImportOptions simpleImportOptions,
      SimpleContentImportListener simpleContentImportListener) throws JSONException, StorageClientException, AccessDeniedException  {
      internalImportContent(contentManager, json, path, simpleImportOptions, simpleContentImportListener, true);
  }
  public void internalImportContent(ContentManager contentManager, JSONObject json,
      String path, SimpleImportOptions simpleImportOptions,
      SimpleContentImportListener simpleContentImportListener, boolean topOfTree) throws JSONException, StorageClientException, AccessDeniedException {
    Iterator<String> keys = json.keys();
    Map<String, Object> properties = new HashMap<String, Object>();
    if ( topOfTree && simpleImportOptions.isOverwrite() ) {
      LOGGER.info("Deleting {} ",path);
      StorageClientUtils.deleteTree(contentManager, path);
      LOGGER.info("Done Deleting {} ",path);
    }
    while (keys.hasNext()) {

      String key = keys.next();
      if (!key.startsWith("jcr:")) {
        Object obj = json.get(key);

        if (obj instanceof JSONObject) {
          internalImportContent(contentManager, (JSONObject) obj, path + "/" + key, simpleImportOptions, simpleContentImportListener, false);
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
      if ( simpleContentImportListener != null) {
        simpleContentImportListener.onCreate(path);
      }
      LOGGER.info("Created Node {} {}",path,properties);
    } else {
      boolean replaceProperties = simpleImportOptions.isPropertyOverwrite();
      for (Entry<String, Object> e : properties.entrySet()) {
        if ( replaceProperties || !content.hasProperty(e.getKey())) {
          LOGGER.info("Updated Node {} {} {} ",new Object[]{path,e.getKey(), e.getValue()});
          content.setProperty(e.getKey(), e.getValue());
        }
      }
      contentManager.update(content);
      if ( simpleContentImportListener != null) {
        simpleContentImportListener.onModify(path);
      }
    }
  }

}
