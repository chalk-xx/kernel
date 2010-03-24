import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

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

/**
 *
 */
public class GP {

  public static void main(String[] args) throws IOException, JSONException {
    fixPov();
  }

  /**
   * @throws JSONException
   * @throws IOException
   * 
   */
  private static void fixPov() throws JSONException, IOException {
    JSONObject o = JSONUtil.loadJson("model.json");
    for (Iterator<String> k = o.keys(); k.hasNext();) {
      String key = k.next();
      Object ox = o.get(key);
      if (ox instanceof JSONObject) {
        JSONObject req = (JSONObject) ox;
        if (req.has("sakai:pov")) {
          String pov = req.getString("sakai:pov");
          
          JSONArray a = new JSONArray();
          splitAndSet(a,pov);
          req.put("sakai:pov", a);
        }
      }
    }
    JSONUtil.saveJson(o, "model2.json");
  }

  /**
   * @param a
   * @param pov
   */
  private static void splitAndSet(JSONArray a, String pov) {
    String povs[] = pov.split("\\W+");
    if ( povs.length > 1 ) {
      for (String p : povs) {
        if (p.trim().length() > 0) {
          splitAndSet(a, p.trim());
        }
      } 
    } else if ( povs.length == 1){
      a.put(povs[0].trim().toLowerCase());
    }
    
  }

  /**
   * @throws JSONException
   * @throws IOException
   * 
   */
  private static void addField() throws IOException, JSONException {
    JSONObject o = JSONUtil.loadJson("requirements.json");
    JSONArray a = o.getJSONArray("requirements");
    for (int i = 0; i < a.length(); i++) {
      JSONObject req = (JSONObject) a.get(i);
      req.put("implementationPhase", "phase1");
    }
    JSONUtil.saveJson(o, "requirements2.json");
  }

}
