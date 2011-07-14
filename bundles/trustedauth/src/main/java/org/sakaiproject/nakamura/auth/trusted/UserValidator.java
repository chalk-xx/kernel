package org.sakaiproject.nakamura.auth.trusted;

public interface UserValidator {

  /**
   * Checks that the userId is Ok, returns the userId if ok, null if not.
   * @param userId
   * @return
   */
  String validate(String userId);

}
