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

package org.sakaiproject.kernel.jpaprovider.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("property")
public class Property {

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Property)) {
      return false;
    }
    Property other = (Property) obj;
    if (!getName().equals(other.getName())) {
      return false;
    }
    return getValue().equals(other.getValue());
  }

  @Override
  public int hashCode() {
    return getName().hashCode() + getValue().hashCode();
  }

  private String name;
  private String value;

  public String getName() {
    return (name != null ? name : "");
  }

  public String getValue() {
    return (value != null ? value : "");
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
