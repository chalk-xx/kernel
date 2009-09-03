package org.sakaiproject.kernel.mailman;

import org.apache.commons.httpclient.HttpException;
import org.sakaiproject.kernel.mailman.impl.MailmanException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

public interface MailmanManager {
  public boolean isServerActive() throws HttpException, IOException, MailmanException, SAXException;
  public List<String> getLists() throws HttpException, IOException, MailmanException, SAXException;
  public void createList(String listName, String ownerEmail, String password) throws MailmanException, HttpException, IOException;
  public void deleteList(String listName, String listPassword) throws HttpException, IOException, MailmanException;
  public boolean listHasMember(String listName, String listPassword, String memberEmail) throws HttpException, IOException, MailmanException, SAXException;
  public boolean addMember(String listName, String listPassword, String userEmail) throws HttpException, IOException, MailmanException, SAXException;
  public boolean removeMember(String listName, String listPassword, String userEmail) throws HttpException, IOException, MailmanException, SAXException;
}
