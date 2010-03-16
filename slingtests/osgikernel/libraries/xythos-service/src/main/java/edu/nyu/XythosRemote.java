package edu.nyu;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface XythosRemote {
	
	/**
	 * This method is just a way of saying 'Hey, are you there?'
	 * @return true if everything is ok on this end
	 */
	boolean ping();
	
	/**
	 * Create a new Xythos directory for the specified user in the specified location
	 * @param username
	 * @param virtualServerName
	 * @param homeDirectory
	 * @param directoryName
	 */
	void createDirectory (String username, String virtualServerName, 
			String homeDirectory, String directoryName);
	
	String findAllFilesForUser(String l_username);
	
	String saveFile(String path, String id,
      byte[] contentBytes, String fileName, String contentType, String userId);
	
	Collection<Map<String, String>> findFilesWithXPath(String xpathQuery, String userId);
	
	Map<String,String> getProperties();
	
	byte[] getFileContent(String path, String userId);
	
	String getContentType(String path, String userId);
	
	long getContentLength(String path, String userId);
	
	String getContentUri(String path, String userId);
	
	Map<String, Object> getFileProperties(String path, String userId);
	
	XythosDocument getDocument(String path, String userId);
	
	List<String> doSearch(Map<String, Object> searchProperties, String userId);
	
	void updateFile(String path, byte[] fileData, Map<String, Object>properties, String userId);
	
	void addMember(String groupId, String userId);
	
	void createGroup(String groupId, String userId);

}
