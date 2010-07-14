package org.sakaiproject.nakamura.rules;

import org.sakaiproject.nakamura.api.rules.RulePackageLoader;

import java.io.InputStream;

public class BundleLoaderRuleSet implements RulePackageLoader{

  @Override
  public InputStream getPackageInputStream() {
   return  this.getClass().getResourceAsStream("/SLING-INF/content/var/rules/org.sakaiproject.nakamura.rules/org.sakaiproject.nakamura.rules.example/0.7-SNAPSHOT/org.sakaiproject.nakamura.rules.example-0.7-SNAPSHOT.pkg");
  }

  @Override
  public ClassLoader getPackageClassLoader() {
    return this.getClass().getClassLoader();
  }

}
