package eu.interopehrate.r2d.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Exception thrown to clients if the same client has too many
 * requests with the RUNNING status.
 */
public class TooManyRequestException extends BaseServerResponseException {

	private static final long serialVersionUID = 4313928868673303957L;
	
	public TooManyRequestException(String theMessage) {
		super(429, theMessage);
	}

}
