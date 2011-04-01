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
package org.sakaiproject.nakamura.files.servlets;

import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_NAME;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS;
import static org.sakaiproject.nakamura.api.files.FilesConstants.TOPIC_FILES_TAG;

import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.lite.AbstractSparsePostOperation;
import org.sakaiproject.nakamura.api.resource.lite.SparseContentResource;
import org.sakaiproject.nakamura.api.resource.lite.SparsePostOperation;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "TagOperation", shortDescription = "Tag a node", description = { "Add a tag to a node." }, methods = { @ServiceMethod(name = "POST", description = { "This operation should be performed on the node you wish to tag. Tagging on any item will be performed by adding a weak reference to the content item. Put simply a sakai:tag-uuid property with the UUID of the tag node. We use the UUID to uniquely identify the tag in question, a string of the tag name is not sufficient. This allows the tag to be renamed and moved without breaking the relationship. Additionally for convenience purposes we may put the name of the tag at the time of tagging in sakai:tag although this will not be actively maintained. " }, parameters = {
    @ServiceParameter(name = ":operation", description = "The value HAS TO BE <i>tag</i>."),
    @ServiceParameter(name = "key", description = "Can be either 1) A fully qualified path, 2) UUID, or 3) a content poolId.") }, response = {
    @ServiceResponse(code = 201, description = "The tag was added to the node."),
    @ServiceResponse(code = 400, description = "The request did not have sufficient information to perform the tagging, probably a missing parameter or the uuid does not point to an existing tag."),
    @ServiceResponse(code = 403, description = "Anonymous users can't tag anything, other people can tag <i>every</i> node in the repository where they have READ on."),
    @ServiceResponse(code = HttpServletResponse.SC_NOT_FOUND, description = "Requested Node  for given key could not be found."),
    @ServiceResponse(code = 500, description = "Something went wrong, the error is in the HTML.") }) }, bindings = { @ServiceBinding(type = BindingType.OPERATION, bindings = { "tag" }) })
@Component(immediate = true)
@Service(value = SparsePostOperation.class)
@Properties(value = {
    @Property(name = "sling.post.operation", value = "tag"),
    @Property(name = "service.description", value = "Creates an internal link to a file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
public class SparseTagOperation extends AbstractSparsePostOperation {


  @Reference
  protected transient EventAdmin eventAdmin;

  private static final Logger LOGGER = LoggerFactory.getLogger(SparseTagOperation.class);

  private static final long serialVersionUID = -7724827744698056843L;

  /**
   * {@inheritDoc}
   * @throws AccessDeniedException 
   * @throws StorageClientException 
   *
   * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      ContentManager contentManager, List<Modification> changes, String contentPath) throws StorageClientException, AccessDeniedException {

    Session session = StorageClientUtils.adaptToSession(request.getResource().getResourceResolver().adaptTo(javax.jcr.Session.class));
    AuthorizableManager authManager = session.getAuthorizableManager();

    // Check if the user has the required minimum privilege.
    String user = request.getRemoteUser();
    if (UserConstants.ANON_USERID.equals(user)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "Anonymous users can't tag things.");
      return;
    }

    Resource resource = request.getResource();
    Content content = resource.adaptTo(Content.class);
    ResourceResolver resourceResolver = request.getResourceResolver();

    if ( content == null) {
      LOGGER.info("Missing Resource  {} ", resource.getPath());
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "A tag operation must be performed on an actual resource");
      return;
    }

    String resourceType = (String) content.getProperty("sling:resourceType");
    boolean isProfile = "sakai/user-profile".equals(resourceType) || "sakai/group-profile".equals(resourceType);
    // Check if the uuid is in the request.
    RequestParameter key = request.getRequestParameter("key");
    if (key == null || "".equals(key.getString())) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "Missing parameter: key");
      return;
    }
    
    String tagContentPath = key.getString();
    
    String tagName = "";
    String tagUuid = "";
    try {
      Resource tagResource = resourceResolver.getResource(tagContentPath);
      if (tagResource == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND, "No tag exists at path " + tagContentPath);
        return;
      }
      if (tagResource instanceof SparseContentResource) {
        Content contentTag = tagResource.adaptTo(Content.class);
        tagUuid = (String) contentTag.getProperty(Content.UUID_FIELD);
        tagName = tagContentWithContentTag(contentManager, content, contentTag);
      } else {
        Node nodeTag = tagResource.adaptTo(Node.class);
        tagUuid = nodeTag.getIdentifier();
        tagName = tagContentWithNodeTag(contentManager, content, nodeTag);
      }
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
      return;
    }
    
    // If we're tagging an authorizable, add the property here
    if (isProfile) {
      final String azId = PathUtils.getAuthorizableId(content.getPath());
      Authorizable authorizable = authManager.findAuthorizable(azId);
      if (authorizable != null) {
        Set<String> nameSet = Sets.newHashSet(StorageClientUtils
            .nonNullStringArray((String[]) authorizable.getProperty(SAKAI_TAG_UUIDS)));
        nameSet.add(tagUuid);
        authorizable.setProperty(SAKAI_TAG_UUIDS,
            nameSet.toArray(new String[nameSet.size()]));
        authManager.updateAuthorizable(authorizable);
      }
    }
    // Send an OSGi event.
    try {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, user);
      properties.put("tag-name", tagName);
      EventUtils.sendOsgiEvent(request.getResource(), properties, TOPIC_FILES_TAG,
          eventAdmin);
    } catch (Exception e) {
      // We do NOT interrupt the normal workflow if sending an event fails.
      // We just log it to the error log.
      LOGGER.error("Could not send an OSGi event for tagging a file", e);
    }

  }

  private String tagContentWithNodeTag(ContentManager contentManager, Content content, Node nodeTag) throws Exception {
    String tagName = "";
    try {
        checkForTagResourceType(nodeTag.getProperty("sling:resourceType").getString());
        FileUtils.addTag(contentManager, content, nodeTag);
        if (nodeTag.hasProperty(SAKAI_TAG_NAME)) {
          tagName = nodeTag.getProperty(SAKAI_TAG_NAME).getString();
        }
    } catch (RepositoryException re) {
      throw new Exception(re.getLocalizedMessage(), re);
    }
    return tagName;
  }

  private String tagContentWithContentTag(ContentManager contentManager, Content content, Content contentTag) throws Exception {
    String tagName = "";
    try {
      checkForTagResourceType((String)contentTag.getProperty("sling:resourceType"));
      FileUtils.addTag(contentManager, content, contentTag);
      if (contentTag.hasProperty(SAKAI_TAG_NAME)) {
        tagName = (String)contentTag.getProperty(SAKAI_TAG_NAME);
      }
    } catch (Exception e) {
      throw new Exception(e.getLocalizedMessage(), e);
    }
    return tagName;
  }
  
  private void checkForTagResourceType(String type) throws Exception {
    if (!"sakai/tag".equals(type)) {
      throw new Exception("Provided key doesn't point to a tag.");
    }
  }

  /**
   * Checks if the node already has the uuid in it's properties.
   *
   * @param node
   * @param uuid
   * @return
   * @throws RepositoryException
   */
  protected boolean hasUuid(Node node, String uuid) throws RepositoryException {
    Value[] uuids = JcrUtils.getValues(node, SAKAI_TAG_UUIDS);
    for (Value v : uuids) {
      if (v.getString().equals(uuid)) {
        return true;
      }
    }
    return false;
  }
}
