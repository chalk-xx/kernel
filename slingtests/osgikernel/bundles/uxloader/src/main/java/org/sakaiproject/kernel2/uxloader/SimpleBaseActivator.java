package org.sakaiproject.kernel2.uxloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleBaseActivator implements BundleActivator {
	protected static final Logger logger = LoggerFactory.getLogger(Activator.class);	
	
	private class GoInvoker implements Runnable {
		private BundleContext bc;
		private Class<?> outer;
		
		private GoInvoker(Class<?> outer,BundleContext bc) {
			this.bc=bc;
			this.outer=outer;
		}
		
		private boolean is_good() {
			for(Class<?> svc : startup_services) {
				Object out=getService(bc,svc);
				if(out==null) {
					logger.info("Cannot start "+outer.getCanonicalName()+" yet : "+svc.getCanonicalName());
					return false;
				}
			}
			logger.info("All services ready: starting "+outer.getCanonicalName());
			return true;
		}
		
		public synchronized void run() {
			int f1=1000,f2=1000;
			
			try {
				while(!is_good()) {
					Thread.sleep(f1);
					// Fibionacci backoff: exponential with base of golden-mean: slower than 2.
					int t=f1+f2;
					f1=f2;
					f2=t;
				}
				go(bc);
			} catch (Exception x) {
				logger.error("Could not start service ",x);
			}
		}
		
	}
	
	private Map<Class<?>,ServiceReference> services=new HashMap<Class<?>,ServiceReference>();
	private Map<Class<?>,ServiceReference[]> mservices=new HashMap<Class<?>,ServiceReference[]>();
	private List<Class<?>> startup_services=new ArrayList<Class<?>>();
	
	protected void registerStartupService(Class svc) {
		startup_services.add(svc);
	}
	
	public abstract void go(BundleContext bc) throws Exception;
	
	// XXX all accesses through Resolver, and thread safety
	protected Object getService(BundleContext bc,Class<?> klass) {
	    ServiceReference ref=services.get(klass);
	    if(ref==null) {
	    	logger.info("Getting service reference for "+klass.getName());
	    	ref=bc.getServiceReference(klass.getName());
	    	services.put(klass,ref);
	    }
	    if(ref==null) {
	    	logger.error("Cannot find service for "+klass.getName());
	    	return null;
	    }
	    Object out=bc.getService(ref);
	    logger.info("Got "+out);
	    return out;
	}
		
	public void start(BundleContext bc) throws Exception {
		Thread t=new Thread(new GoInvoker(this.getClass(),bc));
		t.start();
	}

	public void stop(BundleContext bc) throws Exception {
		for(ServiceReference ref : services.values())
			bc.ungetService(ref);
		for(ServiceReference[] refs : mservices.values())
			for(ServiceReference ref : refs)
				bc.ungetService(ref);
	}
}
