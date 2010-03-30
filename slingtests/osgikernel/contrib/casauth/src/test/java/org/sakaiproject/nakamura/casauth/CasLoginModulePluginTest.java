package org.sakaiproject.nakamura.casauth;

import javax.security.auth.login.FailedLoginException;

import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.junit.Before;
import org.junit.Test;

public class CasLoginModulePluginTest {
  private CasLoginModulePlugin clmp;

  @Before
  public void setup() {
    clmp = new CasLoginModulePlugin();
  }

  @Test
  public void testCanHandleSimpleCreds() {
    SimpleCredentials sc = new SimpleCredentials("foo", new char[0]);
    assertTrue(clmp.canHandle(sc));
  }

  @Test
  public void testCantHandleNonSimpleCreds() {
    class TestCredentials implements Credentials {
      private static final long serialVersionUID = 1L;
    }
    ;
    TestCredentials tc = new TestCredentials();
    assertFalse(clmp.canHandle(tc));
  }

  @Test
  public void testGetPrincipal() {
    SimpleCredentials sc = new SimpleCredentials("foo", new char[0]);
    assertEquals("foo", clmp.getPrincipal(sc).getName());
  }

  @Test
  public void testImpersonate() throws FailedLoginException {
    assertEquals(LoginModulePlugin.IMPERSONATION_DEFAULT, clmp.impersonate(null, null));
  }
}
