package org.sakaiproject.kernel2.uxloader;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.kernel.api.configuration.ConfigurationService;
import org.sakaiproject.kernel2.osgi.simple.ServiceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivationProcess {
	private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);
	
	private HttpService http;
	private HttpContext context;
	private ServiceResolver resolver;
	
	public ActivationProcess(ServiceResolver resolver,HttpService http) {
		this.http=http;
		this.context=http.createDefaultHttpContext();
		this.resolver=resolver;
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
	
	void loadMappings(BundleContext bc,ConfigurationService config) throws ServletException, NamespaceException {
		List<URLFileMapping> out=new ArrayList<URLFileMapping>();
		String raw_config=config.getProperty("uxloader.config");
		for(URLFileMapping mapping : createMapping(raw_config)) {
			out.add(new URLFileMapping(mapping.getURL(),mapping.getFileSystem()));
		}
		for(URLFileMapping m : out)
			map(m.getURL(),m.getFileSystem());
	}
	
	private void map(String url,String filesystem) throws ServletException, NamespaceException {
		logger.info("Mapping url="+url+" to filesystem="+filesystem);
		Dictionary<String, String> uxLoaderParams = new Hashtable<String, String>();
        uxLoaderParams.put(FileServlet.BASE_FILE,filesystem);
        uxLoaderParams.put(FileServlet.MAX_CACHE_SIZE, "102400");
        uxLoaderParams.put(FileServlet.WELCOME_FILE, "index.html");
        http.registerServlet(url,new FileServlet(resolver),uxLoaderParams,context);
	}
	
	
}
