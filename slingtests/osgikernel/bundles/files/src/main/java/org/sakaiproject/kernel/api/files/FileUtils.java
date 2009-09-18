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
package org.sakaiproject.kernel.api.files;

public class FileUtils {

  /**
   * Returns a Base62 representation of a number.
   * @param nr
   * @return
   */
  private static String encodeBase62(long nr) {
    String tempVal = nr == 0 ? "0" : "";
    String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    int remainder = 0;
    while (nr > 0) {
      remainder = (int) (nr % 62);
      nr = nr / 62;
      tempVal += baseDigits.charAt(remainder);
    }
    return tempVal;
  }
  
  /**
   * Returns a Base62 number.
   * @return
   */
  public static String generateID() {
    long n = (long) (Math.random() * 1000000000 + System.currentTimeMillis());
    return encodeBase62(n);
  }
}
