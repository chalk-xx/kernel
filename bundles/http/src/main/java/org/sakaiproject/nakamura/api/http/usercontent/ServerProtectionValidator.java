package org.sakaiproject.nakamura.api.http.usercontent;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

/**
 * Services that implement this service may inspect request to indicate that the request
 * is theirs and should be sonsidered safe to stream.
 */
public interface ServerProtectionValidator {

  boolean safeToStream(SlingHttpServletRequest srequest, Resource resource);

}
