package org.sakaiproject.kernel.api.user;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;

import javax.jcr.RepositoryException;

public class UserModification extends Modification {

  private Group group;
  private User user;
  private boolean isJoin;
  
  public UserModification(ModificationType type, String source, String destination) {
    super(type, source, destination);
  }

  /**
   * Records a 'modified' change
   * 
   * @param path path of the item that was modified
   */
  public static Modification onGroupJoin(String path, Group group, User user) {
    UserModification result = new UserModification(ModificationType.MODIFY, path, null);
    result.setGroup(group);
    result.setUser(user);
    result.setJoin(true);
    return result;
  }

  public static Modification onGroupLeave(String path, Group group, User user) {
    UserModification result = new UserModification(ModificationType.MODIFY, path, null);
    result.setGroup(group);
    result.setUser(user);
    result.setJoin(false);
    return result;
  }

  private void setJoin(boolean isJoin) {
    this.isJoin = isJoin;
  }

  private void setUser(User user) {
    this.user = user;
  }

  private void setGroup(Group group) {
    this.group = group;
  }

  public Group getGroup() {
    return group;
  }

  public User getUser() {
    return user;
  }

  public boolean isJoin() {
    return isJoin;
  }
  
  public String toString() {
    try {
      return "User " + user.getID() + " " + (isJoin ? "" : "un") + "joins group " + group.getID();
    } catch (RepositoryException e) {
    }
    return "User " + user + " " + (isJoin ? "" : "un") + "joins group " + group;
  }
}
