package eu.interopehrate.r2d.providers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.Citizen;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.security.SecurityConstants;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: FHIR provider of the Patient resource.
 */

@Service
public class PatientResourceProvider extends AbstractResourceProvider {

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Patient.class;
	}

	@Search
	public List<Patient> search() throws R2DException {
		throw new NotImplementedOperationException("Sorry, this request is not yet implemented...");
	}
	

	@Operation(name="$everything", idempotent=true)
	public Bundle everything(HttpServletRequest theRequest) throws R2DException {
		throw new NotImplementedOperationException("Sorry, this request is not yet implemented...");
	}
	
	
	@Operation(name="$patient-summary", idempotent=true)
	public Bundle patientSummary(HttpServletRequest theRequest) throws R2DException {
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
