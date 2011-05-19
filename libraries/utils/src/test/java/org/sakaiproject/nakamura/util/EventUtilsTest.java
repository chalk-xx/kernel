package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sakaiproject.nakamura.util.osgi.EventUtils;

public class EventUtilsTest {

	  @SuppressWarnings("unchecked")
	  @Test
	  public void testCleanProperties(){
		  // Unsupported types
		  assertEquals(null, EventUtils.cleanProperty(null));
		  assertEquals(null, EventUtils.cleanProperty(new Date()));

		  // Supported Primitives
		  assertEquals(1, EventUtils.cleanProperty(1));
		  assertEquals((byte)1, EventUtils.cleanProperty((byte)1));
		  assertEquals(true, EventUtils.cleanProperty(true));
		  assertEquals("str", EventUtils.cleanProperty("str"));

		  // Lists are supported
		  List<String> l = new ArrayList<String>();
		  l.add("one");
		  l.add("two");
		  assertEquals(l, EventUtils.cleanProperty(l));

		  // Arrays are converted to Lists
		  String[] sa = new String[]{"one", "two"};
		  List<String> l2 = (List<String>)EventUtils.cleanProperty(sa);
		  assertEquals(2, l2.size());
		  assertEquals("one", l2.get(0));

		  // Map values are cleaned
		  Map<String,Object> m = new HashMap<String,Object>();
		  m.put("one", 1);
		  m.put("array", new String[]{"str1", "str2"});

		  // Nested Map
		  Map<String,Object> hashProp = new HashMap<String, Object>();
		  hashProp.put("prop1", 1);
		  hashProp.put("propArray", new String[]{"meh", "bleh"});
		  m.put("hash", hashProp);

		  // Properties of the nested map
		  Map<String,Object> m1 = (Map<String,Object>)EventUtils.cleanProperty(m);
		  assertEquals(new Integer(1), (Integer)m1.get("one"));
		  assertEquals(2, ((List<String>)m1.get("array")).size());

		  Map<String,Object> cleanedHashProp = (Map<String,Object>)m1.get("hash");
		  assertEquals(true, (cleanedHashProp instanceof Map));
		  assertEquals(2, ((List<String>)cleanedHashProp.get("propArray")).size());
	  }

}
