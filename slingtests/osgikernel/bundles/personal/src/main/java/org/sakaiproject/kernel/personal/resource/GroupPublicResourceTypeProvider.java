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
package org.sakaiproject.kernel.personal.resource;

import static org.sakaiproject.kernel.api.personal.PersonalConstants.GROUP_PUBLIC_RESOURCE_TYPE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.kernel.resource.AbstractVirtualResourceType;
import org.sakaiproject.kernel.resource.VirtualResourceType;

/**
 * This class checks resource paths to see if there is a preferred resource type, where the
 * path is not a jcr path.
 * 
 */
@Component(immediate=true, label="GroupPublicResourceTypeProvider",  description="Group Public Service path resource type provider")
@Properties(value={
    @Property(name="service.description", value="Handles requests for Group Public resources"),
    @Property(name="service.vendor",value="The Sakai Foundation")
})
@Service(value=VirtualResourceType.class)
public class GroupPublicResourceTypeProvider extends AbstractVirtualResourceType {

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.kernel.resource.AbstractPathResourceTypeProvider#getResourceType()
   */
  public String getResourceType() {
    return GROUP_PUBLIC_RESOURCE_TYPE;
  }

}
