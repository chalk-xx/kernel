package org.sakaiproject.kernel.site.servlet;

import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

public class AbstractEasyMockTest {
  private List<Object> mocks;

  @Before
  public void setUp() throws Exception {
    mocks = new ArrayList<Object>();
  }

  protected <T> T createMock(Class<T> c) {
    T result = org.easymock.EasyMock.createMock(c);
    mocks.add(result);
    return result;
  }

  protected void replay() {
    org.easymock.EasyMock.replay(mocks.toArray());
  }

  protected void verify() {
    org.easymock.EasyMock.verify(mocks.toArray());
  }
  
}
