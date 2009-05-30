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
  public static String getUserPrefix(String user,int levels) {
    if (user != null) {
      if (user.length() == 0) {
        user = "anon";
      }
      return getStructuredHash(user,levels);
    }
    return null;
  }

  /**
   * Get the prefix for a message.
   * 
   * @return Prefix used to store a message. Defaults to a yyyy/mm/dd structure.
   * @see java.text.SimpleDateFormat for pattern definitions.
   */
  public static String getMessagePrefix() {
    Calendar c = Calendar.getInstance();
    String prefix = c.get(Calendar.YEAR) + "/" + c.get(Calendar.MONTH) + "/";
    return prefix;
  }

  /**
   * @param target
   *          the target being formed into a structured path.
   * @return the structured path.
   */
  private static String getStructuredHash(String target, int levels) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] userHash = md.digest(target.getBytes("UTF-8"));

      char[] chars = new char[levels*3+1 + target.length()];
      for (int i = 0; i < levels; i++) {
        byte current = userHash[i];
        int hi = (current & 0xF0) >> 4;
        int lo = current & 0x0F;
        chars[(i*3)+0] = (char) (hi < 10 ? ('0' + hi) : ('A' + hi - 10));
        chars[(i*3)+1] = (char) (lo < 10 ? ('0' + lo) : ('A' + lo - 10));
        chars[(i*3)+2] = '/';
      }
      for (int i = 0; i < target.length(); i++) {
        char c = target.charAt(i);
        if (!Character.isLetterOrDigit(c)) {
          c = '_';
        }
        chars[i + levels*3] = c;
      }
      chars[levels*3 + target.length()] = '/';
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
  public static String getDatePrefix(String path,int levels) {
    String hash = getStructuredHash(path,levels);
    Calendar c = Calendar.getInstance();
    StringBuilder sb = new StringBuilder();
    sb.append(c.get(Calendar.YEAR)).append("/").append(c.get(Calendar.MONTH)).append("/")
        .append(hash);
    return sb.toString();
  }
  /**
   * @param path
   *          the original path.
   * @return a pooled hash of the filename
   */
  public static String getHashedPrefix(String path,int levels) {
    return getStructuredHash(path,levels);
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

}
