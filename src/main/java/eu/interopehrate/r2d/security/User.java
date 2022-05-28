package eu.interopehrate.r2d.security;

import eu.interopehrate.r2d.model.Citizen;

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
