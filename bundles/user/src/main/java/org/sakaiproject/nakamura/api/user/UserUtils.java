package org.sakaiproject.nakamura.api.user;

import static org.sakaiproject.nakamura.api.user.UserConstants.PREFERRED_NAME;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_COLLEGE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DATEOFBIRTH;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_DEPARTMENT;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_PICTURE;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_ROLE;

import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_DESCRIPTION_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.GROUP_TITLE_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_BASIC;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_EMAIL_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_FIRSTNAME_PROPERTY;
import static org.sakaiproject.nakamura.api.user.UserConstants.USER_LASTNAME_PROPERTY;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.User;

import java.util.HashMap;
import java.util.Map;

public class UserUtils {

  
  private final static String[] DEFAULT_BASIC_PROFILE_ELEMENTS = new String[] {USER_FIRSTNAME_PROPERTY, USER_LASTNAME_PROPERTY,
    USER_EMAIL_PROPERTY, USER_PICTURE, PREFERRED_NAME, USER_ROLE, USER_DEPARTMENT, USER_COLLEGE, USER_DATEOFBIRTH};


  private static String[] basicProfileElements = DEFAULT_BASIC_PROFILE_ELEMENTS;

  private static void addUserProperties(Authorizable user, ValueMap profileMap) {
    // Backward compatible reasons.
    profileMap.put("rep:userId", user.getId());
    profileMap.put("userid", user.getId());
    profileMap.put("hash", user.getId());
  }

  private static void addGroupProperties(Authorizable group, ValueMap profileMap) {
    // For a group we just dump it's title and description.
    profileMap.put("groupid", group.getId());
    profileMap.put("sakai:group-id", group.getId());
    profileMap.put(GROUP_TITLE_PROPERTY, group.getProperty(GROUP_TITLE_PROPERTY));
    profileMap.put(GROUP_DESCRIPTION_PROPERTY, group
        .getProperty(GROUP_DESCRIPTION_PROPERTY));
  }

  public static ValueMap getCompactProfile(Authorizable authorizable) {
    if (User.ANON_USER.equals(authorizable.getId())) {
      return anonymousProfile();
    }
    
    ValueMap compactProfile = new ValueMapDecorator(new HashMap<String, Object>());
    compactProfile.put(USER_BASIC, basicProfileMapForAuthorizable(authorizable));

    if (authorizable.isGroup()) {
      addGroupProperties(authorizable, compactProfile);
    } else {
      addUserProperties(authorizable, compactProfile);
    }
    return compactProfile;
  }
  
  private static ValueMap basicProfileMapForAuthorizable(Authorizable authorizable) {
    Builder<String, String> propertyBuilder = ImmutableMap.builder();
    for (String profileElementName : basicProfileElements ) {
      if (authorizable.hasProperty(profileElementName)) {
        propertyBuilder.put(profileElementName, (String)authorizable.getProperty(profileElementName));
      }
    }
    // The map were we will stick the compact information in.
    ValueMap compactProfile = basicProfile(propertyBuilder.build());
    if ( authorizable.hasProperty("access")) {
      compactProfile.put("access", authorizable.getProperty("access"));
    } else {
      compactProfile.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    }
    return compactProfile;
  }
  
  
  private static ValueMap basicProfile(Map<String, String> elementsMap) {
    ValueMap basic = new ValueMapDecorator(new HashMap<String, Object>());
    ValueMap elements = new ValueMapDecorator(new HashMap<String, Object>());
    for (String key : elementsMap.keySet()) {
      elements.put(key, new ValueMapDecorator(ImmutableMap.of("value", (Object) elementsMap.get(key))));
    }
    basic.put("elements", elements);
    return basic;
  }
  
  private static ValueMap anonymousProfile() {
    ValueMap rv = new ValueMapDecorator(new HashMap<String, Object>());
    rv.put("rep:userId", User.ANON_USER);
    ValueMap basicProfile =  basicProfile(
        ImmutableMap.of(USER_FIRSTNAME_PROPERTY, "Anonymous", USER_LASTNAME_PROPERTY, "User", USER_EMAIL_PROPERTY, "anon@sakai.invalid"));
    basicProfile.put(UserConstants.USER_BASIC_ACCESS, UserConstants.EVERYBODY_ACCESS_VALUE);
    rv.put(USER_BASIC,basicProfile);
    return rv;
  }

  public static String[] getBasicProfileElements() {
    return basicProfileElements;
  }

  public static String[] getDefaultBasicProfileElements() {
    return DEFAULT_BASIC_PROFILE_ELEMENTS;
  }

  public static void setBasicProfileElements(String[] newBasicProfileElements) {
    basicProfileElements = newBasicProfileElements;
  }



}
