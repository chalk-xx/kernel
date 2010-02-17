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
package org.sakaiproject.nakamura.doc;

import org.junit.Test;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class DocumentedServiceTest {

  private static final Logger LOG = LoggerFactory.getLogger(DocumentedServiceTest.class);

  @Test
  public void testDocumentedService() {
    DocumentedService docService = new DocumentedService();
    ServiceDocumentation serviceDocumetation = docService.getClass().getAnnotation(ServiceDocumentation.class);
    LOG.info("Service Name: "+serviceDocumetation.name());
    LOG.info("Description ");
    for (String desc : serviceDocumetation.description() ) {
      LOG.info(desc);
    }
    LOG.info("Bindings ");
    for ( ServiceBinding sb : serviceDocumetation.bindings()) {
      LOG.info("Type "+sb.type());
      for ( String binding : sb.bindings() ) {
        LOG.info(" bound as: "+binding);
      }
    }
    LOG.info("Methods ");
    for ( ServiceMethod sm : serviceDocumetation.methods() ) {
      LOG.info("Method "+sm.name());
      LOG.info("Description ");
      for (String desc : sm.description() ) {
        LOG.info(desc);
      }
      for ( ServiceParameter sp : sm.parameters() ) {
        LOG.info("   Parameter "+sp.name());
        
        LOG.info("   Description ");
        for (String desc : sp.description() ) {
          LOG.info(desc);
        }
      }
    }
  }
}
