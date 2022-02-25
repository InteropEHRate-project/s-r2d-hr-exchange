package eu.interopehrate.r2d.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

public class TooManyRequestException extends BaseServerResponseException {

	private static final long serialVersionUID = 4313928868673303957L;
	
	public TooManyRequestException(String theMessage) {
		super(429, theMessage);
	}

}
