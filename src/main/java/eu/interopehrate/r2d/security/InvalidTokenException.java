package eu.interopehrate.r2d.security;

public class InvalidTokenException extends Exception {

	private static final long serialVersionUID = 1279429638464017181L;

	public InvalidTokenException() {
	}
	
	public InvalidTokenException(String message) {
		super(message);
	}

	
}
