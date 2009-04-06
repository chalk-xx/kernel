package org.sakaiproject.kernel2.uxloader;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceResolver {
	protected static final Logger logger = LoggerFactory.getLogger(ServiceResolver.class);
	
	private BundleContext bc;
	
	public ServiceResolver(BundleContext bc) { this.bc=bc; }
	
	public Object getService(Class klass) throws ServiceDisappearedException {
		ServiceReference ref=bc.getServiceReference(klass.getName());
	    if(ref==null) {
	    	logger.warn("Service disappeared: "+klass.getCanonicalName());
	    	throw new ServiceDisappearedException("Service disappeared: "+klass.getCanonicalName());
	    }
	    Object out=bc.getService(ref);
	    logger.info("Got "+out);
	    return out;

	}
}
