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

package org.sakaiproject.nakamura.rules;

import org.drools.KnowledgeBase;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.rule.Package;
import org.drools.util.DroolsStreamUtils;
import org.sakaiproject.nakamura.api.rules.RulePackageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * This class holds knowledge bases, and knows how to refresh the knowledge base when the
 * source pacakges are refreshed or and dependent classloaders are reloaded.
 */
public class KnowledgeBaseHolder {

  public List<WeakReferenceClassloader> classloaders = new ArrayList<WeakReferenceClassloader>();
  private KnowledgeBaseImpl knowlegeBase;
  private long lastModified = 0;

  public KnowledgeBaseHolder(Node ruleSetNode) throws IOException,
      ClassNotFoundException, RepositoryException, InstantiationException,
      IllegalAccessException {
    load(ruleSetNode, true);
  }

  private void load(Node ruleSetNode, boolean force) throws IOException,
      ClassNotFoundException, RepositoryException, InstantiationException,
      IllegalAccessException {
    // not in the cache, create a knowledge base.
    // there are 2 ways of creating a rule set.
    // one is to use a resource from annother bundle and also use its classloader
    // the other is to load the package from the content system

    if (force || knowlegeBase == null || !checkClassloaders()) {
      long currentLastModified = getLastModified(ruleSetNode);
      if (currentLastModified > lastModified) {

        classloaders.clear();

        RuleBase ruleBase = RuleBaseFactory.newRuleBase();

        NodeIterator ni = ruleSetNode.getNodes();
        for (; ni.hasNext();) {
          Node n = ni.nextNode();
          if (NodeType.NT_FILE.equals(n.getPrimaryNodeType().getName())) {
            InputStream in = null;
            try {
              in = n.getNode(Node.JCR_CONTENT).getProperty(Property.JCR_DATA).getBinary()
                  .getStream();
              Object o = DroolsStreamUtils.streamIn(in);
              ruleBase.addPackage((Package) o);
            } finally {
              try {
                in.close();
              } catch (Exception e) {

              }
            }
          } else {

            if (n.hasProperty("sling/resourceType")) {
              String resourceType = n.getProperty("sling/ResourceType").getString();
              if ("sakai/rule-set-package-service".equals(resourceType)) {
                @SuppressWarnings("unchecked")
                Class<RulePackageLoader> ruleLoaderCLass = (Class<RulePackageLoader>) this
                    .getClass().getClassLoader()
                    .loadClass(n.getProperty("sakai:bundle-resource-class").getString());
                RulePackageLoader rpl = ruleLoaderCLass.newInstance();
                InputStream in = rpl.getPackageInputStream();
                try {
                  WeakReferenceClassloader wrc = new WeakReferenceClassloader(
                      rpl.getPackageClassLoader());
                  classloaders.add(wrc);
                  Object o = DroolsStreamUtils.streamIn(in, wrc);
                  ruleBase.addPackage((Package) o);
                } finally {
                  try {
                    in.close();
                  } catch (Exception e) {

                  }
                }

              }
            }
          }
        }
        lastModified = currentLastModified;
        knowlegeBase = new KnowledgeBaseImpl(ruleBase);
      }
    }
  }

  private boolean checkClassloaders() {
    for (WeakReferenceClassloader wrc : classloaders) {
      if (!wrc.isAvailable()) {
        return false;
      }
    }
    return true;
  }

  private long getLastModified(Node ruleSetNode) throws RepositoryException {
    NodeIterator ni = ruleSetNode.getNodes();
    long curentLastModified = 0;
    for (; ni.hasNext();) {
      Node n = ni.nextNode();
      if (NodeType.NT_FILE.equals(n.getPrimaryNodeType().getName())) {
        try {
          curentLastModified = Math.max(curentLastModified, n.getNode(Node.JCR_CONTENT)
              .getProperty(Property.JCR_LAST_MODIFIED).getDate().getTimeInMillis());
        } catch (Exception ex) {

        }
      } else {
        if (n.hasProperty("sling/resourceType")) {
          String resourceType = n.getProperty("sling/ResourceType").getString();

          if ("sakai/rule-set-package-service".equals(resourceType)) {
            try {
              curentLastModified = Math.max(curentLastModified,
                  n.getNode(Node.JCR_CONTENT).getProperty(Property.JCR_LAST_MODIFIED)
                      .getDate().getTimeInMillis());
            } catch (Exception ex) {

            }
          }
        }
      }
    }
    return curentLastModified;
  }

  public void refresh(Node ruleSetNode) throws IOException, ClassNotFoundException,
      RepositoryException, InstantiationException, IllegalAccessException {
    load(ruleSetNode, false);
  }

  public KnowledgeBase getKnowledgeBase() {
    return knowlegeBase;
  }

}
