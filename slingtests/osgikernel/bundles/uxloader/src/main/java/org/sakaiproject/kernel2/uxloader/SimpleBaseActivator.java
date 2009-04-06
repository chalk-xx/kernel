package org.sakaiproject.kernel2.uxloader;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleBaseActivator implements BundleActivator {
	private Map<Class<?>,ServiceReference> services=new HashMap<Class<?>,ServiceReference>();
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	
	public abstract void go(BundleContext bc) throws Exception;
	
	protected Object getService(BundleContext bc,Class<?> klass) {
	    ServiceReference ref=services.get(klass);
	    if(ref==null) {
	    	logger.info("Getting service reference for "+klass.getName());
	    	ref=bc.getServiceReference(klass.getName());
	    	services.put(klass,ref);
	    }
	    Object out=bc.getService(ref);
	    logger.info("Got "+out);
	    return out;
	}
	
	public void start(BundleContext bc) throws Exception {
		go(bc);
	}

	public void stop(BundleContext bc) throws Exception {
		for(ServiceReference ref : services.values())
			bc.ungetService(ref);
	}
}
