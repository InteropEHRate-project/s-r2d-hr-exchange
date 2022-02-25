package eu.interopehrate.r2d.services;

import javax.naming.CommunicationException;

import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.R2DRequest;

public interface EHRService {

	/**
	 * 
	 * @param theRequest
	 * @param eidasToken
	 * @throws CommunicationException
	 */
	void sendRequest(R2DRequest r2dRequest, String eidasToken) throws R2DException;
	
}
