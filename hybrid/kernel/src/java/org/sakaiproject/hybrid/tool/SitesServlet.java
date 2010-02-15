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
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;

/**
 * Based on
 * https://source.caret.cam.ac.uk/camtools/trunk/camtools/sdata/tool/sakai
 * -sdata-impl/src/main/java/org/sakaiproject/sdata/services/mcp/
 * MyCoursesAndProjectsBean.java
 * <p>
 * No required get parameters. Runs in the context of the current user. Returns
 * all sites that the user has access to visit.
 */
public class SitesServlet extends HttpServlet {
	private static final long serialVersionUID = 7907409301065984518L;
	private static final Log LOG = LogFactory.getLog(SitesServlet.class);
	private SessionManager sessionManager;
	private SiteService siteService;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("doGet(HttpServletRequest " + req
					+ ", HttpServletResponse " + resp + ")");
		}
		// sites for current user
		final JSONObject json = new JSONObject();
		json.element("principal", sessionManager.getCurrentSession()
				.getUserEid());
		final List<Site> siteList = siteService.getSites(
				org.sakaiproject.site.api.SiteService.SelectionType.ACCESS,
				null, null, null,
				org.sakaiproject.site.api.SiteService.SortType.TITLE_ASC, null);
		if (siteList != null) {
			final JSONArray sites = new JSONArray();
			for (Site site : siteList) {
				final JSONObject siteJson = new JSONObject();
				siteJson.element("title", site.getTitle());
				siteJson.element("id", site.getId());
				siteJson.element("url", site.getUrl());
				siteJson.element("iconUrl", site.getIconUrl());
				siteJson.element("owner", site.getCreatedBy().getDisplayName());
				siteJson.element("members", site.getMembers().size());
				siteJson.element("description", site.getDescription());
				siteJson.element("siteType", site.getType());
				// TODO ISO8601 date format or other?
				siteJson.element("creationDate", new SimpleDateFormat(
						"yyyy-MM-dd").format(new Date(site.getCreatedTime()
						.getTime())));
				sites.add(siteJson);
			}
			json.element("sites", sites);
		}
		json.write(resp.getWriter());
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		sessionManager = org.sakaiproject.tool.cover.SessionManager
				.getInstance();
		if (sessionManager == null) {
			throw new IllegalStateException("SessionManager == null");
		}
		siteService = org.sakaiproject.site.cover.SiteService.getInstance();
		if (siteService == null) {
			throw new IllegalStateException("SiteService == null");
		}
	}
}
