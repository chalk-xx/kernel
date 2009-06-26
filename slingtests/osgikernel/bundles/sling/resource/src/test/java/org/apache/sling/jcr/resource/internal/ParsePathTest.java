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
package org.apache.sling.jcr.resource.internal;

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.resource.ResourceMetadata;
import org.junit.Test;

/**
 * Tests the non existent resource path parsing,
 */
public class ParsePathTest {



  @Test
  public void testPathStart() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("sdsdfs.selector.html");
    assertEquals("sdsdfs", rm.getResolutionPath());
    assertEquals(".selector.html", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartAbs() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/sdsdfs.selector.html");
    assertEquals("/sdsdfs", rm.getResolutionPath());
    assertEquals(".selector.html", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMid() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("midpath/sdsdfs.selector.html");
    assertEquals("midpath/sdsdfs", rm.getResolutionPath());
    assertEquals(".selector.html", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMidAbs() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/midpath/sdsdfs.selector.html");
    assertEquals("/midpath/sdsdfs", rm.getResolutionPath());
    assertEquals(".selector.html", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMidAbsMulti() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/m/i/d/p/a/t/h/sdsdfs.selector.html");
    assertEquals("/m/i/d/p/a/t/h/sdsdfs", rm.getResolutionPath());
    assertEquals(".selector.html", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMidAbsMultiNoSel() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/m/i/d/p/a/t/h/sdsdfs.html");
    assertEquals("/m/i/d/p/a/t/h/sdsdfs", rm.getResolutionPath());
    assertEquals(".html", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMidAbsMultiAfter() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/m/i/d/p/a/t/h/sdsdfs.html/abce/asdad");
    assertEquals("/m/i/d/p/a/t/h/sdsdfs", rm.getResolutionPath());
    assertEquals(".html/abce/asdad", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMidAbsMultiAfterDots() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/m/i/d/p/a/t/h/sdsdfs.html/abce/a.s.d.a.d");
    assertEquals("/m/i/d/p/a/t/h/sdsdfs", rm.getResolutionPath());
    assertEquals(".html/abce/a.s.d.a.d", rm.getResolutionPathInfo());
  }

  @Test
  public void testPathStartMidAbsMultiAfterDotsTrailing() {
    ResourceMetadata rm = ResourceMetatdatFactory.createMetadata("/m/i/d/p/a/t/h/sdsdfs.html/abce/a.s.d.a.d/");
    assertEquals("/m/i/d/p/a/t/h/sdsdfs", rm.getResolutionPath());
    assertEquals(".html/abce/a.s.d.a.d/", rm.getResolutionPathInfo());
  }
}
