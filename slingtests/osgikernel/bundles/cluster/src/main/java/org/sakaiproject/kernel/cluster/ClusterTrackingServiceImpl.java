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
package org.sakaiproject.kernel.cluster;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.kernel.api.cluster.ClusterServer;
import org.sakaiproject.kernel.api.cluster.ClusterTrackingService;
import org.sakaiproject.kernel.api.cluster.ClusterUser;
import org.sakaiproject.kernel.api.memory.Cache;
import org.sakaiproject.kernel.api.memory.CacheManagerService;
import org.sakaiproject.kernel.api.memory.CacheScope;
import org.sakaiproject.kernel.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The ClusterTrackingService, maintains an entry for the active server and tracks active
 * users with a cluster replicated shared cache.
 */
@Component(description = "Cluster tracking, tracks app servers and users within the cluster", label = "Cluster Tracking", name = "ClusterTrackingService", immediate = true)
@Services(value = { @Service(value = ClusterTrackingService.class),
    @Service(value = Runnable.class) })
@Properties(value = {
    @Property(name = Scheduler.PROPERTY_SCHEDULER_CONCURRENT, boolValue = false),
    @Property(name = Scheduler.PROPERTY_SCHEDULER_PERIOD, longValue = 300L) })
public class ClusterTrackingServiceImpl implements ClusterTrackingService, Runnable {

