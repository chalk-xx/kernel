package org.sakaiproject.nakamura.api.templates;

import org.apache.sling.api.request.RequestParameterMap;

import java.util.Collection;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: zach
 * Date: 1/5/11
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TemplateService {

  public String evaluateTemplate(Map parameters, String template);

  public Collection<String> missingTerms(Map parameters, String template);
}
