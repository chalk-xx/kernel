package org.apache.felix.http.jetty.internal;

import org.apache.felix.http.base.internal.AbstractHttpActivator;

public class NakamuraActivator extends AbstractHttpActivator {

  private NakamuraJettyService jetty;

  protected void doStart()
      throws Exception
  {
      super.doStart();
      this.jetty = new NakamuraJettyService(getBundleContext(), getDispatcherServlet(), getEventDispatcher(),
          getHttpServiceController());
      this.jetty.start();
  }

  protected void doStop()
      throws Exception
  {
      this.jetty.stop();
      super.doStop();
  }

}
