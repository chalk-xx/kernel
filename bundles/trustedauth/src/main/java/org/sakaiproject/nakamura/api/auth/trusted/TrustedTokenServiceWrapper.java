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
package org.sakaiproject.nakamura.api.auth.trusted;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.sakaiproject.nakamura.auth.trusted.TrustedTokenServiceImpl;

/**
 *
 */
public abstract class TrustedTokenServiceWrapper {
  
  private TrustedTokenServiceImpl delagate;

  public TrustedTokenServiceWrapper(TrustedTokenService delegate) {
    if ( !delegate.getClass().equals(TrustedTokenServiceImpl.class)) {
      throw new IllegalArgumentException("You may only wrap a valid instance of the TrustedTokenService Invalid :"+delegate);
    }
    // Only exported classes can be listed here,
    // we cant use the class directly since that would set up a circular dependency,
    // at the moment, this is about as good as I can get without putting all the code in a single
    // bundle.
    if ("org.sakaiproject.nakamura.formauth.FormAuthenticationTokenServiceWrapper".equals(getClass().getName()) ) {        
        this.delagate = (TrustedTokenServiceImpl) delegate;
        return;
    }
    throw new IllegalArgumentException("Invalid Wrapping Class :"+getClass());
    
  }
  
  /**
   * @param request
   * @param response
   */
  protected void injectToken(SlingHttpServletRequest request,
      SlingHttpServletResponse response) {
    delagate.injectToken(request, response);
  }

}
