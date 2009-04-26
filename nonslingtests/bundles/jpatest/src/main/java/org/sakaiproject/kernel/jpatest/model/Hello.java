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

package org.sakaiproject.kernel.jpatest.model;

import org.eclipse.persistence.jpa.PersistenceProvider;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class Hello {

  private static final String PERSISTENCE_UNIT_NAME = "greetingPU";
  private EntityManagerFactory emf;
  private EntityManager em;

  private void createAndRead() {
    Greeting g = new Greeting();
    g.setId(1L);
    g.setMessage("hello, createAndRead");
    em.getTransaction().begin();
    em.persist(g);
    em.getTransaction().commit();

    Greeting g2 = em.find(Greeting.class, g.getId());
    System.out.println("Greeting " + g.getId() + " from db: " + g2);
  }

  private void createAndRollback() {
    Greeting g = new Greeting();
    g.setId(2L);
    g.setMessage("hello, createAndRollback");
    em.getTransaction().begin();
    em.persist(g);
    em.getTransaction().rollback();

    System.out.println("Persisted " + g + ", but the transaction was rolled back.");
    Greeting g2 = em.find(Greeting.class, g.getId());
    System.out.println("Greeting " + g.getId() + " from db: " + g2);
  }

  private void initEntityManager() {
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("eclipselink.ddl-generation", "drop-and-create-tables");
    properties.put("eclipselink.ddl-generation.output-mode", "database");
    properties.put("eclipselink.classloader", this.getClass().getClassLoader());
    emf = new PersistenceProvider().createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
    em = emf.createEntityManager();
  }

  public static void main(String[] args) {
    Hello hello = new Hello();
    hello.initEntityManager();
    hello.createAndRead();
    hello.createAndRollback();
  }

}
