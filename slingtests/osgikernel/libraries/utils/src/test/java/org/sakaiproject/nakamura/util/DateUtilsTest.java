package org.sakaiproject.nakamura.util;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class DateUtilsTest {

  @Test
  public void testRfc3339() throws Exception {
    Pattern dateFormat = Pattern
        .compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[-+]\\d{4}$");
    assertTrue(dateFormat.matcher(DateUtils.rfc3339()).matches());
  }

  @Test
  public void testRfc2822() throws Exception {
    Pattern dateFormat = Pattern
        .compile("^\\p{Alpha}{3}, \\d{2} \\p{Alpha}{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [-+]\\d{4}$");
    assertTrue(dateFormat.matcher(DateUtils.rfc2822()).matches());
  }
}
