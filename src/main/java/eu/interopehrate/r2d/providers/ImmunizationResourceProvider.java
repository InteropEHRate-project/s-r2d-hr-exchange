package eu.interopehrate.r2d.providers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Immunization;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import eu.interopehrate.r2d.exceptions.R2DException;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: FHIR provider of the Immunization resource.
 */

@Service
public class ImmunizationResourceProvider extends AbstractResourceProvider {

	@Override
	public Class<Immunization> getResourceType() {
		return Immunization.class;
	}

	@Search
	public List<Immunization> search(@RequiredParam(name = Immunization.SP_STATUS) String theStatus,
			@OptionalParam(name = Immunization.SP_DATE) DateParam theFromDate,
			@OptionalParam(name = "_sort") String theSort, 
			@OptionalParam(name = "_count") String theCount, 
			@OptionalParam(name = "_summary") String isSummary, 
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws R2DException {
		
		throw new NotImplementedOperationException("Sorry, this request is not yet implemented...");
	}
}
