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


/**
 *
 */
public class DocumentedServiceTest {

  @Test
  public void testDocumentedService() {
    DocumentedService docService = new DocumentedService();
    ServiceDocumentation serviceDocumetation = docService.getClass().getAnnotation(ServiceDocumentation.class);
    System.err.println("Service Name: "+serviceDocumetation.name());
    System.err.println("Description ");
    for (String desc : serviceDocumetation.description() ) {
      System.err.println(desc);
    }
    System.err.println("Bindings ");
    for ( ServiceBinding sb : serviceDocumetation.bindings()) {
      System.err.println("Type "+sb.type());
      for ( String binding : sb.bindings() ) {
        System.err.println(" bound as: "+binding);
      }
    }
    System.err.println("Methods ");
    for ( ServiceMethod sm : serviceDocumetation.methods() ) {
      System.err.println("Method "+sm.name());
      System.err.println("Description ");
      for (String desc : sm.description() ) {
        System.err.println(desc);
      }
      for ( ServiceParameter sp : sm.parameters() ) {
        System.err.println("   Parameter "+sp.name());
        
        System.err.println("   Description ");
        for (String desc : sp.description() ) {
          System.err.println(desc);
        }
      }
    }
  }
}
