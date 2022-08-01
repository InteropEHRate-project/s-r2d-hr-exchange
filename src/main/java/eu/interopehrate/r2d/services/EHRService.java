package eu.interopehrate.r2d.services;

import javax.naming.CommunicationException;

import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.R2DRequest;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: interface of the service used to send requests
 * to the EHR Middleware.
 */

public interface EHRService {

	/**
	 * Forwards a request to the EHR Middleware
	 * 
	 * @param theRequest
	 * @param eidasToken
	 * @throws CommunicationException
	 */
	void sendRequest(R2DRequest r2dRequest, String eidasToken) throws R2DException;
	
}
