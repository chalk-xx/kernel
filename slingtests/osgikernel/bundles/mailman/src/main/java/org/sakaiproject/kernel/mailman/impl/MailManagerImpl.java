package org.sakaiproject.kernel.mailman.impl;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.sakaiproject.kernel.api.proxy.ProxyClientService;
import org.sakaiproject.kernel.mailman.MailManager;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class MailManagerImpl implements MailManager {

  
  /** @scr.reference */
  private ProxyClientService proxyClientService;
  
  /** @scr.property value="example.com" type="String" */
  private String mailmanHost = "mailman.host";
  
  /** @scr.property value="/cgi-bin/mailman" type="String" */
  private String mailmanPath = "mailman.path";
  
  /** @scr.property value="password" type="String" */
  private String listAdminPassword = "mailman.listadmin.password";
  
  private String getMailmanUrl(String stub) {
    return "http://" + mailmanHost + mailmanPath + stub;
  }
  
  public void createList(String listName, String ownerEmail, String password) throws MailManException, HttpException, IOException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    PostMethod post = new PostMethod(getMailmanUrl("/create"));
    NameValuePair[] parametersBody = new NameValuePair[] {
        new NameValuePair("listname", listName),
        new NameValuePair("owner", ownerEmail),
        new NameValuePair("password", password),
        new NameValuePair("auth", listAdminPassword),
        new NameValuePair("langs", "en"),
        new NameValuePair("notify", "1"),
        new NameValuePair("autogen", "0"),
        new NameValuePair("moderate", "0"),
        new NameValuePair("doit", "Create List")
    };
    post.setRequestBody(parametersBody);
    int result = client.executeMethod(post);
    try {
      if (result != HttpServletResponse.SC_OK) {
        throw new MailManException("Unable to create list");
      }
    } finally {
      post.releaseConnection();
    }
  }

  public void deleteList(String listName, String listPassword) throws HttpException, IOException, MailManException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    PostMethod post = new PostMethod(getMailmanUrl("/rmlist/" + listName));
    NameValuePair[] parametersBody = new NameValuePair[] {
        new NameValuePair("password", listPassword),
        new NameValuePair("delarchives", "0"),
        new NameValuePair("doit", "Delete this list")
    };
    post.setRequestBody(parametersBody);
    int result = client.executeMethod(post);
    try {
      if (result != HttpServletResponse.SC_OK) {
        throw new MailManException("Unable to create list");
      }
    } finally {
      post.releaseConnection();
    }
  }

}
