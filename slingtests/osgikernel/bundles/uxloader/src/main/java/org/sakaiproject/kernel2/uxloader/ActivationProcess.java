package org.sakaiproject.kernel2.uxloader;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivationProcess {
	private static final Logger logger = LoggerFactory.getLogger(FileServlet.class);
	
	private HttpService http;
	private HttpContext context;
	private CacheManagerService cache;
	
	public ActivationProcess(HttpService http,CacheManagerService cache) {
		this.http=http;
		this.context=http.createDefaultHttpContext();
		this.cache=cache;
	}
	
	public void map(String url,String filesystem) throws ServletException, NamespaceException {
		logger.info("Mapping url="+url+" to filesystem="+filesystem);
		Dictionary<String, String> uxLoaderParams = new Hashtable<String, String>();
        uxLoaderParams.put(FileServlet.BASE_FILE,filesystem);
        uxLoaderParams.put(FileServlet.MAX_CACHE_SIZE, "102400");
        uxLoaderParams.put(FileServlet.WELCOME_FILE, "index.html");
        http.registerServlet(url, new FileServlet(cache),uxLoaderParams,context);
	}
}
