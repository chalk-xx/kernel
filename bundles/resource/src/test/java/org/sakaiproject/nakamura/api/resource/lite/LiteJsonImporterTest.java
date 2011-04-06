package org.sakaiproject.nakamura.api.resource.lite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.sakaiproject.nakamura.api.lite.ClientPoolException;
import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessControlManager;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AclModification.Operation;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Permissions;
import org.sakaiproject.nakamura.api.lite.authorizable.AuthorizableManager;
import org.sakaiproject.nakamura.api.lite.content.ContentManager;
import org.sakaiproject.nakamura.lite.BaseMemoryRepository;
import org.sakaiproject.nakamura.lite.RepositoryImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;

public class LiteJsonImporterTest {

  private static final String[] TEST_FILES = {
    "testimport/test1.json",
    "testimport/test2.json",
    "testimport/test3.json",
    "testimport/test4.json",
    "testimport/test5.json"
    };
  private RepositoryImpl repository;

  public LiteJsonImporterTest() throws AccessDeniedException, StorageClientException,
      ClassNotFoundException {
    BaseMemoryRepository baseMemoryRepository = new BaseMemoryRepository();
    repository = baseMemoryRepository.getRepository();
    Session session = repository.loginAdministrative();
    AuthorizableManager authorizableManager = session.getAuthorizableManager();
    authorizableManager.createUser("ieb", "Ian Boston", "test",
        ImmutableMap.of("x", (Object) "y"));
    org.sakaiproject.nakamura.api.lite.authorizable.Authorizable authorizable = authorizableManager
        .findAuthorizable("ieb");
    System.err.println("Got ieb as " + authorizable);
    session.logout();

  }

  @Test
  public void testPath() {
    LiteJsonImporter liteImporter = new LiteJsonImporter();
    Assert.assertEquals(null, liteImporter.getPathElement(null));
    Assert.assertEquals(null, liteImporter.getPathElement(""));
    Assert.assertEquals(null, liteImporter.getPathElement("@"));
    Assert.assertEquals("", liteImporter.getPathElement("@x"));
    Assert.assertEquals("key", liteImporter.getPathElement("key"));
    Assert.assertEquals("key", liteImporter.getPathElement("key@Unknown"));

  }

  @Test
  public void testTypes() {
    LiteJsonImporter liteImporter = new LiteJsonImporter();
    Assert.assertEquals(String.class, liteImporter.getElementType(null));
    Assert.assertEquals(String.class, liteImporter.getElementType(""));
    Assert.assertEquals(String.class, liteImporter.getElementType("@"));
    Assert.assertEquals(String.class, liteImporter.getElementType("@x"));
    Assert.assertEquals(String.class, liteImporter.getElementType("key"));
    Assert.assertEquals(String.class, liteImporter.getElementType("key@Unknown"));
    Assert.assertEquals(String.class, liteImporter.getElementType("key@grant"));
    Assert.assertEquals(String.class, liteImporter.getElementType("key@deny"));
    Assert.assertEquals(String.class, liteImporter.getElementType("key@Delete"));
    Assert.assertEquals(String.class, liteImporter.getElementType("key@TypeString"));
    Assert.assertEquals(Integer.class, liteImporter.getElementType("key@TypeInteger"));
    Assert.assertEquals(Long.class, liteImporter.getElementType("key@TypeLong"));
    Assert.assertEquals(Double.class, liteImporter.getElementType("key@TypeDouble"));
    Assert.assertEquals(Boolean.class, liteImporter.getElementType("key@TypeBoolean"));
    Assert.assertEquals(BigDecimal.class,
        liteImporter.getElementType("key@TypeBigDecimal"));
    Assert.assertEquals(Calendar.class, liteImporter.getElementType("key@TypeDate"));
  }

  @Test
  public void testObject() {
    LiteJsonImporter liteImporter = new LiteJsonImporter();
    Assert.assertEquals("1", liteImporter.getObject("1", String.class));
    Assert.assertEquals((Long) 1L, liteImporter.getObject("1", Long.class));
    Assert.assertEquals((Integer) 1, liteImporter.getObject("1", Integer.class));
    Assert.assertEquals((Double) 1.0, liteImporter.getObject("1", Double.class));
    Assert.assertEquals(new BigDecimal("1.094"),
        liteImporter.getObject("1.094", BigDecimal.class));
    Assert.assertEquals((Boolean) true, liteImporter.getObject("true", Boolean.class));
    Calendar c = liteImporter.getObject("20110329T101523+0330", Calendar.class);
    Assert.assertEquals(2011, c.get(Calendar.YEAR));
    Assert.assertEquals(2, c.get(Calendar.MONTH));
    Assert.assertEquals(29, c.get(Calendar.DAY_OF_MONTH));
    Assert.assertEquals(10, c.get(Calendar.HOUR_OF_DAY));
    Assert.assertEquals(15, c.get(Calendar.MINUTE));
    Assert.assertEquals(23, c.get(Calendar.SECOND));
    Assert.assertEquals(((3 * 60 + 30) * 60) * 1000, c.get(Calendar.ZONE_OFFSET));
    c = liteImporter.getObject("20110329T101523Z", Calendar.class);
    Assert.assertEquals(2011, c.get(Calendar.YEAR));
    Assert.assertEquals(2, c.get(Calendar.MONTH));
    Assert.assertEquals(29, c.get(Calendar.DAY_OF_MONTH));
    Assert.assertEquals(10, c.get(Calendar.HOUR_OF_DAY));
    Assert.assertEquals(15, c.get(Calendar.MINUTE));
    Assert.assertEquals(23, c.get(Calendar.SECOND));
    Assert.assertEquals(0, c.get(Calendar.ZONE_OFFSET));
    c = liteImporter.getObject("20110329T101523-0330", Calendar.class);
    Assert.assertEquals(2011, c.get(Calendar.YEAR));
    Assert.assertEquals(2, c.get(Calendar.MONTH));
    Assert.assertEquals(29, c.get(Calendar.DAY_OF_MONTH));
    Assert.assertEquals(10, c.get(Calendar.HOUR_OF_DAY));
    Assert.assertEquals(15, c.get(Calendar.MINUTE));
    Assert.assertEquals(23, c.get(Calendar.SECOND));
    Assert.assertEquals(-((3 * 60 + 30) * 60) * 1000, c.get(Calendar.ZONE_OFFSET));
  }

