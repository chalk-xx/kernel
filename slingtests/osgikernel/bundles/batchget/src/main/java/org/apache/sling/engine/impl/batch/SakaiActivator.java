package org.apache.sling.engine.impl.batch;

import org.apache.sling.engine.impl.EngineBundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Dummy activator so we don't startup the same services.
 */
public class SakaiActivator extends EngineBundleActivator {

  
  @Override
  public void start(BundleContext context) throws Exception {
    // TODO Auto-generated method stub
    super.start(context);
  }
  
  @Override
  public void stop(BundleContext context) throws Exception {
    // TODO Auto-generated method stub
    super.stop(context);
  }
  
}
