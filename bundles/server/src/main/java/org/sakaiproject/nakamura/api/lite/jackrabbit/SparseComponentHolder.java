package org.sakaiproject.nakamura.api.lite.jackrabbit;

import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.SakaiActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Component manual creation
public class SparseComponentHolder {

	private static final Logger LOGGER = LoggerFactory.getLogger(SparseComponentHolder.class);
	private static Repository sparseRepositoryInstance;
	private SakaiActivator sakaiActivator;
	
	public void activate(ComponentContext componentContext) {
		BundleContext bundleContext = componentContext.getBundleContext();
		sakaiActivator = new SakaiActivator();
		sakaiActivator.start(bundleContext);
	}
	
	public void deactivate(ComponentContext componentContext) {
		BundleContext bundleContext = componentContext.getBundleContext();
		sakaiActivator.stop(bundleContext);
		sakaiActivator = null;
	}
	
	public void bindRepository(Repository repository) {
		SparseComponentHolder.setSparseRespository(repository);
	}
	public void unbindRepository(Repository repository) {
		SparseComponentHolder.setSparseRespository(null);
	}
	
	public static void setSparseRespository(Repository repository) {
		sparseRepositoryInstance = repository;
	}

	public static Repository getSparseRepositoryInstance() {
		if ( sparseRepositoryInstance == null ) {
			LOGGER.warn("No Sparse Repository availalbe at this time, has the SparseComponentHolder been activated ?");
		}
		return sparseRepositoryInstance;
	}
}
