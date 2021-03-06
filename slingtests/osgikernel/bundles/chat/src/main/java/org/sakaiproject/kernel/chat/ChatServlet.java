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
package org.sakaiproject.kernel.chat;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.chat.ChatManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Will check if a user has any chat updates.
 * 
 * @scr.component metatype="no" immediate="true" label="ChatServlet"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" values="sakai/messagestore"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="chatupdate"
 * @scr.reference name="ChatManagerService"
 *                interface="org.sakaiproject.kernel.api.chat.ChatManagerService"
 */
public class ChatServlet extends SlingAllMethodsServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChatServlet.class);
  private static final long serialVersionUID = -4011626674940239621L;
  private ChatManagerService chatManagerService;

  protected void bindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = chatManagerService;
  }

  protected void unbindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String userID = request.getRemoteUser();
    long time = System.currentTimeMillis();
    RequestParameter timestampParam = request.getRequestParameter("t");
    if (timestampParam != null) {
      time = Long.parseLong(timestampParam.getString());
    }
    boolean update = chatManagerService.checkUpdate(userID, time);

    if (update) {
      // Because there is an update and we just retrieved it. We set a new time and send
      // the new one back to the user.
      time = System.currentTimeMillis();
      chatManagerService.addUpdate(userID, time);
    }

    JSONWriter write = new JSONWriter(response.getWriter());
    try {
      write.object();
      write.key("update");
      write.value(update);
      write.key("time");
      write.value(time);
      write.endObject();
    } catch (JSONException e) {
      LOGGER.warn("Unable to parse JSON for user {} and time {}", userID, time);
      e.printStackTrace();
      response.sendError(500, "Unable to parse JSON.");
    }
    
    // Make sure the connection is not keep-alive.
    response.setHeader("Connection", "close");
  }
}
