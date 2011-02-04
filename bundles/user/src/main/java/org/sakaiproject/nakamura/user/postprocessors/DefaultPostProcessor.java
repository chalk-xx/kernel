package org.sakaiproject.nakamura.user.postprocessors;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_BARE_AUTHORIZABLE;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGED_GROUP;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGERS_GROUP;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.sf.json.JSONObject;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.LitePersonalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * 
 * <pre>
 * HomePostProcessor 
 *  It creates the following if they dont exist (or onCreate)
 *  a:userID
 *      - sling:resourceType = sakai/user-home | sakai/group-home
 *      - sakai:search-exclude-tree = copied from authorizable if present on creation only
 *      + permissions: if anon: anon:read + everyone:read
 *                     if visibility public anon:read everyone:read
 *                     if visibility logged in anong:denyall everyone:read
 *                     if visibility private anon:denyall everyone:denyall
 *                     all principals in the managers property allowed all
 *                     all principals in the viewers property allows read
 *                     current user allowed all
 *  a:userID/public
 *  a:userID/private
 *      + permissions: everyone and anon denied read
 *  
 *  If they do exist (on everything else except delete)
 *  a:userID
 *     Change permissions to match visibility
 *     Change permissions to match managers and viewers
 *     
 *  
 * Sakai Group Postprocessor
 * sets the path property in the authorizable
 * sets a group-manages property name to the name of an auto generated group
 * generates that group (does not trigger any post processors)
 * sets properties in the manger group
 * adds members and removes members according to the request properties sakai:manager and sakai:manager@Delete
 * 
 * Sakau User Post processor
 * sets the path property in the authorizable
 * 
 * 
 * Message Post Processor
 * a:userID/message
 *    - sling:resourceType = sakai/messagestore
 *    + permissions: user can all
 *                   anon deny all
 *                   everyone deny all
 *                   
 * Calendar
 * a:userID/calendar
 *     - sling:resourceType = sakai/calendar
 *     _ stores a default calendar (empty with no properties)
 *     + grants userID all
 *  
 * Connections   
 * a:userID/contacts
 *     - sling:resourceType = sakai/contactstore
 *     + deny all for anon and everyone
 *       grants user all, except anon
 *     + creates a private group of viewers that only the current user can view (could be delayed as not used then)
 *  
 *  Pages post processor
 *  a:userId/pages 
 *      Copies a template content tree verbatum from a uder defined location into the pages folder
 *      
 * 
 * Profile post Processor
 * a:userId/profile
 *     - sling:resourceType = sakai/group-profile | sakai/user-profile
 *      Copies a template of content posted by the UI to generate a tree of content after processing, uses the ContentLoader to achieve this.
 *      Copies all the Authorizable Properties onto the authorizable node.
 * 
 * 
 * 
 * -----------------
 * 
 * We can hard code everything other than the profile importer in a single class
 * IMO the manager group is superfluous on a user and adds unecessary expense
 * </pre>
 */
@Component(immediate = true, metatype = true)
@Service(value = LiteAuthorizablePostProcessor.class)
@Properties(value = { @Property(name = "default", value = "true") })
public class DefaultPostProcessor implements LiteAuthorizablePostProcessor {

  private static final String PAGES_FOLDER = "/pages";
  
  private static final String PAGES_DEFAULT_FILE = "/pages/index.html";

  private static final String CONTACTS_FOLDER = "/contacts";

  private static final String CALENDAR_FOLDER = "/calendar";

  private static final String MESSAGE_FOLDER = "/message";

  private static final String PROFILE_FOLDER = "/authprofile";
  
  private static final String PROFILE_BASIC = "/basic/elements";

  private static final String SAKAI_CONTACTSTORE_RT = "sakai/contactstore";

  private static final String SAKAI_CALENDAR_RT = "sakai/calendar";

  private static final String SAKAI_MESSAGESTORE_RT = "sakai/messagestore";

  private static final String SAKAI_PRIVATE_RT = "sakai/private";

  private static final String SAKAI_PUBLIC_RT = "sakai/public";

  private static final String SAKAI_SEARCH_EXCLUDE_TREE_PROP = "sakai:search-exclude-tree";

  private static final String SAKAI_USER_HOME_RT = "sakai/user-home";

