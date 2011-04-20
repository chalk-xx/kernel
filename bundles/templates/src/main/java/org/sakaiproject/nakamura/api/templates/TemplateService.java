package org.sakaiproject.nakamura.api.templates;

import java.util.Collection;
import java.util.Map;

/**
 * Service to provide templating functionality for replacing variable markers in Strings.
 */
public interface TemplateService {

  /**
   * The VTL property name reserved for the ID generator map.
   */
  public String ID_GENERATOR = "_ids";

  String evaluateTemplate(Map<String, ? extends Object> parameters, String template);

  /**
   * Checks for unresolved variable markers in a processed template. Looks for ${param}
   * but does not look for $param.
   *
   * @param template Template to check
   * @return Collection of keys that were not resolved.
   */
  Collection<String> missingTerms(String template);

  /**
   * Checks for unresolved variable markers in a template. Looks for ${param} but does not
   * look for $param. Checks parameters to see if it can provide a value for any found
   * parameter keys.
   *
   * @param parameters Parameters to verify with
   * @param template Template to check
   * @return Collection of keys that were not resolvable.
   */
  Collection<String> missingTerms(Map<String, ? extends Object> parameters,
      String template);
}
