package org.sakaiproject.nakamura.api.personal;

import java.util.Calendar;

public interface PersonalTrackingStore {
  /**
   * Method to store a record of activity within Sakai groups, content, etc.
   * The record of activity will be persisted so that it may be accessed later.
   * The persistence mechanism is unspecified, and is the responsibility of the
   * implementing class
   *
   * @param resourceId the identifier of the group or content that was modified
   * @param resourceType content | group
   * @param activityType ADDED | CHANGED
   * @param userid the id of the user who performed the activity
   * @param timestamp date and time of the activity
   */
  void recordActivity(String resourceId, String resourceType, String activityType, String userId, Calendar timestamp);
  
}
