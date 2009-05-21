package org.sakaiproject.kernel.guiceuser;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import org.sakaiproject.kernel.guiceuser.api.InterfaceA;
import org.sakaiproject.kernel.guiceuser.api.InterfaceB;
import org.sakaiproject.kernel.guiceuser.api.InterfaceD;
import org.sakaiproject.kernel.guiceuser.api.InterfaceE;
import org.sakaiproject.kernel.guiceuser.impl.ConcreteA;
import org.sakaiproject.kernel.guiceuser.impl.ConcreteB;
import org.sakaiproject.kernel.guiceuser.impl.ConcreteD;
import org.sakaiproject.kernel.guiceuser.impl.ConcreteE;
import org.sakaiproject.kernel.guiceuser.impl.FProvider;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(InterfaceA.class).to(ConcreteA.class).in(Scopes.SINGLETON);
    bind(InterfaceB.class).to(ConcreteB.class).in(Scopes.SINGLETON);
    bind(InterfaceD.class).to(ConcreteD.class).in(Scopes.SINGLETON);
    bind(InterfaceE.class).to(ConcreteE.class).in(Scopes.SINGLETON);
    bind(FProvider.class);
  }

}
