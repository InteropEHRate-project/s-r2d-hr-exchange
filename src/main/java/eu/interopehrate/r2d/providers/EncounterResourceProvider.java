package eu.interopehrate.r2d.providers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.Citizen;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.security.SecurityConstants;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: FHIR provider of the Encounter resource.
 */

@Service
public class EncounterResourceProvider extends AbstractResourceProvider {
	 
	@Override
	public Class<Encounter> getResourceType() {
		return Encounter.class;
	}

	@Search
	public List<Encounter> search(@RequiredParam(name = Encounter.SP_STATUS) String theStatus,
			@OptionalParam(name = Encounter.SP_DATE) DateParam theFromDate,
			@OptionalParam(name = "_sort") String theSort, 
			@OptionalParam(name = "_count") String theCount, 
			@OptionalParam(name = "_summary") String isSummary, 
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws R2DException {

		// sends the request to the EHR
		R2DRequest r2dRequest = (R2DRequest) theRequest.getAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME);
		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		String authToken = theRequest.getHeader(SecurityConstants.AUTH_HEADER);
		// starts request processing
		try {
			requestProcessor.startRequestProcessing(r2dRequest, citizen.getPersonIdentifier(), authToken);
		} catch (R2DException r2de) {
			throw new InternalErrorException(r2de.getCause());
		}
				
		// Always returns an empty list, the fullfilled one 
		// will be retrived by the REST callback implemented 
		// by the EHR-MW and invoked by the EHR
		return new ArrayList<Encounter>();
	}
	
	
	@Operation(name="$everything", idempotent=true)
	public Bundle everything(@IdParam IdType theEncounterId, HttpServletRequest theRequest) throws R2DException {
		// sends the request to the EHR
		R2DRequest r2dRequest = (R2DRequest) theRequest.getAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME);
		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		String authToken = theRequest.getHeader(SecurityConstants.AUTH_HEADER);
		// starts request processing
		try {
			requestProcessor.startRequestProcessing(r2dRequest, citizen.getPersonIdentifier(), authToken);
		} catch (R2DException r2de) {
			throw new InternalErrorException(r2de.getCause());
		}
		
		return new Bundle();
	}
	
	
}
