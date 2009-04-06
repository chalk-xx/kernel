package org.sakaiproject.kernel2.uxloader;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleBaseActivator implements BundleActivator {
	private Map<Class<?>,ServiceReference> services=new HashMap<Class<?>,ServiceReference>();
	private Map<Class<?>,ServiceReference[]> mservices=new HashMap<Class<?>,ServiceReference[]>();
	protected static final Logger logger = LoggerFactory.getLogger(Activator.class);

	
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

	protected Object[] getServices(BundleContext bc,Class<?> klass) throws InvalidSyntaxException {
	    ServiceReference[] refs=mservices.get(klass);
	    if(refs==null) {
	    	logger.info("Getting service reference for "+klass.getName());
	    	refs=bc.getServiceReferences(klass.getName(),"");
	    	mservices.put(klass,refs);
	    }
	    Object[] out=new Object[refs.length];
	    for(int i=0;i<out.length;i++)
	    	out[i]=bc.getService(refs[i]);
	    logger.info("Got "+out);
	    return out;
	}
	
	public void start(BundleContext bc) throws Exception {
		go(bc);
	}

	public void stop(BundleContext bc) throws Exception {
		for(ServiceReference ref : services.values())
			bc.ungetService(ref);
		for(ServiceReference[] refs : mservices.values())
			for(ServiceReference ref : refs)
				bc.ungetService(ref);
	}
}
