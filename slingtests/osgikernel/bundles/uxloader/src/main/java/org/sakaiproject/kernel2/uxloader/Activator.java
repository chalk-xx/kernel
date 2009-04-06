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
package org.sakaiproject.kernel2.uxloader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/* Load config from (decreasing priority)
 * 1. OSGI service config parameter/url
 * 2. config service parameter /url
 * 3. cwd/url 
 * 
 * if parameter then has format url:path;url:path;...
 */

public class Activator extends SimpleBaseActivator implements BundleActivator {	
	
	public Activator() {
		registerStartupService(ConfigurationService.class);
		registerStartupService(HttpService.class);
	}
	
	private URLFileMapping[] createMapping(String raw) {
		logger.info("raw ux config is "+raw);
		List<URLFileMapping> out=new ArrayList<URLFileMapping>();
		for(String part : raw.split(";")) {
			String[] parts=part.split(":");
			if(parts.length!=2)
				continue;
			out.add(new URLFileMapping(parts[0].trim(),parts[1].trim()));
		}
		return out.toArray(new URLFileMapping[0]);
	}
	
	private List<URLFileMapping> loadMappings(BundleContext bc,ConfigurationService config) {
		List<URLFileMapping> out=new ArrayList<URLFileMapping>();
		String raw_config=config.getProperty("uxloader.config");
		for(URLFileMapping mapping : createMapping(raw_config)) {
			out.add(new URLFileMapping(mapping.getURL(),mapping.getFileSystem()));
		}
		return out;
	}
	
	public void go(BundleContext bc) throws Exception {
		HttpService http=(HttpService)getService(bc,HttpService.class);
		ConfigurationService config=(ConfigurationService)getService(bc,ConfigurationService.class);
		ActivationProcess activation=new ActivationProcess(new ServiceResolver(bc),http);
		for(URLFileMapping m : loadMappings(bc,config))
			activation.map(m.getURL(),m.getFileSystem());
	}
}