  @Test
  public void testArray() throws JSONException {
    LiteJsonImporter liteJsonImporter = new LiteJsonImporter();
    JSONArray arr = new JSONArray(Lists.immutableList("1", "2", "3"));
    String[] s = liteJsonImporter.getArray(arr, String.class);
    Assert.assertArrayEquals(new String[] { "1", "2", "3" }, s);
    Integer[] ia = liteJsonImporter.getArray(arr, Integer.class);
    Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, ia);
    Long[] la = liteJsonImporter.getArray(arr, Long.class);
    Assert.assertArrayEquals(new Long[] { 1L, 2L, 3L }, la);
    arr = new JSONArray(Lists.immutableList("1.1", "2.2", "3.3"));
    Double[] da = liteJsonImporter.getArray(arr, Double.class);
    Assert.assertArrayEquals(new Double[] { 1.1, 2.2, 3.3 }, da);
    arr = new JSONArray(Lists.immutableList("true", "false", "0"));
    Boolean[] ba = liteJsonImporter.getArray(arr, Boolean.class);
    Assert.assertArrayEquals(new Boolean[] { true, false, false }, ba);
  }

  @Test
  public void testPermissionBitmap() throws JSONException {
    LiteJsonImporter liteJsonImporter = new LiteJsonImporter();
    Assert.assertEquals(Permissions.CAN_READ.getPermission(),
        liteJsonImporter.getPermissionBitMap(new JSONArray(ImmutableList.of("read"))));
    Assert.assertEquals(Permissions.CAN_WRITE.getPermission(),
        liteJsonImporter.getPermissionBitMap(new JSONArray(ImmutableList.of("write"))));
    Assert.assertEquals(Permissions.CAN_DELETE.getPermission(),
        liteJsonImporter.getPermissionBitMap(new JSONArray(ImmutableList.of("delete"))));
    Assert
        .assertEquals(Permissions.CAN_READ_ACL.getPermission(), liteJsonImporter
            .getPermissionBitMap(new JSONArray(ImmutableList.of("read-acl"))));
    Assert.assertEquals(Permissions.CAN_WRITE_ACL.getPermission(), liteJsonImporter
        .getPermissionBitMap(new JSONArray(ImmutableList.of("write-acl"))));
    Assert.assertEquals(Permissions.CAN_DELETE_ACL.getPermission(), liteJsonImporter
        .getPermissionBitMap(new JSONArray(ImmutableList.of("delete-acl"))));
    Assert.assertEquals(Permissions.ALL.getPermission(),
        liteJsonImporter.getPermissionBitMap(new JSONArray(ImmutableList.of("all"))));
    Assert.assertEquals(Permissions.ALL.getPermission(),
        liteJsonImporter.getPermissionBitMap(new JSONArray(ImmutableList.of("manage"))));
    Assert
        .assertEquals(Permissions.CAN_ANYTHING.getPermission(), liteJsonImporter
            .getPermissionBitMap(new JSONArray(ImmutableList.of("anything"))));
    Assert.assertEquals(Permissions.CAN_ANYTHING_ACL.getPermission(), liteJsonImporter
        .getPermissionBitMap(new JSONArray(ImmutableList.of("anything-acl"))));
    Assert.assertEquals(
        Permissions.CAN_READ.combine(Permissions.CAN_WRITE)
            .combine(Permissions.CAN_DELETE).getPermission(), liteJsonImporter
            .getPermissionBitMap(new JSONArray(ImmutableList
                .of("read", "write", "delete"))));
  }
  
  
  @Test
  public void testOperation() {
    LiteJsonImporter liteJsonImporter = new LiteJsonImporter();
    Assert.assertEquals(Operation.OP_OR, liteJsonImporter.getOperation("or"));
    Assert.assertEquals(Operation.OP_AND, liteJsonImporter.getOperation("and"));
    Assert.assertEquals(Operation.OP_XOR, liteJsonImporter.getOperation("xor"));
    Assert.assertEquals(Operation.OP_NOT, liteJsonImporter.getOperation("not"));
    Assert.assertEquals(Operation.OP_DEL, liteJsonImporter.getOperation("del"));
    Assert.assertEquals(Operation.OP_REPLACE, liteJsonImporter.getOperation("replace"));
    Assert.assertEquals(Operation.OP_OR, liteJsonImporter.getOperation("OR"));
  }
  
  @Test
  public void testImportContent() throws ClientPoolException, StorageClientException, AccessDeniedException, JSONException, IOException {
    LiteJsonImporter liteJsonImporter = new LiteJsonImporter();
    Session session = repository.loginAdministrative();
    ContentManager contentManager = session.getContentManager();
    AccessControlManager accessControlManager = session.getAccessControlManager();
    
    for ( String testFile : TEST_FILES ) {
      JSONObject json = new JSONObject(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(testFile)));
      liteJsonImporter.importContent(contentManager, json, testFile, true, true, true, accessControlManager);
    }
  }
}
