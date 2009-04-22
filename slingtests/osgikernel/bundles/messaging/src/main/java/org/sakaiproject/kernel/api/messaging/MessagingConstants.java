package org.sakaiproject.kernel.api.messaging;

public interface MessagingConstants {
  String JCR_MESSAGE_RCPTS = "rcpts";
  String JCR_MESSAGE_FROM = "from";
  String JCR_MESSAGE_TYPE = "type";
  String JCR_MESSAGE_DATE = "date";

  String JCR_LABELS = "sakaijcr:labels";
  String LABEL_INBOX = "inbox";

  String FOLDER_OUTBOX = "outbox";

  String FOLDER_MESSAGES = "messages";
}
