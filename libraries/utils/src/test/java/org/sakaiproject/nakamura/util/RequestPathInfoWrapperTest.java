package org.sakaiproject.nakamura.util;

import org.junit.Assert;
import org.junit.Test;

public class RequestPathInfoWrapperTest {

  @Test
  public void testRequest() {
    RequestPathInfoWrapper rpi = new RequestPathInfoWrapper("/testing/1/2/3");
    Assert.assertEquals("/testing/1/2/3", rpi.getResourcePath());
    Assert.assertNull(rpi.getExtension());
    Assert.assertNull(rpi.getSelectorString());
    Assert.assertArrayEquals(new String[0],rpi.getSelectors());
    
    rpi = new RequestPathInfoWrapper("/testing/1/2/3.");
    Assert.assertEquals("/testing/1/2/3", rpi.getResourcePath());
    Assert.assertNull(rpi.getExtension());
    Assert.assertNull(rpi.getSelectorString());
    Assert.assertArrayEquals(new String[0],rpi.getSelectors());

    rpi = new RequestPathInfoWrapper("/testing/1/2/.json");
    Assert.assertEquals("/testing/1/2/", rpi.getResourcePath());
    Assert.assertEquals("json",rpi.getExtension());
    Assert.assertNull(rpi.getSelectorString());
    Assert.assertArrayEquals(new String[0],rpi.getSelectors());

     rpi = new RequestPathInfoWrapper("/testing/1/2/3.json");
    Assert.assertEquals("/testing/1/2/3", rpi.getResourcePath());
    Assert.assertEquals("json",rpi.getExtension());
    Assert.assertNull(rpi.getSelectorString());
    Assert.assertArrayEquals(new String[0],rpi.getSelectors());

    rpi = new RequestPathInfoWrapper("/testing/1/2/3.aa.json");
    Assert.assertEquals("/testing/1/2/3", rpi.getResourcePath());
    Assert.assertEquals("json",rpi.getExtension());
    Assert.assertEquals("aa",rpi.getSelectorString());
    Assert.assertArrayEquals(new String[]{"aa"},rpi.getSelectors());
    rpi = new RequestPathInfoWrapper("/testing/1/2/3.aa.bb.json");
    Assert.assertEquals("/testing/1/2/3", rpi.getResourcePath());
    Assert.assertEquals("json",rpi.getExtension());
    Assert.assertEquals("aa.bb",rpi.getSelectorString());
    Assert.assertArrayEquals(new String[]{"aa","bb"},rpi.getSelectors());

  }
}
