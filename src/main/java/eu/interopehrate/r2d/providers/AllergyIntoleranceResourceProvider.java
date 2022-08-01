package eu.interopehrate.r2d.providers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import eu.interopehrate.r2d.exceptions.R2DException;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: FHIR provider of the AllergyIntolerance resource.
 */

@Service
public class AllergyIntoleranceResourceProvider extends AbstractResourceProvider {

	@Override
	public Class<AllergyIntolerance> getResourceType() {
		return AllergyIntolerance.class;
	}

	
	@Search
	public List<AllergyIntolerance> search(@RequiredParam(name = AllergyIntolerance.SP_VERIFICATION_STATUS) String theStatus,
			@OptionalParam(name = AllergyIntolerance.SP_DATE) DateParam theFromDate,
			@OptionalParam(name = AllergyIntolerance.SP_CATEGORY) TokenParam theCategory,
			@OptionalParam(name = AllergyIntolerance.SP_TYPE) TokenParam theType,
			@OptionalParam(name = "_sort") String theSort, 
			@OptionalParam(name = "_count") String theCount, 
			@OptionalParam(name = "_summary") String isSummary, 
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws R2DException {
		
		
		throw new NotImplementedOperationException("Sorry, this request is not yet implemented...");
	}
	
}
