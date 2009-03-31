package org.sakaiproject.kernel2.osgi.guiceuser;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceA;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceB;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceD;
import org.sakaiproject.kernel2.osgi.guiceuser.api.InterfaceE;
import org.sakaiproject.kernel2.osgi.guiceuser.impl.ConcreteA;
import org.sakaiproject.kernel2.osgi.guiceuser.impl.ConcreteB;
import org.sakaiproject.kernel2.osgi.guiceuser.impl.ConcreteD;
import org.sakaiproject.kernel2.osgi.guiceuser.impl.ConcreteE;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(InterfaceA.class).to(ConcreteA.class).in(Scopes.SINGLETON);
    bind(InterfaceB.class).to(ConcreteB.class).in(Scopes.SINGLETON);
    bind(InterfaceD.class).to(ConcreteD.class).in(Scopes.SINGLETON);
    bind(InterfaceE.class).to(ConcreteE.class).in(Scopes.SINGLETON);
  }

}
