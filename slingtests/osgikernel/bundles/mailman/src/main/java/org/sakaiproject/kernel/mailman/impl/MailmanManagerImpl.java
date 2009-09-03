package org.sakaiproject.kernel.mailman.impl;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.cyberneko.html.parsers.DOMParser;
import org.sakaiproject.kernel.api.proxy.ProxyClientService;
import org.sakaiproject.kernel.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLTableElement;
import org.w3c.dom.html.HTMLTableRowElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * @scr.component immediate="true" label="MailManagerImpl"
 *                description="Interface to mailman"
 * @scr.property name="service.description"
 *                value="Handles management of mailman integration"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 */
public class MailmanManagerImpl implements MailmanManager {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(MailmanManagerImpl.class);
  
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
  
  public void createList(String listName, String ownerEmail, String password) throws MailmanException, HttpException, IOException {
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
        throw new MailmanException("Unable to create list");
      }
    } finally {
      post.releaseConnection();
    }
  }

  public void deleteList(String listName, String listPassword) throws HttpException, IOException, MailmanException {
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
        throw new MailmanException("Unable to create list");
      }
    } finally {
      post.releaseConnection();
    }
  }

  public boolean listHasMember(String listName, String listPassword, String memberEmail) throws HttpException, IOException, MailmanException, SAXException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin/" + listName + "members"));
    NameValuePair[] parameters = new NameValuePair[] {
        new NameValuePair("findmember", memberEmail),
        new NameValuePair("setmemberopts_btn", ""),
        new NameValuePair("adminpw", listPassword)
    };
    get.setQueryString(parameters);
    int result = client.executeMethod(get);
    try {
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to search for member");
      }
      Document dom = parseHtml(get);
      NodeList inputs = dom.getElementsByTagName("INPUT");
      String unsubString = URLEncoder.encode(memberEmail, "utf8") + "_unsub";
      for (int i=0; i<inputs.getLength(); i++) {
        Node input = inputs.item(i);
        try {
          if (input.getAttributes().getNamedItem("name").getTextContent().equals(unsubString)) {
            return true;
          }
        } catch (NullPointerException npe) {
        }
      }
    } finally {
      get.releaseConnection();
    }
    return false;
  }
  
  public boolean addMember(String listName, String listPassword, String memberEmail) throws HttpException, IOException, MailmanException, SAXException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin/" + listName + "members/add"));
    NameValuePair[] parameters = new NameValuePair[] {
      new NameValuePair("subscribe_or_invite", "0"),
      new NameValuePair("send_welcome_msg_to_this_batch", "0"),
      new NameValuePair("notification_to_list_owner", "0"),
      new NameValuePair("subscribees_upload", memberEmail),
      new NameValuePair("adminpw", listPassword)
    };
    get.setQueryString(parameters);
    int result = client.executeMethod(get);
    try {
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to add member");
      }
      Document dom = parseHtml(get);
      NodeList inputs = dom.getElementsByTagName("h5");
      if (inputs.getLength() == 0) {
        throw new MailmanException("Unable to read result status");
      }
      return "Successfully subscribed:".equals(inputs.item(0).getTextContent());
    } finally {
      get.releaseConnection();
    }
  }

  public boolean removeMember(String listName, String listPassword, String memberEmail) throws HttpException, IOException, MailmanException, SAXException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin/" + listName + "members/remove"));
    NameValuePair[] parameters = new NameValuePair[] {
      new NameValuePair("send_unsub_ack_to_this_batch", "0"),
      new NameValuePair("send_unsub_notifications_to_list_owner", "0"),
      new NameValuePair("unsubscribees_upload", memberEmail),
      new NameValuePair("adminpw", listPassword)
    };
    get.setQueryString(parameters);
    int result = client.executeMethod(get);
    try {
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to add member");
      }
      Document dom = parseHtml(get);
      NodeList inputs = dom.getElementsByTagName("h5");
      if (inputs.getLength() == 0) {
        inputs = dom.getElementsByTagName("h3");
        if (inputs.getLength() == 0) {
          throw new MailmanException("Unable to read result status");
        }
      }
      return "Successfully Unsubscribed:".equals(inputs.item(0).getTextContent());
    } finally {
      get.releaseConnection();
    }
  }

  private Document parseHtml(HttpMethodBase method) throws SAXException, IOException {
    DOMParser parser = new DOMParser();
    parser.parse(new InputSource(method.getResponseBodyAsStream()));
    return parser.getDocument();
  }

  public List<String> getLists() throws HttpException, IOException, MailmanException, SAXException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin"));
    List<String> lists = new ArrayList<String>();
    int result = client.executeMethod(get);
    try {
      if (result != HttpServletResponse.SC_OK) {
        LOGGER.warn("Got " + result + " from http request");
        throw new MailmanException("Unable to list mailinglists");
      }
      DOMParser parser = new DOMParser();
      parser.parse(new InputSource(get.getResponseBodyAsStream()));
      Document doc = parser.getDocument();
      NodeList tableNodes = doc.getElementsByTagName("table");
      if (tableNodes.getLength() < 2) {
        throw new MailmanException("Unrecognised page format.");
      }
      HTMLTableElement mainTable = (HTMLTableElement) tableNodes.item(0);
      HTMLCollection rows = mainTable.getRows();
      for (int i=4; i<rows.getLength(); i++) {
        lists.add(parseListNameFromRow((HTMLTableRowElement)rows.item(i)));
      }
    } finally {
      get.releaseConnection();
    }
    return lists;
  }

  private String parseListNameFromRow(HTMLTableRowElement item) throws MailmanException {
    HTMLCollection cells = item.getCells();
    if (cells.getLength() != 2) {
      throw new MailmanException("Unexpected table row format");
    }
    return cells.item(0).getTextContent();
  }

  void setServer(String mailmanHost) {
    this.mailmanHost = mailmanHost;
  }

  void setMailmanPath(String mailmanPath) {
    this.mailmanPath = mailmanPath;
  }

  ProxyClientService getProxyClientService() {
    return proxyClientService;
  }

  void setProxyClientService(ProxyClientService proxyClientService) {
    this.proxyClientService = proxyClientService;
  }

  public boolean isServerActive() throws HttpException, IOException, MailmanException,
      SAXException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin"));
    int result = client.executeMethod(get);
    try {
      if (result != HttpServletResponse.SC_OK) {
        LOGGER.warn("Got " + result + " from http request");
        return false;
      }
      return true;
    } finally {
      get.releaseConnection();
    }
  }

}
