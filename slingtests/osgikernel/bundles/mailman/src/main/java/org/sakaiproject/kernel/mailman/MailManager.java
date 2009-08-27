package org.sakaiproject.kernel.mailman;

import org.apache.commons.httpclient.HttpException;
import org.sakaiproject.kernel.mailman.impl.MailManException;

import java.io.IOException;

public interface MailManager {
  public void createList(String listName, String ownerEmail, String password) throws MailManException, HttpException, IOException;
  public void deleteList(String listName, String listPassword) throws HttpException, IOException, MailManException;
}
