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

package org.sakaiproject.kernel.batch;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.util.Hashtable;
import java.util.Iterator;

public class RequestData {

  private String url;
  private String method;
  private Hashtable<String, String[]> parameters;

  public RequestData(String url, Hashtable<String, String[]> parameters) {
    setUrl(url);
    setParameters(parameters);
  }

  public RequestData() {
    setParameters(new Hashtable<String, String[]>());
  }

  public RequestData(JSONObject obj) throws JSONException {
    setUrl(obj.getString("url"));
    setMethod(obj.getString("method"));
    setParameters(new Hashtable<String, String[]>());

    if (obj.has("parameters")) {

      JSONObject data = obj.getJSONObject("parameters");

      Iterator<String> keys = data.keys();

      while (keys.hasNext()) {
        String k = keys.next();
        Object val = data.get(k);
        if (val instanceof JSONArray) {
          JSONArray arr = (JSONArray) val;
          String[] par = new String[arr.length()];
          for (int i = 0; i < arr.length(); i++) {
            par[i] = arr.getString(i);
          }
          getParameters().put(k, par);
        } else {
          String[] par = { val.toString() };
          getParameters().put(k, par);
        }
      }
    }

  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public void setParameters(Hashtable<String, String[]> parameters) {
    this.parameters = parameters;
  }

  public Hashtable<String, String[]> getParameters() {
    return parameters;
  }

  /**
   * @param method
   *          the method to set
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * @return the method
   */
  public String getMethod() {
    return method;
  }

}
