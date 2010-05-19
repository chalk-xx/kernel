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
package org.sakaiproject.nakamura.util.osgi;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
/**
 *
 */
@SuppressWarnings("unchecked")
public abstract class AbstractOrderedService<T> implements BoundService {
  
  /**
   * 
   */
  private T[] orderedServices = (T[]) new Object();
  
  /**
   * The set of processors 
   */
  private Set<T> serviceSet = new HashSet<T>();

  /**
   * A list of listeners that have actions that need to be performed when processors are registered or de-registered.
   */
  private List<BindingListener> listeners = new ArrayList<BindingListener>();



  
  /**
   * @return
   */
  protected T[] getOrderedServices() {
    return orderedServices;
  }


  public void addListener(BindingListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
      listeners.add(listener);
    }
  }
  
  public void removeListener(BindingListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Notifies registered listeners of a change to the list so that they can invoke any aditional actions they deem necessary.
   */
  private void notifyNewList() {
    synchronized(listeners) {
      for ( BindingListener listener : listeners ) {
        listener.notifyBinding();
      }
    }
    
  }
  /**
   * SCR Integration, bind a service.
   * @param service
   */
  protected void bindSortedService(T service) {
    synchronized (serviceSet) {
      serviceSet.add(service);
      createNewSortedList();
      notifyNewList();
    }
  }

  /**
   * SCR integration, unbind a service.
   * @param service
   */
  protected void unbindSortedService(T service) {
    synchronized (serviceSet) {
      serviceSet.remove(service);
      createNewSortedList();
      notifyNewList();
    }
  }

  /**
   * Generate a new sorted list. 
   */
  private void createNewSortedList() {
    List<T> serviceList = new ArrayList<T>(serviceSet);
    Collections.sort(serviceList, getComparitor());
    orderedServices = (T[]) serviceList.toArray(new Object[serviceList.size()]); 
  }


  /**
   * @return
   */
  protected abstract Comparator<? super T> getComparitor();




}
