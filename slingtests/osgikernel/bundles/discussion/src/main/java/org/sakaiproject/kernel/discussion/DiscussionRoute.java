package org.sakaiproject.kernel.discussion;

import org.sakaiproject.kernel.api.message.MessageRoute;
import org.sakaiproject.kernel.util.StringUtils;

public class DiscussionRoute implements MessageRoute {

  private static final String INTERNAL = "internal";
  private String transport;
  private String rcpt;

  // TODO Just use MessageRouteImpl ?
  /**
   * @param r
   */
  public DiscussionRoute(String r) {
    String[] routing = StringUtils.split(r, ':', 2);
    if (routing == null || routing.length == 0) {
      transport = null;
      rcpt = null;
    } else if (routing.length == 1) {
      transport = INTERNAL;
      rcpt = routing[0];
    } else {
      transport = routing[0];
      rcpt = routing[1];
    }
  }

  /**
   * @return the transport
   */
  public String getTransport() {
    return transport;
  }

  /**
   * @return the rcpt
   */
  public String getRcpt() {
    return rcpt;
  }

}