  private static final String SAKAI_GROUP_HOME_RT = "sakai/group-home";

  private static final String SAKAI_GROUP_PROFILE_RT = "sakai/group-profile";
  
  private static final String SAKAI_USER_PROFILE_RT = "sakai/user-profile";
  
  private static final String SAKAI_PAGES_RT = "sakai/pages";


  private static final String SLING_RESOURCE_TYPE = "sling:resourceType";
  public static final String VISIBILITY_PRIVATE = "private";
  public static final String VISIBILITY_LOGGED_IN = "logged_in";
  public static final String VISIBILITY_PUBLIC = "public";
  
  public static final String PROFILE_JSON_IMPORT_PARAMETER = ":sakai:profile-import";
  /**
   * Optional parameter containing the path of a Pages source that should be used instead of
   * the default template.
   */
  public static final String PAGES_TEMPLATE_PARAMETER = ":sakai:pages-template";

  public static final String PARAM_ADD_TO_MANAGERS_GROUP = ":sakai:manager";
  public static final String PARAM_REMOVE_FROM_MANAGERS_GROUP = PARAM_ADD_TO_MANAGERS_GROUP + SlingPostConstants.SUFFIX_DELETE;

  @Property(description = "The default access settings for the home of a new user or group.", value = VISIBILITY_PUBLIC, options = {
      @PropertyOption(name = VISIBILITY_PRIVATE, value = "The home is private."),
      @PropertyOption(name = VISIBILITY_LOGGED_IN, value = "The home is blocked to anonymous users; all logged-in users can see it."),
      @PropertyOption(name = VISIBILITY_PUBLIC, value = "The home is completely public.") })
      
  static final String PROFILE_IMPORT_TEMPLATE = "sakai.user.profile.template.default";
  static final String PROFILE_IMPORT_TEMPLATE_DEFAULT = "{'basic':{'elements':{'firstName':{'value':'@@firstName@@'},'lastName':{'value':'@@lastName@@'},'email':{'value':'@@email@@'}},'access':'everybody'}}";
  
  
  @Property(value="/var/templates/pages/systemuser")
  public static final String DEFAULT_USER_PAGES_TEMPLATE = "default.user.template";
  private String defaultUserPagesTemplate;

