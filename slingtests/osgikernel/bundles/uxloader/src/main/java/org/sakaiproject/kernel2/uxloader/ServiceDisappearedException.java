package org.sakaiproject.kernel2.uxloader;

public class ServiceDisappearedException extends Exception {
	public ServiceDisappearedException() {
		super();
	}

	public ServiceDisappearedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceDisappearedException(String message) {
		super(message);
	}

	public ServiceDisappearedException(Throwable cause) {
		super(cause);
	}
}
