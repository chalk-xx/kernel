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
package org.sakaiproject.nakamura.imap;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SLF4JLogFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.jcr.GlobalMailboxSessionJCRRepository;
import org.apache.james.imap.jcr.JCRMailboxManager;
import org.apache.james.imap.jcr.JCRSubscriptionManager;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imap.store.Authenticator;
import org.apache.james.imapserver.netty.NioImapServer;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;

/**
 *
 */
@Component
public class NakamuraNioImapServer extends NioImapServer {

  private static final Log jcLog = SLF4JLogFactory.getLog(NakamuraNioImapServer.class);
  
  @Reference
  private SlingRepository slingRepository;

  public void activate(ComponentContext componentContext) throws Exception {
    
    GlobalMailboxSessionJCRRepository sessionRepos = new GlobalMailboxSessionJCRRepository(slingRepository, null, "admin", "admin");
    
    // Register imap cnd file org/apache/james/imap/jcr/imap.cnd when the bundle starts up
    // JCRUtils.registerCnd(repository, workspace, user, pass);
    
    Authenticator userManager = new Authenticator() {
      
      public boolean isAuthentic(String userid, CharSequence passwd) {
        return true; // delegate authentication to JCR, the JCR session should use the same user as the imap session. We might want to 
        // integrate this with the sling authentication handlers.
      }
    };

    //TODO: Fix the scaling stuff so the tests will pass with max scaling too
    MailboxManager mailboxManager = new JCRMailboxManager(userManager, new JCRSubscriptionManager(sessionRepos), sessionRepos);

    final DefaultImapProcessorFactory defaultImapProcessorFactory = new DefaultImapProcessorFactory();
    MailboxSession session = mailboxManager.createSystemSession("test", jcLog);
    mailboxManager.startProcessingRequest(session);
    //mailboxManager.deleteEverything(session);
    mailboxManager.endProcessingRequest(session);
    mailboxManager.logout(session, false);
    
    defaultImapProcessorFactory.configure(mailboxManager);
    
    setImapDecoder(new DefaultImapDecoderFactory().buildImapDecoder());
    setImapEncoder(new DefaultImapEncoderFactory().buildImapEncoder());
    setImapProcessor(defaultImapProcessorFactory.buildImapProcessor());
    setLog(jcLog);
    DefaultConfigurationBuilder config = new DefaultConfigurationBuilder(getClass().getResource("imap-config.xml"));
    configure(config);
    init();
  }
  
  public void deactivate(ComponentContext componentContext) {
    destroy();
  }
}
