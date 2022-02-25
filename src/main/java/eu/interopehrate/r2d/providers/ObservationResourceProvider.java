package eu.interopehrate.r2d.providers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import eu.interopehrate.r2d.exceptions.R2DException;

@Service
public class ObservationResourceProvider extends AbstractResourceProvider {

	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

	@Search
	public List<Observation> search(@RequiredParam(name = Observation.SP_STATUS) String theStatus,
			@OptionalParam(name = Observation.SP_DATE) DateParam theFromDate,
			@OptionalParam(name = Observation.SP_CATEGORY) TokenParam theCategory,
			@OptionalParam(name = Observation.SP_CODE) TokenParam theCode,
			@OptionalParam(name = "_sort") String theSort, 
			@OptionalParam(name = "_count") String theCount, 
			@OptionalParam(name = "_summary") String isSummary, 
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws R2DException {
		
		throw new NotImplementedOperationException("Sorry, this request is not yet implemented...");
	}
}
