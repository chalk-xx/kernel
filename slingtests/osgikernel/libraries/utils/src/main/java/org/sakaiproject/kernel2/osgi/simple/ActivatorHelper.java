package org.sakaiproject.kernel2.osgi.simple;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivatorHelper {
	private static final Logger logger = LoggerFactory.getLogger(ActivatorHelper.class);	
	private static Map<Long,ServiceResolver> resolvers=new HashMap<Long,ServiceResolver>();
	
	private static class GoInvoker implements Runnable {
		private BundleContext bc;
		private DelayedActivation activator;
		private Class<?>[] startup_services;
		
		private GoInvoker(DelayedActivation activator,BundleContext bc,Class<?>[] svcs) {
			this.bc=bc;
			this.activator=activator;
			startup_services=svcs;
		}
		
		private boolean tryService(Class<?> klass) {
			logger.info("Getting service reference for "+klass.getName());
			ServiceReference ref=bc.getServiceReference(klass.getName());
			if(ref==null) {
				logger.error("Cannot find service for "+klass.getName());
				return false;
			}
			Object out=bc.getService(ref);
			logger.info("object="+out);
			if(out==null)
				return false;
			bc.ungetService(ref);
			return true;
		}
		
		private boolean is_good() {
			for(Class<?> svc : startup_services) {
				if(!tryService(svc)) {
					logger.info("Cannot start "+activator.getClass().getCanonicalName()+
							    " yet : "+svc.getCanonicalName()+" not loaded.");
					return false;
				}
			}
			logger.info("All services ready: starting "+activator.getClass().getCanonicalName());
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
				ServiceResolver outer=getLinkedServiceResolver(bc);
				ServiceResolver request=new ServiceResolver(outer);
				try {
					activator.go(bc,outer,request);
				} finally {
					request.stop();
				}
			} catch (Exception x) {
				logger.error("Could not start service ",x);
			}
		}
		
	}

	public static void startServices(Class<?>[] required,BundleContext bc,DelayedActivation act) {
		Thread t=new Thread(new GoInvoker(act,bc,required));
		t.start();
	}
	
	/* Synchronizing here isn't a problem because these methods are only called off of the critical path:
	 * at service start and stop time
	 */
	private static synchronized ServiceResolver getLinkedServiceResolver(BundleContext bc) {
		ServiceResolver rs=resolvers.get(bc.getBundle().getBundleId());
		if(rs!=null)
			return rs;
		rs=new ServiceResolver(bc);
		resolvers.put(bc.getBundle().getBundleId(),rs);
		return rs;
	}
	
	public static synchronized void stopServices(BundleContext bc) {
		long id=bc.getBundle().getBundleId();
		ServiceResolver rs=resolvers.get(id);
		resolvers.remove(id); // Important as bundle context is no longer valid
		if(rs!=null)
			rs.stop();
	}
}
