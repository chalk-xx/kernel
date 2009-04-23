package org.sakaiproject.kernel.api.messaging;

public class ConversionException extends MessagingException {

  private static final long serialVersionUID = 1L;

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
