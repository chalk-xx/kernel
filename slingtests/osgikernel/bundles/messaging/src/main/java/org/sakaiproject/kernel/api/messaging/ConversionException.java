package org.sakaiproject.kernel.api.messaging;

public class ConversionException extends MessagingException {

  public ConversionException() {
    super();
  }

  public ConversionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConversionException(String message) {
    super(message);
  }

  public ConversionException(Throwable cause) {
    super(cause);
  }
}