  /**
   * The logger for the service.
   */
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterTrackingServiceImpl.class);
  /**
   * The name of the cache used to track users.
   */
  private static final String TRACKING_CACHE = "user-tracking-cache";

  /**
   * The name of the cache used to track users.
   */
  private static final String SERVER_CACHE = "server-tracking-cache";

  /**
   * The Name of the Cookie used to track users.
   */
  private static final String SAKAI_TRACKING = "SAKAI-TRACKING";

  /**
   * The Cache Manager service, injected.
   */
  @Reference
  private CacheManagerService cacheManagerService;

  /**
   * A String representing the time when the service started.
   */
  private String componentStartTime;

  /**
   * The Id of the server, takned from JMX normally processID@server-hostname.
   */
  private String serverId;
  /**
   * True when the service is active.
   */
  private boolean isActive = false;

  /**
   * becomes true when the server is registered
   */
  private boolean isReady = false;
  private int serverNumber;
  private Object lockObject = new Object();
  private long next;
  private long epoch;
  private long prev;

  /**
   * Constructor for testing purposes only.
   * 
   * @param cacheManagerService2
   */
  protected ClusterTrackingServiceImpl(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
    GregorianCalendar calendar = new GregorianCalendar(2009, 8, 22);
    epoch = calendar.getTimeInMillis();
  }

  public ClusterTrackingServiceImpl() {
    GregorianCalendar calendar = new GregorianCalendar(2009, 8, 22);
    epoch = calendar.getTimeInMillis();
  }

  /**
   * Activate the service, getting the id of the jvm instance and register the instance.
   * 
   * @param ctx
   * @throws Exception
   */
  public void activate(ComponentContext ctx) throws Exception {
    componentStartTime = String.valueOf(System.currentTimeMillis());
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = new ObjectName("java.lang:type=Runtime");
    serverId = ((String) mbeanServer.getAttribute(name, "Name")).replace("@", "-");
    isActive = true;
    pingInstance();
    isReady = true;
  }

  /**
   * Remove the registration for the instance.
   * 
   * @param ctx
   * @throws Exception
   */
  public void deactivate(ComponentContext ctx) throws Exception {
    removeInstance(serverId);
  }

  /**
   * Track a user, called from a filter, this will update the central cache for the cookie
   * and set the cookie if not present.
   * 
   * @param request
   *          the http request oject.
   * @param response
   *          the http response object which will, if there is no cookie present, and the
   *          response is not committed, have the cookie set.
   */
  public void trackClusterUser(HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    String remoteUser = request.getRemoteUser();
    boolean tracking = false;
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie != null) {
          String cookieName = cookie.getName();
          if (cookieName.equals(SAKAI_TRACKING)) {
            String trackingCookie = cookie.getValue();
            pingTracking(trackingCookie, remoteUser);
            tracking = true;
          }
        }
      }
    }
    if (!tracking && !response.isCommitted()) {
      // the tracking cookie is the a sha1 hash of the thread, the server startup id and
      // time
      String seed = Thread.currentThread().getName() + ":" + componentStartTime + ":"
          + System.currentTimeMillis();
      String trackingCookie = Thread.currentThread().getName() + ":"
          + System.currentTimeMillis();
      try {
        trackingCookie = serverId + "-" + StringUtils.sha1Hash(seed);
      } catch (Exception e) {
        LOGGER.error("Failed to hash new cookie ", e);
      }

      Cookie cookie = new Cookie(SAKAI_TRACKING, trackingCookie);
      cookie.setMaxAge(-1);
      cookie.setComment("Cluster User Tracking");
      cookie.setPath("/");
      cookie.setVersion(0);
      response.addCookie(cookie);
      // we *do not* track cookies the first time, to avoid DOS on the cookie store.
      // pingTracking(trackingCookie, remoteUser);
    }

  }

  /**
   * Get the user based on a tracking id.
   * 
   * @param trackingCookie
   * @return
   */
  public ClusterUser getUser(String trackingCookie) {
    if ( trackingCookie == null ) {
      return null;
    }
    Cache<ClusterUser> cache = getTrackingCache();
    ClusterUser cuser = cache.get(trackingCookie);
    if (cuser == null) {
      return null;
    } else if (((ClusterUserImpl) cuser).expired()) {
      cache.remove(trackingCookie);
      return null;
    }
    return cuser;
  }

  /**
   * update the tracking for a user, if expired or the user name has changed.
   * 
   * @param trackingCookie
   *          the cookie tracking.
   * @param remoteUser
   *          the user id.
   */
  private void pingTracking(String trackingCookie, String remoteUser) {
    Cache<ClusterUser> cache = getTrackingCache();
    ClusterUser cuser = cache.get(trackingCookie);
    if (cuser == null || ((ClusterUserImpl) cuser).expired(remoteUser)) {
      cache.put(trackingCookie, new ClusterUserImpl(remoteUser, serverId));
    }
  }

  /**
   * Update the server registration.
   */
  private void pingInstance() {
    if (isActive) {
      if (!isReady) {
        do {
          updateServerNumber();
          getServerCache().put(serverId, new ClusterServerImpl(serverId, serverNumber));
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {

          }
        } while (!checkServerNumber());

      } else {
        Object cs = getServerCache().put(serverId,
            new ClusterServerImpl(serverId, serverNumber));

        if (cs == null) {
          LOGGER.warn("This servers registration dissapeared, replaced as {} ", serverId);
        }
      }
    }
  }

  /**
   * @return true if the only server number registered is our server number
   */
  private boolean checkServerNumber() {
    List<ClusterServer> servers = getServerCache().list();
    for (ClusterServer server : servers) {
      if (server.getServerNumber() == serverNumber
          && !serverId.equals(server.getServerId())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Find the next unique server number.
   */
  private void updateServerNumber() {
    List<ClusterServer> servers = getServerCache().list();
    boolean[] spare = new boolean[servers.size()];
    for (int i = 0; i < spare.length; i++) {
      spare[i] = true;
    }
    for (ClusterServer server : servers) {
      int i = server.getServerNumber();
      if (i < spare.length) {
        spare[i] = false;
      }
    }
    serverNumber = spare.length;
    for (int i = 0; i < spare.length; i++) {
      if (spare[i]) {
        serverNumber = i;
        break;
      }
    }
  }

  /**
   * @param serverId
   *          remove the instance from the cluster wide cache.
   */
  private void removeInstance(String serverId) {
    getServerCache().remove(serverId);
  }

  /**
   * @return the cache used to store server registrations.
   */
  private Cache<ClusterServer> getServerCache() {
    return cacheManagerService.getCache(SERVER_CACHE, CacheScope.CLUSTERREPLICATED);
  }

  /**
   * @return the Cache used to track users
   */
  private Cache<ClusterUser> getTrackingCache() {
    return cacheManagerService.getCache(TRACKING_CACHE, CacheScope.CLUSTERREPLICATED);
  }

  /**
   * {@inheritDoc}
   * 
   * Invoked as a task by the Sling Scheduler, once every 5 minutes to update the last
   * time the server was registered in the cluster cache.
   * 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    pingInstance();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.cluster.ClusterTrackingService#getAllServers()
   */
  public List<ClusterServer> getAllServers() {
    return getServerCache().list();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.cluster.ClusterTrackingService#getCurrentServerId()
   */
  public String getCurrentServerId() {
    return serverId;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.kernel.api.cluster.ClusterTrackingService#getClusterUniqueId()
   */
  public String getClusterUniqueId() {
    synchronized (lockObject) {
      do {
        next = System.currentTimeMillis() - epoch;
      } while (next == prev);
    }
    BigInteger idNum = new BigInteger(String.valueOf(serverNumber) + String.valueOf(next));
    prev = next;
    Base64 b64 = new Base64();
    return b64.encodeToString(idNum.toByteArray()).trim();
  }

}
