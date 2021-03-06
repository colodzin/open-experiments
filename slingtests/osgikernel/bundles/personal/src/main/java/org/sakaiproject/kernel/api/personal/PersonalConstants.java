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
package org.sakaiproject.kernel.api.personal;

/**
 * 
 */
public interface PersonalConstants {

  /**
   * The base location of personal private stores.
   */
  public static final String _USER_PRIVATE = "/_user/private";
  /**
   * The resource type of personal private stores.
   */
  public static final String USER_PRIVATE_RESOURCE_TYPE = "sakai/personalPrivate";
  /**
   * The base location of public personal stores
   */
  public static final String _USER_PUBLIC = "/_user/public";
  /**
   * The resource type for public personal stores
   */
  public static final String USER_PUBLIC_RESOURCE_TYPE = "sakai/personalPublic";

  /**
   * The base location of private groups stores
   */
  public static final String _GROUP_PRIVATE = "/_group/private";
  /**
   * The resource type for private group stores
   */
  public static final String GROUP_PRIVATE_RESOURCE_TYPE = "sakai/groupPrivate";
  /**
   * The locatio of public groups
   */
  public static final String _GROUP_PUBLIC = "/_group/public";
  /**
   * The resource type for the public group store.
   */
  public static final String GROUP_PUBLIC_RESOURCE_TYPE = "sakai/groupPublic";

  /**
  *
  */
  public static final String PERSONAL_OPERATION = "org.sakaiproject.kernel.personal.operation";

  /**
   * The node name of the authentication profile in public space.
   */
  public static final String AUTH_PROFILE = "authprofile";

  /**
   * Property name for the e-mail property of a user's profile
   */
  public static final String EMAIL_ADDRESS = "email";

  /**
   * Property name for the user's preferred means of message delivery
   */
  public static final String PREFERRED_MESSAGE_TRANSPORT = "preferredMessageTransport";

}
