package org.sakaiproject.nakamura.user.lite.servlet;

import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_DESCRIPTION_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_TITLE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.PREFERRED_NAME;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_BASIC;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_COLLEGE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DATEOFBIRTH;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DEPARTMENT;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_EMAIL_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_FIRSTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_LASTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_PICTURE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_ROLE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.user.BasicUserInfoService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.Map;

@Component(immediate=true, metatype=true)
@Service(value=BasicUserInfoService.class)
public class BasicUserInfoServiceImpl implements BasicUserInfoService {



  @Property(value={USER_FIRSTNAME_PROPERTY, USER_LASTNAME_PROPERTY,
      USER_EMAIL_PROPERTY, USER_PICTURE, PREFERRED_NAME, USER_ROLE, USER_DEPARTMENT, USER_COLLEGE, USER_DATEOFBIRTH})
  public final static String BASIC_PROFILE_ELEMENTS = "basicUserInfoElements";

  private final static String[] DEFAULT_BASIC_USER_INFO_ELEMENTS = new String[] {USER_FIRSTNAME_PROPERTY, USER_LASTNAME_PROPERTY,
    USER_EMAIL_PROPERTY, USER_PICTURE, PREFERRED_NAME, USER_ROLE, USER_DEPARTMENT, USER_COLLEGE, USER_DATEOFBIRTH};


  private static String[] basicUserInfoElements = DEFAULT_BASIC_USER_INFO_ELEMENTS;


  @Activate
  protected void activated(Map<String, Object> properties ) {
    modified(properties);
  }

  @Modified
  protected void modified(Map<String, Object> properties ) {
    basicUserInfoElements = OsgiUtil.toStringArray(properties.get(BASIC_PROFILE_ELEMENTS), DEFAULT_BASIC_USER_INFO_ELEMENTS);
  }

  

  
  
  public Map<String, Object> getProperties(Authorizable authorizable) {
    if (authorizable == null || User.ANON_USER.equals(authorizable.getId())) {
      return anonymousBasicInfo();
    }
    
    Map<String, Object> compactProfile = Maps.newHashMap();
    compactProfile.put(USER_BASIC, basicProfileMapForAuthorizable(authorizable));

    if (authorizable.isGroup()) {
      addGroupProperties(authorizable, compactProfile);
    } else {
      addUserProperties(authorizable, compactProfile);
    }
    return compactProfile;
  }
  
  


  private void addUserProperties(Authorizable user, Map<String, Object> basicInfo) {
    // Backward compatible reasons.
    basicInfo.put("rep:userId", user.getId());
    basicInfo.put("userid", user.getId());
    basicInfo.put("hash", user.getId());
  }

  private void addGroupProperties(Authorizable group, Map<String, Object> basicInfo) {
    // For a group we just dump it's title and description.
    basicInfo.put("groupid", group.getId());
    basicInfo.put("sakai:group-id", group.getId());
    basicInfo.put(GROUP_TITLE_PROPERTY, group.getProperty(GROUP_TITLE_PROPERTY));
    basicInfo.put(GROUP_DESCRIPTION_PROPERTY, group
        .getProperty(GROUP_DESCRIPTION_PROPERTY));
  }

  private  Map<String, Object> basicProfileMapForAuthorizable(Authorizable authorizable) {
    Builder<String, String> propertyBuilder = ImmutableMap.builder();
    for (String basicUserInfoElementName : basicUserInfoElements ) {
      if (authorizable.hasProperty(basicUserInfoElementName)) {
        propertyBuilder.put(basicUserInfoElementName, (String)authorizable.getProperty(basicUserInfoElementName));
      }
    }
    // The map were we will stick the compact information in.
    Map<String, Object> basicInfo = basicInfo(propertyBuilder.build());
    if ( authorizable.hasProperty("access")) {
      basicInfo.put("access", authorizable.getProperty("access"));
    } else {
      basicInfo.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    }
    return basicInfo;
  }
  
  
  private  Map<String, Object> basicInfo(Map<String, String> elementsMap) {
    Map<String, Object> basic = Maps.newHashMap();
    Map<String, Object> elements = Maps.newHashMap();
    for (String key : elementsMap.keySet()) {
      elements.put(key, ImmutableMap.of("value", (Object) elementsMap.get(key)));
    }
    basic.put("elements", elements);
    return basic;
  }
  
  private Map<String, Object> anonymousBasicInfo() {
    Map<String, Object> rv = Maps.newHashMap();
    rv.put("rep:userId", User.ANON_USER);
    Map<String, Object> basicProfile =  basicInfo(
        ImmutableMap.of(USER_FIRSTNAME_PROPERTY, "Anonymous", USER_LASTNAME_PROPERTY, "User", USER_EMAIL_PROPERTY, "anon@sakai.invalid"));
    basicProfile.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    rv.put(USER_BASIC,basicProfile);
    return rv;
  }

  public String[] getBasicProfileElements() {
    return basicUserInfoElements;
  }



}
