package org.sakaiproject.kernel2.osgi.simple;

import java.util.Map;
import java.util.HashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceResolver {
	protected static final Logger logger = LoggerFactory.getLogger(ServiceResolver.class);
	
	private BundleContext bc;
	private Map<String,Object> services=new HashMap<String,Object>();
	private Map<String,ServiceReference> service_refs=new HashMap<String,ServiceReference>();
	
	public ServiceResolver(BundleContext bc) { this.bc=bc; }
	public ServiceResolver(ServiceResolver in) { this.bc=in.bc; }
	
	@SuppressWarnings("unchecked")
  public <T> T getService(Class<T> klass) throws ServiceDisappearedException {
		// Concurrency OK: if it's not null it won't change until shutdown.
		T out = (T)services.get(klass.getCanonicalName());
		if(out!=null) {
			logger.info("Getting cached service: "+klass.getCanonicalName());			
			return out;
		}
		// Synchronized okay here: block only called once per service used per bundle start/stop
		synchronized(this) {
			logger.info("Getting service from OSGi: "+klass.getCanonicalName());
			ServiceReference ref=bc.getServiceReference(klass.getName());
		    if(ref==null) {
		    	logger.warn("Service disappeared: "+klass.getCanonicalName());
		    	throw new ServiceDisappearedException("Service disappeared: "+klass.getCanonicalName());
		    }
		    out=(T) bc.getService(ref);
		    services.put(klass.getCanonicalName(),out);
		    service_refs.put(klass.getCanonicalName(),ref);
		    logger.info("Got "+out);
		    return out;
		}
	}
	
	// Synchronized okay here: only called once per bundle start/stop
	public synchronized void stop() {
		logger.info("Stopping services");
		for(Map.Entry<String,ServiceReference> ref : service_refs.entrySet()) {
			logger.info("Ungetting service to OSGi: "+ref.getKey());
			bc.ungetService(ref.getValue());
		}
	}
}
