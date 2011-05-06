package org.sakaiproject.nakamura.api.activity;

import org.apache.sling.api.resource.Resource;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ActivityServiceCallback {

  Resource processRequest(Content activtyNode) throws StorageClientException, ServletException, IOException;

}
