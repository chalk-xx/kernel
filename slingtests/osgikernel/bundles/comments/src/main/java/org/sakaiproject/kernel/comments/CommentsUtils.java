package org.sakaiproject.kernel.comments;

import org.sakaiproject.kernel.util.PathUtils;

public class CommentsUtils {

    /**
     * Gets the full JCR path to a comment.
     * @param commentstore The path to the comment store
     * @param commentid The id of the comment.
     * @return The JCR path to the comment like: /path/to/store/aa/bb/cc/45/commentid
     */
    public static String getCommentsPathById(String commentstore, String commentid) {
        return PathUtils.toInternalHashedPath(commentstore, commentid, "");
    }
}
