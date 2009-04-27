package org.sakaiproject.kernel.api.messaging.email;

import javax.mail.Session;

public interface MailSessionProvider {
  Session getSession();
}
