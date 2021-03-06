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

package org.sakaiproject.kernel.discussion;

import org.sakaiproject.kernel.api.discussion.DiscussionConstants;
import org.sakaiproject.kernel.api.discussion.DiscussionManager;
import org.sakaiproject.kernel.api.discussion.DiscussionTypes;
import org.sakaiproject.kernel.api.message.AbstractMessageRoute;
import org.sakaiproject.kernel.api.message.MessageConstants;
import org.sakaiproject.kernel.api.message.MessageRoute;
import org.sakaiproject.kernel.api.message.MessageRouter;
import org.sakaiproject.kernel.api.message.MessageRoutes;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * 
 * @scr.component inherit="true" label="DiscussionRouter" immediate="true"
 * @scr.service interface="org.sakaiproject.kernel.api.message.MessageRouter"
 * @scr.property name="service.description"
 *               value="Manages Routing for the discussion posts."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.reference name="DiscussionManager"
 *                interface="org.sakaiproject.kernel.api.discussion.DiscussionManager"
 */
public class DiscussionRouter implements MessageRouter {

  private DiscussionManager discussionManager;

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    List<MessageRoute> toRemove = new ArrayList<MessageRoute>();
    List<MessageRoute> toAdd = new ArrayList<MessageRoute>();

    // Check if this message is a discussion message.
    try {
      if (n.hasProperty(MessageConstants.PROP_SAKAI_TYPE)
          && n.hasProperty(DiscussionConstants.PROP_MARKER)
          && DiscussionTypes.hasValue(n.getProperty(MessageConstants.PROP_SAKAI_TYPE)
              .getString())) {

        // This is a discussion message, find the settings file for it.
        String marker = n.getProperty(DiscussionConstants.PROP_MARKER).getString();
        String type = n.getProperty(MessageConstants.PROP_SAKAI_TYPE).getString();

        // TODO: I have a feeling that this is really part of something more generic
        // and not specific to discussion. If we make it specific to discussion we
        // will loose unified messaging and control of that messaging.

        Node settings = discussionManager.findSettings(marker, n.getSession(), type);
        if (settings != null
            && settings.hasProperty(DiscussionConstants.PROP_NOTIFICATION)) {
          boolean sendMail = settings.getProperty(DiscussionConstants.PROP_NOTIFICATION)
              .getBoolean();
          if (sendMail && settings.hasProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS)) {
            String address = settings
                .getProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS).getString();
            toAdd.add(new AbstractMessageRoute("internal:" + address) {
            });

          }
        }

      }
    } catch (ValueFormatException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (MessageRoute route : routing) {
      if (DiscussionTypes.hasValue(route.getTransport())) {
        toAdd.add(new AbstractMessageRoute("internal:" + route.getRcpt()) {
        });
        toRemove.add(route);
      }
    }
    // Add the new routes
    for (MessageRoute route : toAdd) {
      routing.add(route);
    }
    // Remove the discussion route (if there is any).
    for (MessageRoute route : toRemove) {
      routing.remove(route);
    }
  }

}
