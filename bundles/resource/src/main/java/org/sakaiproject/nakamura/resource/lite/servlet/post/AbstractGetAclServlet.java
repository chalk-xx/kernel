
package org.sakaiproject.nakamura.resource.lite.servlet.post;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletResponse;

public abstract class AbstractGetAclServlet extends SlingAllMethodsServlet {




  /**
   * 
   */
  private static final long serialVersionUID = 7209815168152490665L;


  protected void outputAcl(Map<String, Object> acl, ServletResponse response) throws JSONException, IOException {
    Map<String, Map<String, Set<String>>> aclMap = Maps.newLinkedHashMap();
    for (Entry<String, Object> ace : acl.entrySet()) {
      String principalKey = ace.getKey();
      String principal = AclModification.getPrincipal(principalKey);
      String type = AclModification.isGrant(principalKey) ? "granted" : "denied";
      Set<String> s = createSet(createMap(aclMap, principal), type);

      try {
        // Add permissions for all valid integer inputs. If string is invalid (not an
        // integer or value is too large) then skip
        for (Permission p : AclModification.listPermissions(StorageClientUtils.toInt(ace
            .getValue()))) {
          s.add(p.getName());
        }
      }
 catch (NumberFormatException e) {
      }
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    List<JSONObject> aclList = new ArrayList<JSONObject>();
    for (Entry<String, Map<String, Set<String>>> entry : aclMap.entrySet()) {
      String principalName = entry.getKey();
      Map<String, Set<String>> value = entry.getValue();

      JSONObject aceObject = new JSONObject();
      aceObject.put("principal", principalName);

      Set<String> grantedSet = value.get("granted");
      if (grantedSet != null) {
        aceObject.put("granted", grantedSet);
      }

      Set<String> deniedSet = value.get("denied");
      if (deniedSet != null) {
        aceObject.put("denied", deniedSet);
      }
      aclList.add(aceObject);
    }
    JSONObject jsonAclMap = new JSONObject(aclMap);
    for (JSONObject jsonObj : aclList) {
      jsonAclMap.put(jsonObj.getString("principal"), jsonObj);
    }
    jsonAclMap.write(response.getWriter());
  }

  private <K, V> Set<V> createSet(Map<K, Set<V>> map, K key) {
    Set<V> s = map.get(key);
    if (s != null) {
      return s;
    }
    s = Sets.newLinkedHashSet();
    map.put(key, s);
    return s;
  }

  private <K, K2, V> Map<K2, V> createMap(Map<K, Map<K2, V>> map, K key) {
    Map<K2, V> m = map.get(key);
    if (m != null) {
      return m;
    }
    m = Maps.newLinkedHashMap();
    map.put(key, m);
    return m;
  }


}
