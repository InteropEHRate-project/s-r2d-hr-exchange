package eu.interopehrate.r2d.model;

public class Citizen {
	private String firstName;
	private String familyName;
	private String dateOfBirth;
	private String personIdentifier;
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getFamilyName() {
		return familyName;
	}
	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}
	public String getDateOfBirth() {
		return dateOfBirth;
	}
	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
	public String getPersonIdentifier() {
		return personIdentifier;
	}
	public void setPersonIdentifier(String personIdentifier) {
		this.personIdentifier = personIdentifier;
	}

}
