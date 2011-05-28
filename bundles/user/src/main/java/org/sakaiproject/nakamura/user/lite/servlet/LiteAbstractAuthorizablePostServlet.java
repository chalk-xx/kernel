/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.lite.servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.authorizable.Group;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.api.resource.DateParser;
import org.sakaiproject.nakamura.api.resource.JSONResponse;
import org.sakaiproject.nakamura.api.resource.RequestProperty;
import org.sakaiproject.nakamura.user.lite.servlet.LitePropertyType.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for all the POST servlets for the UserManager operations
 */
@Component(immediate=true, metatype=true,componentAbstract=true)
public abstract class LiteAbstractAuthorizablePostServlet extends
        SlingAllMethodsServlet {
    private static final long serialVersionUID = -5918670409789895333L;

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     */
    @Property(value={"EEE MMM dd yyyy HH:mm:ss 'GMT'Z","yyyy-MM-dd'T'HH:mm:ss.SSSZ","yyyy-MM-dd'T'HH:mm:ss","yyyy-MM-dd","dd.MM.yyyy HH:mm:ss","dd.MM.yyyy"})
    private static final String PROP_DATE_FORMAT = "servlet.post.dateFormats";

    private static final Logger LOGGER = LoggerFactory.getLogger(LiteAbstractAuthorizablePostServlet.class);

    private DateParser dateParser;

    // ---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        Dictionary<?, ?> props = context.getProperties();

        dateParser = new DateParser();
        String[] dateFormats = PropertiesUtil.toStringArray(props.get(PROP_DATE_FORMAT));
        for (String dateFormat : dateFormats) {
            dateParser.register(dateFormat);
        }
    }

    protected void deactivate(ComponentContext context) {
        dateParser = null;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache
     * .sling.api.SlingHttpServletRequest,
     * org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse httpResponse) throws ServletException,
            IOException {
        // prepare the response
        HtmlResponse htmlResponse = createHtmlResponse(request);
        htmlResponse.setReferer(request.getHeader("referer"));

        // calculate the paths
        String path = getItemPath(request);
        htmlResponse.setPath(path);

        // location
        htmlResponse.setLocation(path);

        // parent location
        path = ResourceUtil.getParent(path);
        if (path != null) {
            htmlResponse.setParentLocation(path);
        }

        final List<Modification> changes = new ArrayList<Modification>();

        try {
            handleOperation(request, htmlResponse, changes);

            // TODO: maybe handle SlingAuthorizablePostProcessor handlers here

            // set changes on html response
            for (Modification change : changes) {
                switch (change.getType()) {
                    case MODIFY:
                        htmlResponse.onModified(change.getSource());
                        break;
                    case DELETE:
                        htmlResponse.onDeleted(change.getSource());
                        break;
                    case MOVE:
                        htmlResponse.onMoved(change.getSource(),
                            change.getDestination());
                        break;
                    case COPY:
                        htmlResponse.onCopied(change.getSource(),
                            change.getDestination());
                        break;
                    case CREATE:
                        htmlResponse.onCreated(change.getSource());
                        break;
                    case ORDER:
                        htmlResponse.onChange("ordered", change.getSource(),
                            change.getDestination());
                        break;
                }
            }
        } catch ( AccessDeniedException e ) {
          log.info("Exception while handling POST "
              + request.getResource().getPath() + " with "
              + getClass().getName());
          log.debug("Exception was "+e.getMessage(),e);
          htmlResponse.setStatus(403, e.getMessage());
        } catch (ResourceNotFoundException rnfe) {
            htmlResponse.setStatus(HttpServletResponse.SC_NOT_FOUND,
                rnfe.getMessage());
        } catch (Throwable throwable) {
            log.info("Exception while handling POST "
                + request.getResource().getPath() + " with "
                + getClass().getName(), throwable);
            htmlResponse.setError(throwable);
        }

        // check for redirect URL if processing succeeded
        if (htmlResponse.isSuccessful()) {
            String redirect = getRedirectUrl(request, htmlResponse);
            if (redirect != null) {
                httpResponse.sendRedirect(redirect);
                return;
            }
        }

        // create a html response and send if unsuccessful or no redirect
        htmlResponse.send(httpResponse, isSetStatus(request));
    }

    /**
     * Creates an instance of a HtmlResponse.
     * @param req The request being serviced
     * @return a {@link org.apache.sling.servlets.post.impl.helper.JSONResponse} if any of these conditions are true:
     * <ul>
     *   <li>the response content type is application/json
     * </ul>
     * or a {@link org.apache.sling.api.servlets.HtmlResponse} otherwise
     */
    protected HtmlResponse createHtmlResponse(SlingHttpServletRequest req) {
      if (JSONResponse.RESPONSE_CONTENT_TYPE.equals(req.getResponseContentType())) {
        return new JSONResponse();
      } else {
            return new HtmlResponse();
      }
    }
    
    /**
     * Extending Servlet should implement this operation to do the work
     * 
     * @param request the sling http request to process
     * @param htmlResponse the response
     * @param changes
     * @throws AuthorizableExistsException 
     */
    abstract protected void handleOperation(SlingHttpServletRequest request,
            HtmlResponse htmlResponse, List<Modification> changes) throws StorageClientException, AccessDeniedException, AuthorizableExistsException;

    /**
     * compute redirect URL (SLING-126)
     * 
     * @param ctx the post processor
     * @return the redirect location or <code>null</code>
     */
    protected String getRedirectUrl(HttpServletRequest request, HtmlResponse ctx) {
        // redirect param has priority (but see below, magic star)
        String result = request.getParameter(SlingPostConstants.RP_REDIRECT_TO);
        if (result != null && ctx.getPath() != null) {

            // redirect to created/modified Resource
            int star = result.indexOf('*');
            if (star >= 0) {
                StringBuffer buf = new StringBuffer();

                // anything before the star
                if (star > 0) {
                    buf.append(result.substring(0, star));
                }

                // append the name of the manipulated node
                buf.append(ResourceUtil.getName(ctx.getPath()));

                // anything after the star
                if (star < result.length() - 1) {
                    buf.append(result.substring(star + 1));
                }

                // use the created path as the redirect result
                result = buf.toString();

            } else if (result.endsWith(SlingPostConstants.DEFAULT_CREATE_SUFFIX)) {
                // if the redirect has a trailing slash, append modified node
                // name
                result = result.concat(ResourceUtil.getName(ctx.getPath()));
            }

            if (log.isDebugEnabled()) {
                log.debug("Will redirect to " + result);
            }
        }
        return result;
    }

    protected boolean isSetStatus(SlingHttpServletRequest request) {
        String statusParam = request.getParameter(SlingPostConstants.RP_STATUS);
        if (statusParam == null) {
            log.debug(
                "getStatusMode: Parameter {} not set, assuming standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        if (SlingPostConstants.STATUS_VALUE_BROWSER.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for user-friendly status code",
                SlingPostConstants.RP_STATUS);
            return false;
        }

        if (SlingPostConstants.STATUS_VALUE_STANDARD.equals(statusParam)) {
            log.debug(
                "getStatusMode: Parameter {} asks for standard status code",
                SlingPostConstants.RP_STATUS);
            return true;
        }

        log.debug(
            "getStatusMode: Parameter {} set to unknown value {}, assuming standard status code",
            SlingPostConstants.RP_STATUS);
        return true;
    }

    // ------ The methods below are based on the private methods from the
    // ModifyOperation class -----

    /**
     * Collects the properties that form the content to be written back to the
     * repository. NOTE: In the returned map, the key is the property name not a
     * path.
     * 
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    protected Map<String, RequestProperty> collectContent(
            SlingHttpServletRequest request, HtmlResponse response,
            String authorizablePath) {

        boolean requireItemPrefix = requireItemPathPrefix(request);

        // walk the request parameters and collect the properties
        Map<String, RequestProperty> reqProperties = new HashMap<String, RequestProperty>();
        for (Map.Entry<String, RequestParameter[]> e : request.getRequestParameterMap().entrySet()) {
            final String paramName = e.getKey();

            // do not store parameters with names starting with sling:post
            if (paramName.startsWith(SlingPostConstants.RP_PREFIX)) {
                continue;
            }
            // SLING-298: skip form encoding parameter
            if (paramName.equals("_charset_")) {
                continue;
            }
            // skip parameters that do not start with the save prefix
            if (requireItemPrefix && !hasItemPathPrefix(paramName)) {
                continue;
            }

            // ensure the paramName is an absolute property name
            String propPath;
            if (paramName.startsWith("./")) {
                propPath = paramName.substring(2);
            } else {
                propPath = paramName;
            }
            if (propPath.indexOf('/') != -1) {
                // only one path segment is valid here, so this paramter can't
                // be used.
                continue; // skip it.
            }
            
            propPath = authorizablePath + "/" + propPath;

            // @TypeHint example
            // <input type="text" name="./age" />
            // <input type="hidden" name="./age@TypeHint" value="long" />
            // causes the setProperty using the 'long' property type
            if (propPath.endsWith(SlingPostConstants.TYPE_HINT_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.TYPE_HINT_SUFFIX);

                final RequestParameter[] rp = e.getValue();
                if (rp.length > 0) {
                    prop.setTypeHintValue(rp[0].getString());
                }

                continue;
            }

            // @DefaultValue
            if (propPath.endsWith(SlingPostConstants.DEFAULT_VALUE_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.DEFAULT_VALUE_SUFFIX);

                prop.setDefaultValues(e.getValue());

                continue;
            }

            // SLING-130: VALUE_FROM_SUFFIX means take the value of this
            // property from a different field
            // @ValueFrom example:
            // <input name="./Text@ValueFrom" type="hidden" value="fulltext" />
            // causes the JCR Text property to be set to the value of the
            // fulltext form field.
            if (propPath.endsWith(SlingPostConstants.VALUE_FROM_SUFFIX)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath,
                    SlingPostConstants.VALUE_FROM_SUFFIX);

                // @ValueFrom params must have exactly one value, else ignored
                if (e.getValue().length == 1) {
                    String refName = e.getValue()[0].getString();
                    RequestParameter[] refValues = request.getRequestParameters(refName);
                    if (refValues != null) {
                        prop.setValues(refValues);
                    }
                }

                continue;
            }

            // SLING-458: Allow Removal of properties prior to update
            // @Delete example:
            // <input name="./Text@Delete" type="hidden" />
            // causes the JCR Text property to be deleted before update
            if (propPath.endsWith(SlingPostConstants.SUFFIX_DELETE)) {
                RequestProperty prop = getOrCreateRequestProperty(
                    reqProperties, propPath, SlingPostConstants.SUFFIX_DELETE);

                prop.setDelete(true);

                continue;
            }

            // SLING-455: @MoveFrom means moving content to another location
            // @MoveFrom example:
            // <input name="./Text@MoveFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by moving the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_MOVE_FROM)) {
                // don't support @MoveFrom here
                continue;
            }

            // SLING-455: @CopyFrom means moving content to another location
            // @CopyFrom example:
            // <input name="./Text@CopyFrom" type="hidden" value="/tmp/path" />
            // causes the JCR Text property to be set by copying the /tmp/path
            // property to Text.
            if (propPath.endsWith(SlingPostConstants.SUFFIX_COPY_FROM)) {
                // don't support @CopyFrom here
                continue;
            }

            // plain property, create from values
            RequestProperty prop = getOrCreateRequestProperty(reqProperties,
                propPath, null);
            prop.setValues(e.getValue());
        }

        return reqProperties;
    }

    /**
     * Returns the request property for the given property path. If such a
     * request property does not exist yet it is created and stored in the
     * <code>props</code>.
     * 
     * @param props The map of already seen request properties.
     * @param paramPath The absolute path of the property including the
     *            <code>suffix</code> to be looked up.
     * @param suffix The (optional) suffix to remove from the
     *            <code>paramName</code> before looking it up.
     * @return The {@link RequestProperty} for the <code>paramName</code>.
     */
    private RequestProperty getOrCreateRequestProperty(
            Map<String, RequestProperty> props, String paramPath, String suffix) {
        if (suffix != null && paramPath.endsWith(suffix)) {
            paramPath = paramPath.substring(0, paramPath.length()
                - suffix.length());
        }

        RequestProperty prop = props.get(paramPath);
        if (prop == null) {
            prop = new RequestProperty(paramPath);
            props.put(paramPath, prop);
        }

        return prop;
    }

    /**
     * Removes all properties listed as {@link RequestProperty#isDelete()} from
     * the authorizable.
     * 
     * @param authorizable The
     *            <code>org.apache.jackrabbit.api.security.user.Authorizable</code>
     *            that should have properties deleted.
     * @param reqProperties The map of request properties to check for
     *            properties to be removed.
     * @param toSave 
     * @param response The <code>HtmlResponse</code> to be updated with
     *            information on deleted properties.
     * @throws RepositoryException Is thrown if an error occurrs checking or
     *             removing properties.
     */
    protected void processDeletes(Authorizable resource,
            Map<String, RequestProperty> reqProperties,
            List<Modification> changes, Map<String, Object> toSave)  {

        for (RequestProperty property : reqProperties.values()) {
            if (property.isDelete()) {
                if (resource.hasProperty(property.getName())) {
                    resource.removeProperty(property.getName());
                    toSave.put(resource.getId(), resource);
                    changes.add(Modification.onDeleted(property.getPath()));
                }
            }
        }
    }

    /**
     * Writes back the content
     * @param toSave 
     * 
     * @throws RepositoryException if a repository error occurs
     * @throws ServletException if an internal error occurs
     */
    protected void writeContent(Session session, Authorizable authorizable,
            Map<String, RequestProperty> reqProperties,
            List<Modification> changes, Map<String, Object> toSave) {

        for (RequestProperty prop : reqProperties.values()) {
            if (prop.hasValues()) {
                // skip jcr special properties
                if (prop.getName().equals("jcr:primaryType")
                    || prop.getName().equals("jcr:mixinTypes")) {
                    continue;
                }
                if (authorizable instanceof Group) {
                    if (prop.getName().equals("groupId")) {
                        // skip these
                        continue;
                    }
                } else {
                    if (prop.getName().equals("userId")
                        || prop.getName().equals("pwd")
                        || prop.getName().equals("pwdConfirm")) {
                        // skip these
                        continue;
                    }
                }
                if (prop.isFileUpload()) {
                    // don't handle files for user properties for now.
                    continue;
                    // uploadHandler.setFile(parent, prop, changes);
                } else {
                    setPropertyAsIs(session, authorizable, prop, changes, toSave);
                }
            }
        }
    }

    /**
     * set property without processing, except for type hints
     * 
     * @param parent the parent node
     * @param prop the request property
     * @param toSave 
     * @throws RepositoryException if a repository error occurs.
     */
    private void setPropertyAsIs(Session session, Authorizable parent,
            RequestProperty prop, List<Modification> changes, Map<String, Object> toSave) {

        String parentPath = "a:"+parent.getId();
        // no explicit typehint
        Type type = Type.UNDEFINED;
        if (prop.getTypeHint() != null) {
            try {
                type = LitePropertyType.create(prop.getTypeHint());
            } catch (Exception e) {
                // ignore
            }
        }

        String[] values = prop.getStringValues();
        if (values == null) {
            // remove property
            boolean removedProp = removePropertyIfExists(parent, prop.getName());
            if (removedProp) {
                toSave.put(parent.getId(),parent);
                changes.add(Modification.onDeleted(parentPath + "/"
                    + prop.getName()));
            }
        } else if (values.length == 0) {
            // do not create new prop here, but clear existing
            if (parent.hasProperty(prop.getName())) {
              parent.setProperty(prop.getName(), "");
              toSave.put(parent.getId(),parent);
              changes.add(Modification.onModified(parentPath + "/"
                    + prop.getName()));
            }
        } else if (values.length == 1) {
            boolean removedProp = removePropertyIfExists(parent, prop.getName());
            // if the provided value is the empty string, we don't have to do
            // anything.
            if (values[0].length() == 0) {
                if (removedProp) {
                  toSave.put(parent.getId(),parent);
                    changes.add(Modification.onDeleted(parentPath + "/"
                        + prop.getName()));
                }
            } else {
                // modify property
                if (type == Type.DATE) {
                    // try conversion
                    Calendar c = dateParser.parse(values[0]);
                    if (c != null) {
                      
                          parent.setProperty(prop.getName(), c);
                          toSave.put(parent.getId(),parent);
                         changes.add(Modification.onModified(parentPath
                                + "/" + prop.getName()));
                        return;
                    }
                    // fall back to default behaviour
                }
                toSave.put(parent.getId(),parent);
                parent.setProperty(prop.getName(), values[0]);
            }
        } else {
            removePropertyIfExists(parent, prop.getName());
            if (type == Type.DATE) {
                // try conversion
                Calendar[] c = dateParser.parse(values);
                if (c != null) {
                    parent.setProperty(prop.getName(), c);
                    toSave.put(parent.getId(),parent);
                    changes.add(Modification.onModified(parentPath + "/"
                        + prop.getName()));
                    return;
                }
                // fall back to default behaviour
            }

            parent.setProperty(prop.getName(), values);
            toSave.put(parent.getId(),parent);
            changes.add(Modification.onModified(parentPath + "/"
                + prop.getName()));
        }

    }

    /**
     * Removes the property with the given name from the parent resource if it
     * exists.
     * 
     * @param parent the parent resource
     * @param name the name of the property to remove
     * @return path of the property that was removed or <code>null</code> if it
     *         was not removed
     * @throws RepositoryException if a repository error occurs.
     */
    private boolean removePropertyIfExists(Authorizable resource, String name) {
        if (resource.getProperty(name) != null) {
            resource.removeProperty(name);
            return true;
        }
        return false;
    }

    // ------ These methods were copied from AbstractSlingPostOperation ------

    /**
     * Returns the path of the resource of the request as the item path.
     * <p>
     * This method may be overwritten by extension if the operation has
     * different requirements on path processing.
     */
    protected String getItemPath(SlingHttpServletRequest request) {
        return request.getResource().getPath();
    }


    /**
     * Returns <code>true</code> if the <code>name</code> starts with either of
     * the prefixes {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT
     * <code>./</code>}, {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT
     * <code>../</code>} and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE
     * <code>/</code>}.
     */
    protected boolean hasItemPathPrefix(String name) {
        return name.startsWith(SlingPostConstants.ITEM_PREFIX_ABSOLUTE)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT)
            || name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_PARENT);
    }

    /**
     * Returns true if any of the request parameters starts with
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>}.
     * In this case only parameters starting with either of the prefixes
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_CURRENT <code>./</code>},
     * {@link SlingPostConstants#ITEM_PREFIX_RELATIVE_PARENT <code>../</code>}
     * and {@link SlingPostConstants#ITEM_PREFIX_ABSOLUTE <code>/</code>} are
     * considered as providing content to be stored. Otherwise all parameters
     * not starting with the command prefix <code>:</code> are considered as
     * parameters to be stored.
     */
    protected final boolean requireItemPathPrefix(
            SlingHttpServletRequest request) {

        boolean requirePrefix = false;

        Enumeration<?> names = request.getParameterNames();
        while (names.hasMoreElements() && !requirePrefix) {
            String name = (String) names.nextElement();
            requirePrefix = name.startsWith(SlingPostConstants.ITEM_PREFIX_RELATIVE_CURRENT);
        }

        return requirePrefix;
    }
    
    protected void dumpToSave(Map<String, Object> toSave, String message) throws AccessDeniedException, StorageClientException {
      if ( LOGGER.isDebugEnabled() ) {
        LOGGER.debug("At [{}], Save List Contains {} objects ",message, toSave.size());
        for ( Object o : toSave.values()) {
          if (o instanceof Group ) {
            Group g = (Group)o;
            LOGGER.debug(" Would Save {} {} {} {} {}",new Object[]{o, g.getPropertiesForUpdate(), Arrays.toString(g.getMembers()),  Arrays.toString(g.getMembersAdded()),  Arrays.toString(g.getMembersRemoved())});
          } else if (o instanceof User ) {
            LOGGER.debug(" Would Save {} {} ",o,((User)o).getPropertiesForUpdate());
          } else if ( o instanceof Content ) {
            LOGGER.debug(" Would Save {} {} ",o,((Content)o).getProperties());
          } else {
            LOGGER.debug(" Skipping {} ",o);
          }
        }
      }
    }

    protected void saveAll(Session session, Map<String, Object> toSave) throws AccessDeniedException, StorageClientException {
      ContentManager contentManager = session.getContentManager();
      AuthorizableManager authorizableManager = session.getAuthorizableManager();
      for ( Object o : toSave.values()) {
        if (o instanceof Group ) {
          Group g = (Group)o;
          if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Saving {} {} {} {} {}",new Object[]{o,g.getPropertiesForUpdate(), Arrays.toString(g.getMembers()),  Arrays.toString(g.getMembersAdded()),  Arrays.toString(g.getMembersRemoved())});
          }
          // we mustn't overwrite newer properties with ours
          Authorizable auth = authorizableManager.findAuthorizable(((Group) o).getId());
          if (auth != null) {
            Long storageLastModified = (Long) auth.getProperty(Authorizable.LASTMODIFIED_FIELD);
            Long mineLastModified = (Long) g.getProperty(Authorizable.LASTMODIFIED_FIELD);
            if (storageLastModified != null && (storageLastModified > mineLastModified)) {
              g.setProperty("sakai:group-title", auth.getProperty("sakai:group-title"));
              g.setProperty("sakai:group-description", auth.getProperty("sakai:group-description"));
            }
          }
          authorizableManager.updateAuthorizable((Group) o);
        } else if (o instanceof User ) {
          if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Saving {} {} ",o,((User)o).getPropertiesForUpdate());
          }
          authorizableManager.updateAuthorizable((User) o);
        } else if ( o instanceof Content ) {
          if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Saving {} {} ",o,((Content)o).getProperties());
          }
          contentManager.update((Content) o);
        } else {
          if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Skipping {} ",o);
          }
        }
      }
    }


}
