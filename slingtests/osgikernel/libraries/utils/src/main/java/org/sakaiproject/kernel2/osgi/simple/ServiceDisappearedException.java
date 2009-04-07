package org.sakaiproject.kernel2.osgi.simple;

public class ServiceDisappearedException extends Exception {
	private static final long serialVersionUID = -2474691283147508901L;

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
