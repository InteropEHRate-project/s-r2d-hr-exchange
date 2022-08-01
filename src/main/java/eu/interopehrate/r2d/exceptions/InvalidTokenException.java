package eu.interopehrate.r2d.exceptions;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Exception thrown to clients if the authentication token is not valid.
 */
public class InvalidTokenException extends Exception {

	private static final long serialVersionUID = 1279429638464017181L;

	public InvalidTokenException() {
	}
	
	public InvalidTokenException(String message) {
		super(message);
	}

	
}
