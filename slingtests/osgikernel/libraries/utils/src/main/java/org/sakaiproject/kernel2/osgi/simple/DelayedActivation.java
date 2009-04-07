package org.sakaiproject.kernel2.osgi.simple;

import org.osgi.framework.BundleContext;

public interface DelayedActivation {
	public void go(BundleContext bc,ServiceResolver bundle,ServiceResolver startup) throws Exception;
}
