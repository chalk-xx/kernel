package org.sakaiproject.nakamura.activity;

import org.sakaiproject.nakamura.api.lite.Session;
import org.sakaiproject.nakamura.api.lite.StorageClientException;
import org.sakaiproject.nakamura.api.lite.accesscontrol.AccessDeniedException;
import org.sakaiproject.nakamura.api.lite.content.Content;

import java.io.IOException;

import javax.servlet.ServletException;

public interface ActivityService {

  void createActivity(Session session, Content location, String userId, ActivityServiceCallback activityServiceCallback) throws AccessDeniedException, StorageClientException, ServletException, IOException;

}
