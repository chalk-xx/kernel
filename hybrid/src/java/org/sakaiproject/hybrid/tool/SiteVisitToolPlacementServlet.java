/**
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
package org.sakaiproject.hybrid.tool;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.portal.charon.ToolHelperImpl;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;

/**
 * Based on
 * https://source.caret.cam.ac.uk/camtools/trunk/camtools/sdata/tool/sakai
 * -sdata-impl/src/main/java/org/sakaiproject/sdata/services/site/SiteBean.java
 * <p>
 * Requires one getParameter: siteId.
 * <p>
 * Servlet runs in the context of the current user, so they must have access to
 * the siteId specified. Normal HTTP error codes to expect are:
 * HttpServletResponse.SC_NOT_FOUND for an invalid siteId, or
 * HttpServletResponse.SC_FORBIDDEN if the current user does not have permission
 * to access the specified site.
 */
public class SiteVisitToolPlacementServlet extends HttpServlet {
	private static final long serialVersionUID = -1182601175544873164L;
	private static final Log LOG = LogFactory
			.getLog(SiteVisitToolPlacementServlet.class);
	private static final String SITE_ID = "siteId";

	private SessionManager sessionManager;
	private SiteService siteService;
	private ToolHelperImpl toolHelper = new ToolHelperImpl();

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doGet(HttpServletRequest " + req
					+ ", HttpServletResponse " + resp + ")");
		}
		// ensure siteId getParameter
		final String siteId = req.getParameter(SITE_ID);
		if (siteId == null || "".equals(siteId)) {
			if (!resp.isCommitted()) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			} else {
				throw new IllegalAccessError(
						"HttpServletResponse.SC_BAD_REQUEST");
			}
		}
		// current user
		final String principal = sessionManager.getCurrentSession()
				.getUserEid();
		// 1) get the Site object for siteId
		// 2) ensure user has access to Site via SiteService.getSiteVisit()
		Site site = null;
		try {
			site = siteService.getSiteVisit(siteId);
		} catch (IdUnusedException e) {
			LOG.warn("Site not found: " + siteId, e);
			sendError(resp, HttpServletResponse.SC_NOT_FOUND,
					"HttpServletResponse.SC_NOT_FOUND: " + siteId);
		} catch (PermissionException e) {
			LOG.warn("Permission denied: " + principal
					+ " could not access site " + siteId, e);
			sendError(resp, HttpServletResponse.SC_FORBIDDEN,
					"HttpServletResponse.SC_FORBIDDEN");
		}
		if (site != null) { // normal program flow
			final JSONObject json = new JSONObject();
			json.element("principal", sessionManager.getCurrentSession()
					.getUserEid());
			final JSONObject siteJson = new JSONObject();
			siteJson.element("title", site.getTitle());
			siteJson.element("id", site.getId());
			siteJson.element("icon", site.getIconUrlFull());
			siteJson.element("skin", site.getSkin());
			siteJson.element("type", site.getType());
			// get the list of site pages
			List<SitePage> pages = site.getOrderedPages();
			int number = 0;
			if (pages != null && canAccessAtLeastOneTool(site)) {
				final JSONArray pagesArray = new JSONArray();
				for (SitePage page : pages) { // for each page
					final JSONObject pageJson = new JSONObject();
					pageJson.element("id", page.getId());
					pageJson.element("name", page.getTitle());
					pageJson.element("layout", page.getLayout());
					pageJson.element("number", ++number);
					pageJson.element("popup", page.isPopUp());
					// get list of tools for the page
					List<ToolConfiguration> tools = page.getTools();
					if (tools != null && !tools.isEmpty()) {
						pageJson.element("iconclass", "icon-"
								+ tools.get(0).getToolId().replaceAll("[.]",
										"-"));
						final JSONArray toolsArray = new JSONArray();
						for (ToolConfiguration toolConfig : tools) {
							// for each toolConfig
							if (toolHelper.allowTool(site, toolConfig)) {
								final JSONObject toolJson = new JSONObject();
								toolJson.element("url", toolConfig.getId());
								final Tool tool = toolConfig.getTool();
								if (tool != null && tool.getId() != null) {
									toolJson.element("title", tool.getTitle());
									toolJson.element("layouthint", toolConfig
											.getLayoutHints());
								} else {
									toolJson.element("title", page.getTitle());
								}
								toolsArray.add(toolJson);
							}
						}
						pageJson.element("tools", toolsArray);
					}
					pagesArray.add(pageJson);
				}
				siteJson.element("pages", pagesArray);
			}
			json.element("site", siteJson);
			json.write(resp.getWriter());
		} else {
			sendError(resp, HttpServletResponse.SC_NOT_FOUND,
					"HttpServletResponse.SC_NOT_FOUND: " + siteId);
		}
	}

	/**
	 * Loops through all of the site pages and checks to see if the current user
	 * can access at least one of those tools.
	 * 
	 * @param site
	 * @return
	 */
	private boolean canAccessAtLeastOneTool(Site site) {
		List<SitePage> pages = site.getOrderedPages();
		if (pages != null) {
			for (SitePage page : pages) {
				List<ToolConfiguration> tools = page.getTools();
				for (ToolConfiguration tool : tools) {
					if (toolHelper.allowTool(site, tool)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Simple little wrapper for HttpServletResponse.sendError - just to improve
	 * readability of main-line code.
	 * 
	 * @param resp
	 * @param errorCode
	 * @param message
	 * @throws IOException
	 */
	private void sendError(HttpServletResponse resp, int errorCode,
			String message) throws IOException {
		if (!resp.isCommitted()) {
			resp.sendError(errorCode);
			return;
		} else {
			throw new Error(message);
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		sessionManager = org.sakaiproject.tool.cover.SessionManager
				.getInstance();
		siteService = org.sakaiproject.site.cover.SiteService.getInstance();
	}
}
