package org.sakaiproject.nakamura.discussion;

import static org.junit.Assert.assertNotNull;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.fs.local.FileUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import java.io.File;
import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

public class DiscussionManagerTest {

  /**
   * 
   */
  private static final String DATA = "./target/testdata/jackrabbittest";

  /**
   * 
   */
  private static final String CONFIG = "repository.xml";

  private static Repository repository;
  private Session aliveSession;

  @BeforeClass
  public static void beforeAll() throws IOException, LoginException, RepositoryException {
    // Clean up the test data ...
    File dataFile = new File(DATA);
    if (dataFile.exists()) {
      FileUtil.delete(dataFile);
    }

    repository = new TransientRepository(CONFIG, DATA);
  }

  @AfterClass
  public static void afterAll() throws IOException {
    try {
      ((JackrabbitRepository) repository).shutdown();
    } finally {
      // Clean up after ourselves
      File dataFile = new File(DATA);
      if (dataFile.exists()) {
        FileUtil.delete(dataFile);
      }
    }
  }

  /**
   * Starts the repository if it hasn't been started before.
   * 
   * @throws LoginException
   * @throws RepositoryException
   */
  private void startRepo() throws LoginException, RepositoryException {
    if (aliveSession == null) {
      aliveSession = repository.login();
      NamespaceRegistry registry = aliveSession.getWorkspace().getNamespaceRegistry();
      try {
        registry.getPrefix("sakai");
      } catch (NamespaceException e) {
        registry.registerNamespace("sakai", "sakai");
      }
      try {
        registry.getPrefix("sling");
      } catch (NamespaceException e) {
        registry.registerNamespace("sling", "sling");
      }
    }
  }

  /**
   * Login as administrator
   * 
   * @return Returns the administrator session.
   * @throws LoginException
   * @throws RepositoryException
   */
  private Session loginAsAdmin() throws LoginException, RepositoryException {
    return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
  }

  @Test
  public void testFindSettings() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException {

    startRepo();

    Session adminSession = loginAsAdmin();

    // Add a couple of nodes
    Node rootNode = adminSession.getRootNode();
    Node settingsNode = rootNode.addNode("settingsNode");
    settingsNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "sakai/settings");
    settingsNode.setProperty("sakai:marker", "foo");
    settingsNode.setProperty("sakai:type", "discussion");

    Node randomNode = rootNode.addNode("foo");
    randomNode.setProperty("foo", "bar");

    adminSession.save();

    DiscussionManager manager = new DiscussionManagerImpl();
    Node result = manager.findSettings("foo", adminSession, "discussion");

    assertNotNull(result);
    assertEquals("/settingsNode", result.getPath());
  }

  @Test
  public void testFindMessage() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException {

    startRepo();

    Session adminSession = loginAsAdmin();

    // Add a couple of nodes
    Node rootNode = adminSession.getRootNode();
    Node messagesNode = rootNode.addNode("messages");
    Node msgNode = messagesNode.addNode("msgNodeCorrect");
    msgNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    msgNode.setProperty("sakai:marker", "foo");
    msgNode.setProperty("sakai:type", "discussion");
    msgNode.setProperty("sakai:id", "10");

    Node msgNode2 = messagesNode.addNode("msgNodeCorrect2");
    msgNode2.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    msgNode2.setProperty("sakai:marker", "foo");
    msgNode2.setProperty("sakai:type", "discussion");
    msgNode2.setProperty("sakai:id", "20");

    Node randomNode = messagesNode.addNode("foo");
    randomNode.setProperty("foo", "bar");

    adminSession.save();

    DiscussionManager manager = new DiscussionManagerImpl();
    Node result = manager.findMessage("10", "foo", adminSession, "/messages");

    assertNotNull(result);
    assertEquals("/messages/msgNodeCorrect", result.getPath());
  }
}
