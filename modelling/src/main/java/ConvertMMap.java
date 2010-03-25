import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
public class ConvertMMap {

  /**
   * @param args
   * @throws IOException 
   * @throws SAXException 
   * @throws ParserConfigurationException 
   * @throws JSONException 
   */
  public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, JSONException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
    File f = new File("mmap.xml");
    Document doc = db.parse(f);
    NodeList nl = doc.getChildNodes();
    JSONObject jo = new JSONObject();
    JSONArray ja = new JSONArray();
    jo.put("items", ja);
    buildStructure(ja, "",nl);
    
    JSONUtil.saveJson(jo, "mmap.json");
  }

  /**
   * @param string
   * @param nl
   * @throws JSONException 
   */
  private static void buildStructure(JSONArray ja, String path, NodeList nl) throws JSONException {
    for ( int i = 0; i < nl.getLength(); i++ ) {
      Node n = nl.item(i);
      String localName = n.getNodeName();
      if ( "node".equals(localName) ) {
        NamedNodeMap nn = n.getAttributes();
        String nodeName = path+"."+safeDirName(nn.getNamedItem("TEXT"));
        JSONObject jo = new JSONObject();
        ja.put(jo);
        jo.put("id", nodeName);
        jo.put("label", nodeName);
        jo.put("type", "JohnUserMap");
        buildStructure(ja, nodeName,n.getChildNodes());
      } else {
        buildStructure(ja, path,n.getChildNodes());   
      }
    }
  }

  /**
   * @param namedItem
   * @return
   */
  private static String safeDirName(Node namedItem) {
    String nodeName = namedItem.getTextContent();
    nodeName = nodeName.replace(' ','-');
    nodeName = nodeName.replace('"','-');
    nodeName = nodeName.replace(';','-');
    nodeName = nodeName.replace(',','-');
    nodeName = nodeName.replace('\'','-');
    nodeName = nodeName.replace('(','-');
    nodeName = nodeName.replace(')','-');
    nodeName = nodeName.replace('.','-');
    nodeName = nodeName.replace('/','-');
    return nodeName;
  }

}
