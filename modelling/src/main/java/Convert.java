import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;

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
public class Convert {

  private static final String[] COLUMNS = { "LCID", "B Point Of View", "C Simple Goal",
      "D Alternative Goal", "E Capability", "F Capability+", "G More Complex example",
      "H Innovations", "I 1st Round Major Themes", "J Activity Flows", "K Categories",
      "L Chickering & Gamsen - T&L Good Practices", "M Comments" };

  private static final String[] FILES = { null, "B", "C", "D", "E", "F", "G" };

  /**
   * @param args
   * @throws IOException
   * @throws JSONException 
   */
  public static void main(String[] args) throws IOException, JSONException {
    
//    convert("eddmaster1","EddMaster1");
    convert("nyuedd","NYUviaEdd");
  }

  /**
   * @param string
   * @param string2
   * @throws IOException 
   * @throws JSONException 
   */
  private static void convert(String file, String type) throws IOException, JSONException {
    CSVReader reader = new CSVReader(new FileReader(file+".csv"));
    String[] nextLine;
    String[] headers = reader.readNext();
    JSONObject jo = new JSONObject();
    JSONArray a = new JSONArray();
    jo.put("items", a);
    for (int i = 0; i < headers.length; i++) {
      headers[i] = JSONUtil.safeId(headers[i]).toLowerCase();
    }
    while ((nextLine = reader.readNext()) != null) {
      JSONObject o = new JSONObject();
      for (int i = 0; i < nextLine.length && i < headers.length; i++) {
        o.put(headers[i], nextLine[i]);
      }
      o.put("type", type);
      o.put("label", o.getString("id"));
      JSONUtil.toArray(o,"sakai-pov");
      JSONUtil.toArray(o,"requirementpath");
      JSONArray ja = o.getJSONArray("requirementpath");
      o.remove("requirementpath");
      o.put("requirementPath", ja);
      
      
      a.put(o);
    }
    File f = new File(file+".json");
    FileWriter fw = new FileWriter(f);
    fw.append(jo.toString(4));
    fw.close();
  }

  /**
   * @param string
   * @param string2
   * @param string3
   * @param string4
   */
  private static void outputElement(String keyId, String keyValue, String headerName,
      String value, int column) {
    if (column < COLUMNS.length && COLUMNS[column] != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("<e id=\"").append(keyValue.trim()).append("-").append(column).append(
          "\"");
      sb.append(" name=\"").append(COLUMNS[column].trim()).append("\"");
      sb.append(" >");
      sb.append(value.trim());
      sb.append("</e>");
      System.out.println(sb.toString());
    }
  }

  private static void outputFile(String keyId, String keyValue, String pointOfView, String headerName,
      String value, int column) throws IOException, JSONException {
    if (column < FILES.length && FILES[column] != null && value.trim().length() != 0) {
      File f = new File("dataset/" + keyValue.trim() + "-" + FILES[column]+".json");
      f.getParentFile().mkdirs();
      JSONObject o = new JSONObject();
      o.put("sling:resourceType", "sakai/learningCapability");
      o.put("sakai:pov", pointOfView);
      o.put("sakai:id", keyValue.trim()+"_"+column);
      o.put("sakai:name",COLUMNS[column]);
      o.put("sakai:capability", value.trim());
      FileWriter fw = new FileWriter(f);
      fw.append(o.toString(4));
      fw.close();
    }
  }
  private static void outputFile(JSONObject jo, String keyId, String keyValue, String pointOfView, String headerName,
      String value, int column) throws IOException, JSONException {
    if (column < FILES.length && FILES[column] != null && value.trim().length() != 0) {
      String name = keyValue.trim() + "-" + FILES[column];
      JSONObject o = new JSONObject();
      o.put("sling:resourceType", "sakai/learningCapability");
      o.put("sakai:pov", pointOfView);
      o.put("sakai:id", keyValue.trim()+"_"+column);
      o.put("sakai:name",COLUMNS[column]);
      o.put("sakai:capability", value.trim());
      jo.put(name, o);
    }
  }

}
