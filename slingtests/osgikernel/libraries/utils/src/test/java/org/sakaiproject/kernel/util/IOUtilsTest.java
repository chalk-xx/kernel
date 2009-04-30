package org.sakaiproject.kernel.util;

import static org.junit.Assert.*;

import java.io.InputStream;
import org.junit.Test;

public class IOUtilsTest {

  @Test
  public void testReadFully() throws Exception {
    InputStream is = this.getClass().getResourceAsStream("lipsum.txt");
    assertNotNull(is);
    String lipsum = IOUtils.readFully(is, "UTF-8");
    InputStream verify = this.getClass().getResourceAsStream("lipsum.txt");
    for (Byte b : lipsum.getBytes()) {
      assertEquals(b.intValue(), verify.read());
    }
    verify.close();
  }

}
