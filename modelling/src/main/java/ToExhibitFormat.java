import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
public class ToExhibitFormat {

  public static void main(String[] argv) throws IOException, JSONException {
    JSONObject out = JSONUtil.loadJson("types.json");
    JSONObject in = JSONUtil.loadJson("model.json");
    JSONArray a = new JSONArray();
    if ( out.has("items") ) {
      a = (JSONArray) out.get("items");
    } else {
      out.put("items", a);
    }
    for ( Iterator<String> ik = in.keys(); ik.hasNext();  ) {
      String key = ik.next();
      Object jo = in.get(key);
      if ( jo instanceof JSONObject ) {
        ((JSONObject) jo).put("id",key);
        ((JSONObject) jo).put("label",key);
        ((JSONObject) jo).put("type","LearningCapability");
      }
      a.put(jo);
    }
    
    JSONObject requirements = JSONUtil.loadJson("requirements.json");
    JSONArray folders = (JSONArray) requirements.get("requirements");
    for ( int i = 0; i< folders.length(); i++ ) {
      JSONObject jo = (JSONObject) folders.get(i);
      String name = jo.getString("id");
      jo.put("requirementPath", name);
      int j = name.lastIndexOf('.');
      if ( j > 0 ) {
        String parent = name.substring(0,j);
        jo.put("parent", parent);
      }
      jo.put("label", name);
      jo.put("type", "RequirementPath");
      a.put(jo);
    }

    JSONObject johnUserMap = JSONUtil.loadJson("johnusermap.json");
    JSONArray johnUserItems = (JSONArray) johnUserMap.get("items");
    for ( int i = 0; i< johnUserItems.length(); i++ ) {
      JSONObject jo = (JSONObject) johnUserItems.get(i);
      String name = jo.getString("id");
      int j = name.lastIndexOf('.');
      if ( j > 0 ) {
        String parent = name.substring(0,j);
        jo.put("parent", parent);
      }
      a.put(jo);
    }

    JSONUtil.saveJson(out,"exhibit.json");

  }

}
