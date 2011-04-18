package org.sakaiproject.nakamura.meservice;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public abstract class LiteAbstractMyGroupsServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = -8743012430930506449L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MyManagedGroupsServlet.class);

  public static final String PARAM_TEXT_TO_MATCH = "q";

  /**
  * This and related code attempts to mimic the usual client-server API for paged searching.
  *
  * TODO Replace the MyGroupsServlet and MyManagedGroupsServlet with search queries.
  * (This was not possible when the servlets were first written, but should be now.)
  */
  public static final String PARAMS_ITEMS_PER_PAGE = "items";
  /**
  *
  */
  public static final String PARAMS_PAGE = "page";
  /**
   * The default amount of items in a page.
   */
  public static final int DEFAULT_PAGED_ITEMS = 25;

  /**
  *
  */
  public static final String TOTAL = "total";
 /**
  *
  */
  public static final String JSON_RESULTS = "results";
  private static final Set<String> IGNORE_PROPERTIES = new HashSet<String>();
  private static final String[] IGNORE_PROPERTY_NAMES = new String[] {
    "members", "principals", "sakai:managers-group"
  };

  static {
    for ( String ignore : IGNORE_PROPERTY_NAMES) {
      IGNORE_PROPERTIES.add(ignore);
    }
  }



  protected transient ProfileService profileService;

  protected void bindProfileService(ProfileService profileService) {
    this.profileService = profileService;
  }
  protected void unbindProfileService(ProfileService profileService) {
    this.profileService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    javax.jcr.Session jcrSession = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
    Session session =
      StorageClientUtils.adaptToSession(jcrSession);
    String userId = session.getUserId();
    String requestedUserId = request.getParameter("uid");
    if ( requestedUserId != null && requestedUserId.length() > 0) {
      userId = requestedUserId;
    }
    try {
      // Find the Group entities associated with the user.
      AuthorizableManager am = session.getAuthorizableManager();
      Authorizable authorizable = am.findAuthorizable(userId);
      if ( authorizable == null ) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,"User "+userId+" not found.");
        return;
      }
      TreeMap<String, Group> groups = getGroups(authorizable, am);

      // Get the specified search query filter, if any.
      Pattern filterPattern = getFilterPattern(request.getParameter(PARAM_TEXT_TO_MATCH));

      // Filter the Profiles so as to set up proper paging.
      List<ValueMap> filteredProfiles = new ArrayList<ValueMap>();
      for (Group group : groups.values()) {
        ValueMap profile = profileService.getProfileMap(group, jcrSession);
        if (profile != null) {
          if ((filterPattern == null) || (isValueMapPattternMatch(profile, filterPattern))) {
            filteredProfiles.add(profile);
          }
        } else {
          LOGGER.info("No Profile found for group {}", group.getId());
        }
      }

      // Write out the Profiles.
      long nitems = longRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
          DEFAULT_PAGED_ITEMS);
      long page = longRequestParameter(request, PARAMS_PAGE, 0);
      long offset = page * nitems;

      List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(selectors.contains("tidy"));

      writer.object();
      writer.key(PARAMS_ITEMS_PER_PAGE);
      writer.value(nitems);
      writer.key(JSON_RESULTS);

      writer.array();
      int i = 0;
      for (ValueMap profile : filteredProfiles) {
        if ( i >= (offset + nitems) ) {
          break;
        } else if ( i >= offset ) {
          writer.valueMap(profile);
        }
        i++;
      }
      writer.endArray();

      writer.key(TOTAL);
      writer.value(filteredProfiles.size());

      writer.endObject();


    } catch (JSONException e) {
      LOGGER.error("Failed to write out groups for user " + userId, e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to build a proper JSON output.");
    } catch (StorageClientException e) {
      LOGGER.error("Failed to write out groups for user " + userId, e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Storage error.");
    } catch (AccessDeniedException e) {
      LOGGER.error("Failed to write out groups for user " + userId, e);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Access denied error.");
    } catch (RepositoryException e) {
      LOGGER.error("Failed to write out groups for user " + userId, e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
      "Storage error.");
    }
  }

  private long longRequestParameter(SlingHttpServletRequest request,
      String paramName, long defaultValue) {
    String p = request.getParameter(paramName);
    if ( p == null || p.trim().length() == 0 ) {
      return defaultValue;
    }
    try {
      return Long.valueOf(p);
    } catch ( Exception e) {
      return defaultValue;
    }
  }
  protected abstract TreeMap<String, Group> getGroups(Authorizable member, AuthorizableManager userManager) throws StorageClientException, AccessDeniedException;

  protected static Pattern getFilterPattern(String filterParameter) {
    Pattern filterPattern;
    if (filterParameter == null) {
      filterPattern = null;
    } else {
      // Translate Jackrabbit-style wildcards to Java-style wildcards.
      String translatedParameter = "(?i).*\\b" + filterParameter.replaceAll("\\*", ".*") + "\\b.*";
      filterPattern = Pattern.compile(translatedParameter);
    }
    return filterPattern;
  }

  private boolean isObjectPatternMatch(Object object, Pattern queryFilter) {
    boolean match = false;
    if (object instanceof ValueMap) {
      match =  isValueMapPattternMatch((ValueMap) object, queryFilter);
    } else if (object instanceof Object[]) {
      match = isArrayPatternMatch((Object[]) object, queryFilter);
    } else if (object != null) {
      match = isStringPatternMatch(object.toString(), queryFilter);
    }
    return match;
  }

  private boolean isArrayPatternMatch(Object[] array, Pattern queryFilter) {
    for (Object object : array) {
      if (isObjectPatternMatch(object, queryFilter)) {
        return true;
      }
    }
    return false;
  }

  private boolean isValueMapPattternMatch(ValueMap valueMap, Pattern queryFilter) {
    for (Entry<String, Object> entry : valueMap.entrySet()) {
      Object rawValue = entry.getValue();
      if ( !IGNORE_PROPERTIES.contains(entry.getKey())) {
        if (isObjectPatternMatch(rawValue, queryFilter)) {
          LOGGER.info("Matched Property {} {} ",entry.getKey(), rawValue);
          return true;
        }
      }
    }
    return false;
  }

  private boolean isStringPatternMatch(String stringValue, Pattern queryFilter) {
    return queryFilter.matcher(stringValue).matches();
  }

}