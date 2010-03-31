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
package org.sakaiproject.nakamura.calendar;

import net.fortuna.ical4j.model.Calendar;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.CalendarService;

import java.io.InputStream;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 *
 */
@Component(immediate = true)
@Service(value = CalendarService.class)
public class CalendarServiceImpl implements CalendarService {

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#export(javax.jcr.Node)
   */
  public Calendar export(Node node) throws CalendarException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(net.fortuna.ical4j.model.Calendar,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(Calendar calendar, Session session, String path)
      throws CalendarException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(java.lang.String,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(String calendar, Session session, String path)
      throws CalendarException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(java.io.InputStream,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(InputStream calendar, Session session, String path)
      throws CalendarException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(java.io.Reader,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(Reader calendar, Session session, String path)
      throws CalendarException {
    // TODO Auto-generated method stub
    return null;
  }

}
