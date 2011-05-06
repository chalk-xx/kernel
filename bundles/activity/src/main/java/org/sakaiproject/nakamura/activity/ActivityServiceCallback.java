package org.sakaiproject.nakamura.activity;

import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ActivityServiceCallback {

  void processRequest(Content activtyNode) throws StorageClientException, ServletException, IOException, AccessDeniedException;

}
