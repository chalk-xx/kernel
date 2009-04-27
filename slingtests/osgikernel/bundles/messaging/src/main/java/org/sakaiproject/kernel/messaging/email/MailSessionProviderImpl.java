package org.sakaiproject.kernel.messaging.email;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.messaging.email.MailSessionProvider;

import java.util.Dictionary;
import java.util.Properties;

import javax.mail.Session;

/**
 * @scr.component
 * @scr.service
 */
public class MailSessionProviderImpl implements MailSessionProvider, ManagedService {
  /** @scr.property value="localhost" */
  public final static String SMTP_HOST = "mail.smtp.host";

  /** @scr.property value="25" */
  public final static String SMTP_PORT = "mail.smtp.port";

  /** @scr.property value="true" type="Boolean" */
  public final static String SMTP_SENDPARTIAL = "mail.smtp.sendpartial";

  private Session session;

  public MailSessionProviderImpl() {
  }

  @SuppressWarnings("unchecked")
  public MailSessionProviderImpl(Dictionary dict) {
    session = createSession(dict);
  }

  @SuppressWarnings("unchecked")
  public void activate(ComponentContext ctx) {
    Dictionary dict = ctx.getProperties();
    session = createSession(dict);
  }

  public void deactivate(ComponentContext ctx) {
    session = null;
  }

  public Session getSession() {
    return session;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary dict) throws ConfigurationException {
    if (dict != null) {
      session = createSession(dict);
    }
  }

  @SuppressWarnings("unchecked")
  private Session createSession(Dictionary dict) {
    Properties props = new Properties();
    props.put("mail.smtp.host", dict.get(SMTP_HOST));
    props.put("mail.smtp.port", dict.get(SMTP_PORT));
    return Session.getDefaultInstance(props);
  }
}
