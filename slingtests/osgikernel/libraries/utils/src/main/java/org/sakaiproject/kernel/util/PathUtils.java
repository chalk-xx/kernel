/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.kernel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

/**
 * Generate a path prefix based on the user id.
 * 
 */
public class PathUtils {

  /**
   *
   */
  private static final Logger logger = LoggerFactory.getLogger(PathUtils.class);

  /**
   * Generate a path using a SHA-1 hash split into path parts to generate a unique path to
   * the user information, that will not result in too many objects in each folder.
   * 
   * @param user
   *          the user for which the path will be generated.
   * @return a structured path fragment for the user.
   */
  public static String getUserPrefix(String user, int levels) {
    if (user != null) {
      if (user.length() == 0) {
        user = "anon";
      }
      return getStructuredHash(user, levels, false);
    }
    return null;
  }

  /**
   * Get the prefix for a message.
   * 
   * @return Prefix used to store a message. Defaults to a yyyy/mm/dd structure.
   * @see java.text.SimpleDateFormat for pattern definitions.
   */
  public static String getMessagePath() {
    Calendar c = Calendar.getInstance();
    String prefix = "/" + c.get(Calendar.YEAR) + "/" + c.get(Calendar.MONTH) + "/";
    return prefix;
  }

  /**
   * @param target
   *          the target being formed into a structured path.
   * @param b
   * @return the structured path.
   */
  private static String getStructuredHash(String target, int levels, boolean absPath) {
    try {
      // take the first element as the key for the target so that subtrees end up in the
      // same place.
      String[] elements = StringUtils.split(target, '/', 1);
      String pathInfo = removeFirstElement(target);
      target = elements[0];

      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] userHash = md.digest(target.getBytes("UTF-8"));

      char[] chars = new char[(absPath?1:0) + levels * 3 + target.length() + pathInfo.length()];
      int j = 0;
      if (absPath) {
        chars[j++] = '/';
      }
      for (int i = 0; i < levels; i++) {
        byte current = userHash[i];
        int hi = (current & 0xF0) >> 4;
        int lo = current & 0x0F;
        chars[j++] = (char) (hi < 10 ? ('0' + hi) : ('a' + hi - 10));
        chars[j++] = (char) (lo < 10 ? ('0' + lo) : ('a' + lo - 10));
        chars[j++] = '/';
      }
      for (int i = 0; i < target.length(); i++) {
        char c = target.charAt(i);
        if (!Character.isLetterOrDigit(c)) {
          c = '_';
        }
        chars[j++] = c;

      }
      for (int i = 0; i < pathInfo.length(); i++) {
        chars[j++] = pathInfo.charAt(i);
      }
      return new String(chars);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * @param resourceReference
   * @return
   */
  public static String getParentReference(String resourceReference) {
    char[] ref = resourceReference.toCharArray();
    int i = ref.length - 1;
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    while (i >= 0 && ref[i] != '/') {
      i--;
    }
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    if (i == -1) {
      return "/";
    }
    return new String(ref, 0, i + 1);
  }

  /**
   * @param path
   *          the original path.
   * @return a pooled hash of the filename
   */
  public static String getDatePath(String path, int levels) {
    String hash = getStructuredHash(path, levels, true);
    Calendar c = Calendar.getInstance();
    StringBuilder sb = new StringBuilder();
    sb.append("/").append(c.get(Calendar.YEAR)).append("/").append(c.get(Calendar.MONTH))
        .append(hash);
    return sb.toString();
  }

  /**
   * @param path
   *          the original path.
   * @return a pooled hash of the filename
   */
  public static String getHashedPath(String path, int levels) {
    return getStructuredHash(path, levels, true);
  }

  /**
   * Normalizes the input path to an absolute path prepending / and ensuring that the path
   * does not end in /.
   * 
   * @param pathFragment
   *          the path.
   * @return a normalized path.
   */
  public static String normalizePath(String pathFragment) {
    char[] source = pathFragment.toCharArray();
    char[] normalized = new char[source.length + 1];
    int i = 0;
    int j = 0;
    if (source.length == 0 || source[i] != '/') {
      normalized[j++] = '/';
    }
    boolean slash = false;
    for (; i < source.length; i++) {
      char c = source[i];
      switch (c) {
      case '/':
        if (!slash) {
          normalized[j++] = c;
        }
        slash = true;
        break;
      default:
        slash = false;
        normalized[j++] = c;
        break;
      }
    }
    if (j > 1 && normalized[j - 1] == '/') {
      j--;
    }
    return new String(normalized, 0, j);
  }

  /**
   * Removes the first element of the path
   * 
   * @param path
   *          the path
   * @return the path with the first element removed.
   */
  public static String removeFirstElement(String path) {
    if (path == null || path.length() == 0) {
      return path;
    }
    char[] p = path.toCharArray();
    int i = 0;
    while (i < p.length && p[i] == '/') {
      i++;
    }
    while (i < p.length && p[i] != '/') {
      i++;
    }
    if (i < p.length) {
      return new String(p, i, p.length - i);
    }
    return "/";
  }

  /**
   * Remove the last path element.
   * 
   * @param path
   *          the path
   * @return the path with the last element removed.
   */
  public static String removeLastElement(String path) {
    if (path == null || path.length() == 0) {
      return path;
    }
    char[] p = path.toCharArray();
    int i = p.length - 1;
    while (i >= 0 && p[i] == '/') {
      i--;
    }
    while (i >= 0 && p[i] != '/') {
      i--;
    }
    if (i > 0) {
      return new String(p, 0, i);
    }
    return "/";

  }

}
