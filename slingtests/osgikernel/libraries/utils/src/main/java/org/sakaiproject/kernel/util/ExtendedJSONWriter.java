package org.sakaiproject.kernel.util;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import java.io.Writer;
import java.util.Map.Entry;

public class ExtendedJSONWriter extends JSONWriter {

  public ExtendedJSONWriter(Writer w) {
    super(w);
  }

  public void valueMap(ValueMap valueMap) throws JSONException
  {
    object();
    for (Entry<String, Object> entry : valueMap.entrySet()) {
      key(entry.getKey());
      value(entry.getValue());
    }
    endObject();
  }
}