  @Property(value="/var/templates/pages/systemgroup")
  public static final String DEFAULT_GROUP_PAGES_TEMPLATE = "default.group.template";
  private String defaultGroupPagesTemplate;

  
  private String defaultProfileTemplate;
  private ArrayList<String> profileParams = new ArrayList<String>();
  static final String VISIBILITY_PREFERENCE = "visibility.preference";
  static final String VISIBILITY_PREFERENCE_DEFAULT = VISIBILITY_PUBLIC;

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPostProcessor.class);



  private String visibilityPreference;


  @Activate
  @Modified
  protected void modified(Map<?, ?> props) {
    visibilityPreference = OsgiUtil.toString(props.get(VISIBILITY_PREFERENCE),
        VISIBILITY_PREFERENCE_DEFAULT);
    
    defaultProfileTemplate = PROFILE_IMPORT_TEMPLATE_DEFAULT;

    int startPos = defaultProfileTemplate.indexOf("@@");
    while (startPos > -1) {
      int endPos = defaultProfileTemplate.indexOf("@@", startPos + 2);
      if (endPos > -1) {
        String param = defaultProfileTemplate.substring(startPos + 2, endPos);
        profileParams.add(param);

        endPos = defaultProfileTemplate.indexOf("@@", endPos + 2);
      }
      startPos = endPos;
    }
    
    defaultUserPagesTemplate = OsgiUtil.toString(props.get(DEFAULT_USER_PAGES_TEMPLATE), "");
    defaultGroupPagesTemplate = OsgiUtil.toString(props.get(DEFAULT_GROUP_PAGES_TEMPLATE), "");

  }

  public void process(SlingHttpServletRequest request, Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    LOGGER.debug("Default Prost processor on {} with {} ", authorizable.getId(), change);

    ContentManager contentManager = session.getContentManager();
    AccessControlManager accessControlManager = session.getAccessControlManager();
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    boolean isGroup = authorizable instanceof Group;

    if (ModificationType.DELETE.equals(change.getType())) {
      LOGGER.debug("Performing delete operation on {} ", authorizable.getId());
      if ( isGroup ) {
        deleteManagersGroup(authorizable, authorizableManager);
      }
      return; // do not
    }

    // If the sessionw as capable of performing the create or modify operation, it must be
    // capable of performing these operations.
    String authId = authorizable.getId();
    String homePath = LitePersonalUtils.getHomePath(authId);

    // Home Authorizable PostProcessor
    // ==============================
    // home path
    if (!contentManager.exists(homePath)) {
      Builder<String, Object> props = ImmutableMap.builder();
      if (isGroup) {
        props.put(SLING_RESOURCE_TYPE, StorageClientUtils.toStore(SAKAI_GROUP_HOME_RT));

      } else {
        props.put(SLING_RESOURCE_TYPE, StorageClientUtils.toStore(SAKAI_USER_HOME_RT));
      }
      if (authorizable.hasProperty(SAKAI_SEARCH_EXCLUDE_TREE_PROP)) {
        // raw copy
        props.put(SAKAI_SEARCH_EXCLUDE_TREE_PROP,
            authorizable.getProperty(SAKAI_SEARCH_EXCLUDE_TREE_PROP));
      }
      contentManager.update(new Content(homePath, props.build()));

      List<AclModification> aclModifications = new ArrayList<AclModification>();
      // KERN-886 : Depending on the profile preference we set some ACL's on the profile.
      if (User.ANON_USER.equals(authId)) {
        AclModification.addAcl(true, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(true, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else if (VISIBILITY_PUBLIC.equals(visibilityPreference)) {
        AclModification.addAcl(true, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(true, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else if (VISIBILITY_LOGGED_IN.equals(visibilityPreference)) {
        AclModification.addAcl(false, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(true, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else if (VISIBILITY_PRIVATE.equals(visibilityPreference)) {
        AclModification.addAcl(false, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(false, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      }

      Map<String, Object> acl = Maps.newHashMap();
      syncOwnership(authorizable, acl, aclModifications);

      accessControlManager.setAcl(Security.ZONE_CONTENT, homePath,
          aclModifications.toArray(new AclModification[aclModifications.size()]));

    } else {
      // Sync the Acl on the home folder with whatever is present in the authorizable
      // permissions.
      Map<String, Object> acl = accessControlManager.getAcl(Security.ZONE_CONTENT,
          homePath);
      List<AclModification> aclModifications = new ArrayList<AclModification>();

      syncOwnership(authorizable, acl, aclModifications);

      accessControlManager.setAcl(Security.ZONE_CONTENT, homePath,
          aclModifications.toArray(new AclModification[aclModifications.size()]));
    }
    if (ModificationType.CREATE.equals(change.getType())) {
      createPath(authId, LitePersonalUtils.getPublicPath(authId), SAKAI_PUBLIC_RT, false,
          contentManager, accessControlManager, null);
      createPath(authId, LitePersonalUtils.getPrivatePath(authId), SAKAI_PRIVATE_RT,
          true, contentManager, accessControlManager, null);
      // Group Authorizable PostProcessor
      // ==============================
      // no action required (IMO we should drop the generated group and use ACL on the
      // object itself)
      if (isGroup) {
        updateManagersGroup(authorizable, authorizableManager, accessControlManager,
            parameters);
      }
      // Message PostProcessor
      createPath(authId, homePath + MESSAGE_FOLDER, SAKAI_MESSAGESTORE_RT, true,
          contentManager, accessControlManager, null);
      // Calendar
      createPath(authId, homePath + CALENDAR_FOLDER, SAKAI_CALENDAR_RT, false,
          contentManager, accessControlManager, null);
      // Connections
      createPath(authId, homePath + CONTACTS_FOLDER, SAKAI_CONTACTSTORE_RT, true,
          contentManager, accessControlManager, null);
      // Pages
      boolean createdPages = createPath(authId, homePath + PAGES_FOLDER, SAKAI_PAGES_RT,
          false, contentManager, accessControlManager, null);
      createPath(authId, homePath + PAGES_DEFAULT_FILE, SAKAI_PAGES_RT, false,
          contentManager, accessControlManager, null);
      if (createdPages) {
        intitializeContent(request, authorizable, session, homePath + PAGES_FOLDER,
            parameters);
      }
      // Profile
      String profileType = (authorizable instanceof Group) ? SAKAI_GROUP_PROFILE_RT
                                                          : SAKAI_USER_PROFILE_RT;
      createPath(authId, LitePersonalUtils.getPublicPath(authId) + PROFILE_FOLDER,
          profileType, false, contentManager, accessControlManager, null);
      createPath(authId, LitePersonalUtils.getProfilePath(authId) + PROFILE_BASIC,
          "nt:unstructured", false, contentManager, accessControlManager,
          processProfileParameters(defaultProfileTemplate, authorizable, parameters));
    }

  }

  private void intitializeContent(SlingHttpServletRequest request, Authorizable authorizable, Session session,
      String pagesPath, Map<String, Object[]> parameters) throws StorageClientException, AccessDeniedException, IOException  {
    String templatePath = null;

    // Check for an explicit pages template path.
    Object[] templateParameterValues = parameters.get(PAGES_TEMPLATE_PARAMETER);
    if (templateParameterValues != null) {
      if ((templateParameterValues.length == 1) && templateParameterValues[0] instanceof String) {
        String templateParameterValue = (String) templateParameterValues[0];
        if (templateParameterValue.length() > 0) {
          templatePath = templateParameterValue;
        }
      } else {
        LOGGER.warn("Unexpected {} value = {}. Using defaults instead.", PAGES_TEMPLATE_PARAMETER, templateParameterValues);
      }
    }

    // If no template was specified, use the default.
    if (templatePath == null) {
      if (authorizable instanceof Group) {
        templatePath = defaultGroupPagesTemplate;
      } else {
        templatePath = defaultUserPagesTemplate;
      }
    }
    
    Resource resource = request.getResourceResolver().resolve(templatePath);
    Node node = resource.adaptTo(Node.class);
    if ( node != null) {
      try {
        recursiveCopy(node, pagesPath, session.getContentManager());
      } catch (RepositoryException e) {
        LOGGER.warn("Failed to Copy JCR Template ",e);
        throw new StorageClientException(e.getMessage(),e);
      }
    } else {
      session.getContentManager().copy(templatePath, pagesPath, true);
    }
  }

  private void recursiveCopy(Node node, String thisPath, ContentManager contentManager) throws RepositoryException, StorageClientException, AccessDeniedException, IOException {
      Builder<String, Object> builder = ImmutableMap.builder();
      PropertyIterator pi = node.getProperties();
      while(pi.hasNext()) {
        javax.jcr.Property p = pi.nextProperty();
        LOGGER.debug("Setting property {}:{} ",thisPath,p.getName());
        PropertyDefinition pd = p.getDefinition();
        int type = pd.getRequiredType();
        if ( pd.isMultiple() ) {
          Value[] v = p.getValues();
          switch(type) {
          case PropertyType.DATE:
            {
              Calendar[] s = new Calendar[v.length];
              for ( int i = 0; i < v.length; i++ ) {
                s[i] = v[i].getDate();
              }
              builder.put(p.getName(), StorageClientUtils.toStore(s));
              break;
            }
          default:
            {
              String[] s = new String[v.length];
              for ( int i = 0; i < v.length; i++ ) {
                s[i] = v[i].getString();
              }
              builder.put(p.getName(), StorageClientUtils.toStore(s));
              break;
            }
          }
          
        } else {
          Value v = p.getValue();
          switch(type) {
          case PropertyType.BOOLEAN:
            builder.put(p.getName(), StorageClientUtils.toStore(v.getBoolean()));
            break;
          case PropertyType.DATE:
            builder.put(p.getName(), StorageClientUtils.toStore(v.getDate()));
            break;
          case PropertyType.DECIMAL:
            builder.put(p.getName(), StorageClientUtils.toStore(v.getDecimal()));
            break;
          case PropertyType.LONG:
            builder.put(p.getName(), StorageClientUtils.toStore(v.getLong()));
            break;
          case PropertyType.STRING:
            builder.put(p.getName(), StorageClientUtils.toStore(v.getString()));
            break;
          default:
            builder.put(p.getName(), StorageClientUtils.toStore(v.getString()));
            break;
          }
        }
        Content content = contentManager.get(thisPath);
        if ( content == null ) {
          contentManager.update(new Content(thisPath, builder.build()));
        } else {
          Map<String,Object> props = builder.build();
          for ( Entry<String, Object> e : props.entrySet()) {
            content.setProperty(e.getKey(), e.getValue());
          }
        }
        
        if ( JcrConstants.NT_FILE.equals(node.getPrimaryNodeType().getName())) {
          Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
          javax.jcr.Property data = contentNode.getProperty(JcrConstants.JCR_DATA);
          contentManager.writeBody(thisPath, data.getBinary().getStream());
          if ( content == null ) {
            content = contentManager.get(thisPath);
            if ( contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
              content.setProperty(Content.MIMETYPE, StorageClientUtils.toStore(contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString()));
            }
            if ( contentNode.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
              content.setProperty(Content.LASTMODIFIED, StorageClientUtils.toStore(contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getLong()));
            }
          }
        }
        if ( content != null ) {
          contentManager.update(content);
        }
        NodeIterator ni = node.getNodes();
        while(ni.hasNext()) {
          Node n = ni.nextNode();
          if ( !JcrConstants.NT_FILE.equals(n.getPrimaryNodeType().getName()) ) {
            recursiveCopy(n, thisPath+"/"+n.getName(), contentManager);
          }
        }
    }
  }

  @Deprecated
  private void deleteManagersGroup(Authorizable authorizable, AuthorizableManager authorizableManager) {
    if (authorizable.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
      String managersGroup = StorageClientUtils.toString(authorizable.getProperty(UserConstants.PROP_MANAGERS_GROUP));
      LOGGER.debug(" {} deleting managers group  {}",authorizable.getId(), managersGroup);
      try {
        authorizableManager.delete(managersGroup);
      } catch ( Exception e ) {
        LOGGER.info("Failed to delete managers group {}  {}",managersGroup);
      }
    } else {
      LOGGER.debug(" {} has no manager group {} ", authorizable, authorizable.getSafeProperties());
    }
  }

  /**
   * Create or update the managers group. Note, this is deprecated since this is not how we will do this longer term.
   * @param authorizable
   * @param authorizableManager
   * @param accessControlManager
   * @param parameters
   * @throws AccessDeniedException
   * @throws StorageClientException
   */
  @Deprecated
  private void updateManagersGroup(Authorizable authorizable, AuthorizableManager authorizableManager, AccessControlManager accessControlManager, Map<String, Object[]> parameters) throws AccessDeniedException, StorageClientException {
    if ( !authorizable.hasProperty(PROP_MANAGERS_GROUP) ) {
      // if authorizable.getId() is unique, then it only has 1 manages group, which is also unique by definition.
       String managersGroupId = authorizable.getId() + "-managers";
       authorizable.setProperty(PROP_MANAGERS_GROUP, StorageClientUtils.toStore(managersGroupId));
       Set<String> managers = Sets.newHashSet(StorageClientUtils.toStringArray(authorizable.getProperty(UserConstants.PROP_GROUP_MANAGERS)));
       managers.add(managersGroupId);
       authorizable.setProperty(UserConstants.PROP_GROUP_MANAGERS, StorageClientUtils.toStore(managers.toArray(new String[managers.size()])));


       authorizableManager.updateAuthorizable(authorizable);

       authorizableManager.createGroup(managersGroupId, managersGroupId, ImmutableMap.of(
           PROP_MANAGED_GROUP, StorageClientUtils.toStore(authorizable.getId()), // the ID of the group this group manages
           PROP_MANAGERS_GROUP,StorageClientUtils.toStore(managersGroupId), // the ID of the special managers group
           PROP_GROUP_MANAGERS,StorageClientUtils.toStore(managersGroupId), // the managers of this group (ie itself)
           PROP_BARE_AUTHORIZABLE, StorageClientUtils.toStore(true)
           ));

       Group managersGroup = (Group) authorizableManager.findAuthorizable(managersGroupId);
       Object[] addValues = parameters.get(PARAM_ADD_TO_MANAGERS_GROUP);
       if ((addValues != null) && (addValues instanceof String[])) {
         for (String memberId : (String [])addValues) {
           Authorizable toAdd = authorizableManager.findAuthorizable(memberId);
           if (toAdd != null) {
             managersGroup.addMember(toAdd.getId());
           } else {
             LOGGER.warn("Could not add {} to managers group {}", memberId, managersGroupId);
           }
         }
       }
       authorizableManager.updateAuthorizable(managersGroup);


       // grant the mangers group management over this group
       accessControlManager.setAcl(
           Security.ZONE_AUTHORIZABLES, authorizable.getId(), new AclModification[] {
             new AclModification(AclModification.grantKey(managersGroupId), Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE)
           });
       // and over itself
       accessControlManager.setAcl(
           Security.ZONE_AUTHORIZABLES, managersGroupId, new AclModification[] {
             new AclModification(AclModification.grantKey(managersGroupId), Permissions.CAN_MANAGE.getPermission(), Operation.OP_REPLACE)
           });
    } else {
      String managersGroupId = StorageClientUtils.toString(authorizable.getProperty(PROP_MANAGERS_GROUP));
      Group managersGroup = (Group) authorizableManager.findAuthorizable(managersGroupId);
      Object[] removeValues = parameters.get(PARAM_REMOVE_FROM_MANAGERS_GROUP);
      if ((removeValues != null) && (removeValues instanceof String[])) {
        for (String memberId : (String [])removeValues) {
           managersGroup.removeMember(memberId);
        }
      }
      Object[] addValues = parameters.get(PARAM_ADD_TO_MANAGERS_GROUP);
      if ((addValues != null) && (addValues instanceof String[])) {
        for (String memberId : (String [])addValues) {
          Authorizable toAdd = authorizableManager.findAuthorizable(memberId);
          if (toAdd != null) {
            managersGroup.addMember(toAdd.getId());
          } else {
            LOGGER.warn("Could not add {} to managers group {}", memberId, managersGroup.getId());
          }
        }
      }
      // update knows if anything has changed and wont update if nothing changed.
      authorizableManager.updateAuthorizable(managersGroup);
    }
  }

  private Map<String, Object> processProfileParameters(String profileTemplate,
      Authorizable authorizable, Map<String, Object[]> parameters) {
    Map<String, Object> retval = new HashMap<String, Object>();
    if (parameters.containsKey(PROFILE_JSON_IMPORT_PARAMETER)) {
      String profileJson = (String) parameters.get(PROFILE_JSON_IMPORT_PARAMETER)[0];
      JSONObject jsonObject = JSONObject.fromObject(profileJson);
      JSONObject basic = jsonObject.getJSONObject("basic");
      if (basic != null) {
        JSONObject elements = basic.getJSONObject("elements");
        if (elements != null) {
          for (Object propName : elements.entrySet()) {
            retval.put((String)propName, elements.get(propName));
          }
        }
      }
    }
      for (String param : profileParams) {
        String val = "unknown";
        if (parameters.containsKey(param)) {
          val = (String) parameters.get(param)[0];
        } else if (authorizable.hasProperty(param)) {
          val = StorageClientUtils.toString(authorizable.getProperty(param));
        }
        retval.put(param, StorageClientUtils.toStore(val));
      }
      return retval;
  }

  private boolean createPath(String authId, String path, String resourceType, boolean isPrivate,
      ContentManager contentManager, AccessControlManager accessControlManager, Map<String, Object>additionalProperties)
      throws AccessDeniedException, StorageClientException {
    Builder<String, Object> propertyBuilder = ImmutableMap.builder();
    propertyBuilder.put(SLING_RESOURCE_TYPE, resourceType);
    if (additionalProperties != null) {
      propertyBuilder.putAll(additionalProperties);
    }
    if (!contentManager.exists(path)) {
      contentManager.update(new Content(path, propertyBuilder.build()));
      if (isPrivate) {
        accessControlManager.setAcl(
            Security.ZONE_CONTENT,
            path,
            new AclModification[] {
                new AclModification(AclModification.denyKey(User.ANON_USER),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE),
                new AclModification(AclModification.denyKey(Group.EVERYONE),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE),
                new AclModification(AclModification.grantKey(authId),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE)});
      }
      return true;
    }
    return false;
  }

  private void syncOwnership(Authorizable authorizable, Map<String, Object> acl,
      List<AclModification> aclModifications) {
    // remove all acls we are not concerned with from the copy of the current state
    acl.remove(AclModification.denyKey(User.ANON_USER));
    acl.remove(AclModification.grantKey(User.ANON_USER));
    acl.remove(AclModification.denyKey(User.ADMIN_USER));
    acl.remove(AclModification.grantKey(User.ADMIN_USER));
    acl.remove(AclModification.denyKey(Group.EVERYONE));
    acl.remove(AclModification.grantKey(Group.EVERYONE));
    acl.remove(AclModification.denyKey(authorizable.getId()));
    acl.remove(AclModification.grantKey(authorizable.getId()));
    

    // make sure the owner has permission on their home
    if (authorizable instanceof User && !User.ANON_USER.equals(authorizable.getId())) {
      AclModification.addAcl(true, Permissions.ALL, authorizable.getId(),
          aclModifications);
    }

    Set<String> managerSettings = null;
    if (authorizable.hasProperty(UserConstants.PROP_GROUP_MANAGERS)) {
      managerSettings = ImmutableSet.of(StorageClientUtils.toStringArray(authorizable
          .getProperty(UserConstants.PROP_GROUP_MANAGERS)));
    } else {
      managerSettings = ImmutableSet.of();
    }
    Set<String> viewerSettings = null;
    if (authorizable.hasProperty(UserConstants.PROP_GROUP_VIEWERS)) {
      viewerSettings = ImmutableSet.of(StorageClientUtils.toStringArray(authorizable
          .getProperty(UserConstants.PROP_GROUP_VIEWERS)));
    } else {
      viewerSettings = ImmutableSet.of();
    }

    for (String key : acl.keySet()) {
      if (AclModification.isGrant(key)) {
        String principal = AclModification.getPrincipal(key);
        if (!managerSettings.contains(principal)) {
          // grant permission is present, but not present in managerSettings, manage
          // ability (which include read ability must be removed)
          if (viewerSettings.contains(principal)) {
            aclModifications.add(new AclModification(key, Permissions.CAN_READ
                .getPermission(), Operation.OP_REPLACE));
          } else {
            aclModifications.add(new AclModification(key, Permissions.CAN_MANAGE
                .getPermission(), Operation.OP_XOR));
          }
        }
      }
    }
    for (String manager : managerSettings) {
      if (!acl.containsKey(AclModification.grantKey(manager))) {
        AclModification.addAcl(true, Permissions.CAN_MANAGE, manager, aclModifications);
      }
    }
    for (String viewer : viewerSettings) {
      if (!acl.containsKey(AclModification.grantKey(viewer))) {
        AclModification.addAcl(true, Permissions.CAN_READ, viewer, aclModifications);
      }
    }
    if (viewerSettings.size() > 0) {
      // ensure its private
      aclModifications.add(new AclModification(AclModification.grantKey(User.ANON_USER),
          Permissions.ALL.getPermission(), Operation.OP_DEL));
      if (!viewerSettings.contains(Group.EVERYONE)) {
        // only deny everyone if not in the list of viewers
        aclModifications.add(new AclModification(
            AclModification.grantKey(Group.EVERYONE), Permissions.ALL.getPermission(),
            Operation.OP_DEL));
      }
      aclModifications.add(new AclModification(AclModification.denyKey(User.ANON_USER),
          Permissions.ALL.getPermission(), Operation.OP_REPLACE));
      if (!viewerSettings.contains(Group.EVERYONE)) {
        // only deny everyone if not in the list of viewers
        aclModifications.add(new AclModification(AclModification.denyKey(Group.EVERYONE),
            Permissions.ALL.getPermission(), Operation.OP_REPLACE));
      }
    } else {
      // anon and everyone can read
      aclModifications.add(new AclModification(AclModification.denyKey(User.ANON_USER),
          Permissions.ALL.getPermission(), Operation.OP_DEL));
      aclModifications.add(new AclModification(AclModification.denyKey(Group.EVERYONE),
          Permissions.ALL.getPermission(), Operation.OP_DEL));
      aclModifications.add(new AclModification(AclModification.grantKey(User.ANON_USER),
          Permissions.CAN_READ.getPermission(), Operation.OP_REPLACE));
      aclModifications.add(new AclModification(AclModification.grantKey(Group.EVERYONE),
          Permissions.CAN_READ.getPermission(), Operation.OP_REPLACE));

    }

  }
}
  
  

