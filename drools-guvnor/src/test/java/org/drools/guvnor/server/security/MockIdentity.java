/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.server.security;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.login.LoginException;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.permission.PermissionResolver;

public class MockIdentity extends Identity {
	
	private static final long serialVersionUID = 3159602570340648366L;
	private boolean hasRole;
	private Set<String> roles = new HashSet<String>();
	private List<PermissionResolver> resolvers = new ArrayList<PermissionResolver>();
	private boolean isLoggedIn = true;
    boolean loggoutCalled = true;
    boolean allowLogin = true;
    boolean checkPermission = false;

    @Override
	public boolean addRole(String r) {
		roles.add(r);
		return true;
	}

	public boolean hasRole(String role) {
		return hasRole || roles.contains(role);
	}



	public void setHasRole(boolean hasRole) {
		this.hasRole = hasRole;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}
	
	public void setIsLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

	public boolean isLoggedIn(boolean attemptLogin) {
		return true;
	}

    @Override
     public void logout() {
        this.loggoutCalled = true;
    }

    public boolean hasPermission(Object target, String action) {
	      for (PermissionResolver resolver : resolvers)
	      {
	         if (resolver.hasPermission(target, action))
	         {
	            return true;
	         }
	      }

	      return false;
	}

	public void addPermissionResolver(PermissionResolver r) {
		resolvers.add(r);
	}

	/**
	 * Push this mock as the identity to Seam.
	 */
	public void inject() {
		Contexts.getSessionContext().set("org.jboss.seam.security.identity",
				this);
	}

	public Credentials getCredentials() {
		return new Credentials() {
			 
			private static final long serialVersionUID = -6746530728283388952L;

			public String getUsername()
			   {
			      return "mockedUser";
			   }
		};
	}   
	
	public void authenticate() throws LoginException {
		if(allowLogin) {
			return;
		}
		
		throw new LoginException();
	}
	
	public void setAllowLogin (boolean allowLogin) {
		this.allowLogin = allowLogin;
	}
	
	public void setCheckPermission (boolean checkPermission) {
		this.checkPermission = checkPermission;
	}
	
	public void checkPermission(Object target, String action ) {
		if(checkPermission) {
		System.out.println("MockIdentity.checkPermission");
		} else {
			super.checkPermission(target, action);
		}
	}
}
