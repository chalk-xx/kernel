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
package org.sakaiproject.nakamura.api.tika;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * OSGi service to wrap {@link Tika} and load a config file found local this bundle. This
 * service can be updated to match whatever underlying version of Tika that is being used.
 * 
 * The traditional way of getting a reference to Tika is still applicable which is how
 * this service gets its internal reference.
 * 
 * <code>
 * URL configUrl = bundleContext.getBundle().getResource("/org/apache/tika/tika-config.xml");
 * Tika tika = new Tika(new TikaConfig(configUrl));
 * </code>
 */
@Component
@Service(value = TikaService.class)
public class TikaService {
  private Tika tika;

  // ---------- SCR integration ----------
  @Activate
  protected void activate(BundleContext bundleContext) throws Exception {
    URL configUrl = bundleContext.getBundle().getResource(
        "/org/apache/tika/tika-config.xml");
    tika = new Tika(new TikaConfig(configUrl));
  }

  @Deactivate
  protected void deactivate() {
    tika = null;
  }

  // ---------- Tika methods ----------
  public String detect(InputStream stream, Metadata metadata) throws IOException {
    return tika.detect(stream, metadata);
  }

  public String detect(InputStream stream) throws IOException {
    return tika.detect(stream);
  }

  public String detect(File file) throws IOException {
    return tika.detect(file);
  }

  public String detect(URL url) throws IOException {
    return tika.detect(url);
  }

  public String detect(String name) {
    return tika.detect(name);
  }

  public Reader parse(InputStream stream, Metadata metadata) throws IOException {
    return tika.parse(stream, metadata);
  }

  public Reader parse(InputStream stream) throws IOException {
    return tika.parse(stream);
  }

  public Reader parse(File file) throws IOException {
    return tika.parse(file);
  }

  public Reader parse(URL url) throws IOException {
    return tika.parse(url);
  }

  public String parseToString(InputStream stream, Metadata metadata) throws IOException,
      TikaException {
    return tika.parseToString(stream, metadata);
  }

  public String parseToString(InputStream stream) throws IOException, TikaException {
    return tika.parseToString(stream);
  }

  public String parseToString(File file) throws IOException, TikaException {
    return tika.parseToString(file);
  }

  public String parseToString(URL url) throws IOException, TikaException {
    return tika.parseToString(url);
  }

  public int getMaxStringLength() {
    return tika.getMaxStringLength();
  }
}