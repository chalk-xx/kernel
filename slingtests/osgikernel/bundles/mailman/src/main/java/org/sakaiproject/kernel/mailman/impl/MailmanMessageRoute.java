package org.sakaiproject.kernel.mailman.impl;

import org.sakaiproject.kernel.api.message.MessageRoute;

public class MailmanMessageRoute implements MessageRoute {

  private String transport;
  private String rcpt;

  public MailmanMessageRoute(String rcpt, String transport) {
    this.rcpt = rcpt;
    this.transport = transport;
  }
  
  public String getRcpt() {
    return rcpt;
  }

  public String getTransport() {
    return transport;
  }

}
