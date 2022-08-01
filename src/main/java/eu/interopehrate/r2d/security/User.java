package eu.interopehrate.r2d.security;

import eu.interopehrate.r2d.model.Citizen;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: User that submitted a request to the R2D. The User
 * can be: 1) a Citizen identified by an EIDAS token (in this case also
 *            an instance of Citizen will be created).
 *         2) the EHR-Middleware sending callback about request processing status.
 *         3) a system administrator.
 */

public class User {
	
	private UserRole role;
	private Citizen citizen;
	
	
	public User(UserRole role) {
		this.role = role;
	}

	public User(Citizen citizen) {
		super();
		this.citizen = citizen;
		this.role = UserRole.CITIZEN;
	}

	
	public Citizen getCitizen() {
		return citizen;
	}
	
	void setCitizen(Citizen citizen) {
		this.citizen = citizen;
	}

	public UserRole getRole() {
		return role;
	}
	
}
