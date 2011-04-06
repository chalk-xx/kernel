package org.sakaiproject.nakamura.api.http.usercontent;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Server protection Veto implementations can prevent a resource from streaming regardless
 * of what the ServerProtectionService deduces. Implementing this interface is a last
 * resort for streaming servlets that dont expose normal request to url mapping semantics.
 */
public interface ServerProtectionVeto {

  /**
   * returns true if the implementation is going to veto the request.
   * @param srequest
   * @return
   */
  boolean willVeto(SlingHttpServletRequest srequest);

  /**
   * @param srequest
   * @return the vetoed decision on the request, true will stream, false will not.
   */
  boolean safeToStream(SlingHttpServletRequest srequest);

}
