package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SparseComponentHolder {

	private static final Logger LOGGER = LoggerFactory.getLogger(SparseComponentHolder.class);
	private static Repository sparseRepositoryInstance;
	@Reference
	private Repository repository;
	@Activate
	public void activate(Map<String, Object> properties) {
		SparseComponentHolder.setSparseRespository(repository);
	}
	@Deactivate
	public void deactivate(Map<String, Object> properties) {
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
