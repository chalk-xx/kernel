package org.sakaiproject.nakamura.user.postprocessors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Security;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.user.LiteAuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.LitePersonalUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  private static final String CONTACTS_FOLDER = "/contacts";

  private static final String CALENDAR_FOLDER = "/calendar";

  private static final String MESSAGE_FOLDER = "/message";

  private static final String SAKAI_CONTACTSTORE_RT = "sakai/contactstore";

  private static final String SAKAI_CALENDAR_RT = "sakai/calendar";

  private static final String SAKAI_MESSAGESTORE_RT = "sakai/messagestore";

  private static final String SAKAI_PRIVATE_RT = "sakai/private";

  private static final String SAKAI_PUBLIC_RT = "sakai/public";

  private static final String SAKAI_SEARCH_EXCLUDE_TREE_PROP = "sakai:search-exclude-tree";

  private static final String SAKAI_USER_HOME_RT = "sakai/user-home";

  private static final String SAKAI_GROUP_HOME_RT = "sakai/group-home";

  private static final String SLING_RESOURCE_TYPE = "sling:resourceType";
  static final String VISIBILITY_PRIVATE = "private";
  static final String VISIBILITY_LOGGED_IN = "logged_in";
  static final String VISIBILITY_PUBLIC = "public";

  @Property(description = "The default access settings for the home of a new user or group.", value = VISIBILITY_PUBLIC, options = {
      @PropertyOption(name = VISIBILITY_PRIVATE, value = "The home is private."),
      @PropertyOption(name = VISIBILITY_LOGGED_IN, value = "The home is blocked to anonymous users; all logged-in users can see it."),
      @PropertyOption(name = VISIBILITY_PUBLIC, value = "The home is completely public.") })
  static final String VISIBILITY_PREFERENCE = "visibility.preference";
  static final String VISIBILITY_PREFERENCE_DEFAULT = VISIBILITY_PUBLIC;
  private String visibilityPreference;

  @Activate
  @Modified
  protected void modified(Map<?, ?> props) {
    visibilityPreference = OsgiUtil.toString(props.get(VISIBILITY_PREFERENCE),
        VISIBILITY_PREFERENCE_DEFAULT);
  }

  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {

    if (ModificationType.DELETE.equals(change)) {
      return; // do nothing
    }

    // If the sessionw as capable of performing the create or modify operation, it must be
    // capable of performing these operations.
    ContentManager contentManager = session.getContentManager();
    AccessControlManager accessControlManager = session.getAccessControlManager();
    String homePath = LitePersonalUtils.getHomePath(authorizable.getId());
    boolean isGroup = authorizable instanceof Group;

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
      if (User.ANON_USER.equals(authorizable.getId())) {
        AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else if (VISIBILITY_PUBLIC.equals(visibilityPreference)) {
        AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else if (VISIBILITY_LOGGED_IN.equals(visibilityPreference)) {
        AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(Boolean.TRUE, Permissions.CAN_READ, Group.EVERYONE,
            aclModifications);
      } else if (VISIBILITY_PUBLIC.equals(visibilityPreference)) {
        AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, User.ANON_USER,
            aclModifications);
        AclModification.addAcl(Boolean.FALSE, Permissions.CAN_READ, Group.EVERYONE,
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
    createPath(LitePersonalUtils.getPublicPath(authorizable.getId()), SAKAI_PUBLIC_RT,
        false, contentManager, accessControlManager);
    createPath(LitePersonalUtils.getPrivatePath(authorizable.getId()), SAKAI_PRIVATE_RT,
        true, contentManager, accessControlManager);

    // User Authorizable PostProcessor
    // ==============================
    // no action required

    // Group Authorizable PostProcessor
    // ==============================
    // no action required (IMO we should drop the generated group and use ACL on the
    // object itself)

    // Message PostProcessor
    createPath(homePath + MESSAGE_FOLDER, SAKAI_MESSAGESTORE_RT, true, contentManager,
        accessControlManager);
    // Calendar
    createPath(homePath + CALENDAR_FOLDER, SAKAI_CALENDAR_RT, false, contentManager,
        accessControlManager);
    // Connections
    createPath(homePath + CONTACTS_FOLDER, SAKAI_CONTACTSTORE_RT, true, contentManager,
        accessControlManager);
    // Pages
    // TODO:
    // Profile
    // TODO:

  }

  private boolean createPath(String path, String resourceType, boolean isPrivate,
      ContentManager contentManager, AccessControlManager accessControlManager)
      throws AccessDeniedException, StorageClientException {
    if (!contentManager.exists(path)) {
      contentManager.update(new Content(path, ImmutableMap.of(SLING_RESOURCE_TYPE,
          StorageClientUtils.toStore(resourceType))));
      if (isPrivate) {
        accessControlManager.setAcl(
            Security.ZONE_CONTENT,
            path,
            new AclModification[] {
                new AclModification(AclModification.denyKey(User.ANON_USER),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE),
                new AclModification(AclModification.denyKey(Group.EVERYONE),
                    Permissions.ALL.getPermission(), Operation.OP_REPLACE), });
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
