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
package org.sakaiproject.kernel.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPResponseQueue;

import org.sakaiproject.kernel.api.ldap.LdapConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * Extension of standard <code>LDAPConnection</code> that attempts to return
 * objects to the pool if someone just drops them out of scope.
 * @see PoolingLdapConnectionManager
 * @see PooledLDAPConnectionFactory
 * @author John Lewis, Unicon Inc
 */
public class PooledLDAPConnection extends LDAPConnection {

	/** Class-specific logger */
  private static Logger log = LoggerFactory.getLogger(PooledLDAPConnection.class);

	/** is this pooled connection currently active (i.e. used) */
	private boolean active = false;

	/** the controlling connection manager */
	private LdapConnectionManager connectionManager;

	/** track if a bind operation has been attempted since it was last reset */
	private boolean bindAttempted = false;

	private long birthdate = new Date().getTime();

	/**
	 * protect against pooled connections leaking out of the pool
	 */
  @Override
  protected void finalize() throws LDAPException {
		if (active) {
			if (connectionManager != null) {
        log.warn("went out-of-scope without being returned to the pool -- returning myself");
				connectionManager.returnConnection(this);
			} else {
				log.error("went out-of-scope without being returned to the pool -- unable to clean up since no connectionManager is set!");
				super.finalize();
			}
		} else {
			super.finalize();
		}
	}

	/*
	 * Override all the bind methods to capture that they were attempted.
	 * We need this to detect that someone has modified the binding from the
	 * settings for the pool so that we can reset it accordingly.
	 * Tried using LDAPConnection.isBound and LDAPConnection.getAuthenticationDN
	 * to do this, but these do not seem to work correctly when a secondary bind
	 * attempt fails due to bad credentials.
	 */

	@Override
  public void bind(int i, String s, byte[] abyte0, LDAPConstraints ldapconstraints) throws LDAPException {
		bindAttempted = true;
		super.bind(i, s, abyte0, ldapconstraints);
	}

	@Override
  public LDAPResponseQueue bind(int i, String s, byte[] abyte0, LDAPResponseQueue ldapresponsequeue, LDAPConstraints ldapconstraints) throws LDAPException {
		bindAttempted = true;
		return super.bind(i, s, abyte0, ldapresponsequeue, ldapconstraints);
	}

	@Override
  public LDAPResponseQueue bind(int i, String s, byte[] abyte0, LDAPResponseQueue ldapresponsequeue) throws LDAPException {
		bindAttempted = true;
		return super.bind(i, s, abyte0, ldapresponsequeue);
	}

	@Override
  public void bind(int i, String s, byte[] abyte0) throws LDAPException {
		bindAttempted = true;
		super.bind(i, s, abyte0);
	}

	@SuppressWarnings("unchecked")
  @Override
  public void bind(String s, String s1, Map map, Object obj, LDAPConstraints ldapconstraints) throws LDAPException {
		bindAttempted = true;
		super.bind(s, s1, map, obj, ldapconstraints);
	}

	@SuppressWarnings("unchecked")
  @Override
  public void bind(String s, String s1, Map map, Object obj) throws LDAPException {
		bindAttempted = true;
		super.bind(s, s1, map, obj);
	}

	@SuppressWarnings("unchecked")
  @Override
  public void bind(String s, String s1, String[] as, Map map, Object obj, LDAPConstraints ldapconstraints) throws LDAPException {
		bindAttempted = true;
		super.bind(s, s1, as, map, obj, ldapconstraints);
	}

	@SuppressWarnings("unchecked")
  @Override
  public void bind(String s, String s1, String[] as, Map map, Object obj) throws LDAPException {
		bindAttempted = true;
		super.bind(s, s1, as, map, obj);
	}

  @Override
  @Deprecated
  public void bind(String s, String s1) throws LDAPException {
    bindAttempted = true;
    super.bind(s, s1);
  }

  @Override
  @Deprecated
  public void bind(int version, String dn, String passwd, LDAPConstraints cons)
      throws LDAPException {
    bindAttempted = true;
    super.bind(version, dn, passwd, cons);
  }

  @Override
  @Deprecated
  public void bind(int version, String dn, String passwd) throws LDAPException {
    bindAttempted = true;
    super.bind(version, dn, passwd);
  }

  @Override
  @Deprecated
  public void bind(String dn, String passwd, LDAPConstraints cons) throws LDAPException {
    bindAttempted = true;
    super.bind(dn, passwd, cons);
  }

  public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isBindAttempted() {
		return bindAttempted;
	}

	public void setBindAttempted(boolean bindAttempted) {
		this.bindAttempted = bindAttempted;
	}

	public LdapConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public void setConnectionManager(LdapConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public long getBirthdate() {
		return birthdate;
	}
}
