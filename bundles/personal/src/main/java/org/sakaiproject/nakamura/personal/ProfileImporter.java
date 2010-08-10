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
package org.sakaiproject.nakamura.personal;

import static org.sakaiproject.nakamura.api.personal.PersonalConstants.PROFILE_JSON_IMPORT_PROPERTY;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Helper class to use a JSON string to initialize the contents of an authprofile
 * node.
 */
public class ProfileImporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileImporter.class);
  /**
   * When passed to importContent, this string means "Do not create a new root
   * node. Use the JSON reader."
   */
  public static final String CONTENT_ROOT_NAME = ".json";

  private final static ImportOptions importOptions = new ImportOptions() {
    @Override
    public boolean isOverwrite() {
      return false;
    }
    @Override
    public boolean isCheckin() {
      return false;
    }
    @Override
    public boolean isIgnoredImportProvider(String extension) {
      return false;
    }
    @Override
    public boolean isPropertyOverwrite() {
      return false;
    }
  };

  public static void importFromAuthorizable(Node profileNode, Authorizable authorizable, ContentImporter contentImporter,
      Session session) {
    try {
      if (authorizable.hasProperty(PROFILE_JSON_IMPORT_PROPERTY)) {
        Value[] values = authorizable.getProperty(PROFILE_JSON_IMPORT_PROPERTY);
        if (values.length == 1) {
          String json = values[0].getString();
          importFromJsonString(profileNode, json, contentImporter, session);
        } else {
          LOGGER.warn("Improperly formatted profile import property: {}", values);
        }
        // The property's hackish work is done.
        authorizable.removeProperty(PROFILE_JSON_IMPORT_PROPERTY);
      }
    } catch (RepositoryException e) {
      LOGGER.error("Unable to import content for profile node " + profileNode, e);
    } catch (IOException e) {
      LOGGER.error("Unable to import content for profile node " + profileNode, e);
    }
  }

  private static void importFromJsonString(Node profileNode, String json, ContentImporter contentImporter,
      Session session) throws RepositoryException, IOException {
    ByteArrayInputStream contentStream = new ByteArrayInputStream(json.getBytes());
    contentImporter.importContent(profileNode, CONTENT_ROOT_NAME, contentStream, importOptions, null);
    LOGGER.info("Imported content to {} from JSON string '{}'", profileNode.getPath(), json);
  }
}
