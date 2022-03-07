package eu.interopehrate.r2d.providers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.Citizen;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.security.SecurityConstants;

@Service
public class DiagnosticReportResourceProvider extends AbstractResourceProvider {

	@Override
	public Class<DiagnosticReport> getResourceType() {
		return DiagnosticReport.class;
	}

	@Search
	public List<DiagnosticReport> search(@RequiredParam(name = DiagnosticReport.SP_STATUS) String theStatus,
			@OptionalParam(name = DiagnosticReport.SP_DATE) DateParam theFromDate,
			@OptionalParam(name = DiagnosticReport.SP_CATEGORY) TokenParam theCategory,
			@OptionalParam(name = DiagnosticReport.SP_CODE) TokenParam theCode,
			@OptionalParam(name = "_sort") String theSort, 
			@OptionalParam(name = "_count") String theCount, 
			@OptionalParam(name = "_summary") String isSummary, 
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws R2DException {
		
		throw new NotImplementedOperationException("Sorry, this request is not yet implemented...");
	}
	
	@Operation(name="$everything", idempotent=true)
	public Bundle everything(@IdParam IdType theEncounterId, HttpServletRequest theRequest) throws R2DException {
		// sends the request to the EHR
		R2DRequest r2dRequest = (R2DRequest) theRequest.getAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME);
		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		String authToken = theRequest.getHeader(SecurityConstants.AUTH_HEADER);
		// starts request processing
		requestProcessor.startRequestProcessing(r2dRequest, citizen.getPersonIdentifier(), authToken);
		
		return new Bundle();
	}
	
}
