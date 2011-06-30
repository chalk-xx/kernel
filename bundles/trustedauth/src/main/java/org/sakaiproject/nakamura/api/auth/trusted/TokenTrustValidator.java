package org.sakaiproject.nakamura.api.auth.trusted;

import javax.servlet.http.HttpServletRequest;

/**
 * Validates if the Token is suitable for this request.
 */
public interface TokenTrustValidator {

  boolean isTrusted(HttpServletRequest request);
}
