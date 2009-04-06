package org.sakaiproject.kernel2.uxloader;

public class URLFileMapping {
	private String fs,url;
	
	URLFileMapping(String url,String fs) {
		this.fs=fs;
		this.url=url;
	}
	
	String getFileSystem() { return fs; }
	String getURL() { return url; }
}
