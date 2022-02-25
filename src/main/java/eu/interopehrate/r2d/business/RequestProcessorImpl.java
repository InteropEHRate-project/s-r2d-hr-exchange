package eu.interopehrate.r2d.business;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.dao.RequestRepository;
import eu.interopehrate.r2d.dao.ResponseRepository;
import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.exceptions.TooManyRequestException;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.model.R2DResponse;
import eu.interopehrate.r2d.model.RequestStatus;
import eu.interopehrate.r2d.services.EHRService;
import eu.interopehrate.r2d.utils.URLUtility;

@Component
public class RequestProcessorImpl implements RequestProcessor {
	private static FhirContext fhirContext;
	private static final Logger logger = LoggerFactory.getLogger(RequestProcessorImpl.class);
	private static final String MAX_CONCURRENT_REQUEST_PROPERTY =  "r2da.maxConcurrentRunningRequestPerDay";
	private static int MAX_CONCURRENT_REQUEST;
	private static final String CACHE_DURATION_PROPERTY =  "r2da.cacheDurationInDays";
	private static int CACHE_DURATION_IN_DAYS;

	private IParser parser;

	@Autowired(required = true)
	private EHRService ehrService;
	
	@Autowired(required = true)
	private ResponseRepository responseRepository;

	@Autowired(required = true)
	private RequestRepository requestRepository;
	
	
	public RequestProcessorImpl() {
		super();
		if (fhirContext == null)
			fhirContext = FhirContext.forR4();
		
		// Retrieves MAX_CONCURRENT_REQUEST from properties file
		MAX_CONCURRENT_REQUEST = Integer.parseInt(Configuration.getProperty(MAX_CONCURRENT_REQUEST_PROPERTY));
		// Retrieves CACHE_DURATION_IN_DAYS from properties file
		CACHE_DURATION_IN_DAYS = Integer.parseInt(Configuration.getProperty(CACHE_DURATION_PROPERTY));
		
    	parser = fhirContext.newJsonParser();
	}


	@Override
	public synchronized R2DRequest newIncomingRequest(String r2dUrl, String eidasCitizenId) throws R2DException {
		String r2dQuery = URLUtility.extractR2DSubQuery(r2dUrl);
		
		// Before accepting the request some checks must be executed
		
		// #1 Checks how many running requests the citizen has
		if (MAX_CONCURRENT_REQUEST > 0) {
			LocalDateTime now = LocalDateTime.now();
			Date to = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
			
			LocalDateTime _24HoursFromNow = now.minusHours(24);
			Date from = Date.from(_24HoursFromNow.atZone(ZoneId.systemDefault()).toInstant());
			
			long size = requestRepository.findRunningRequestOfTheCitizen(eidasCitizenId,
					from, to);
			
			if (size == MAX_CONCURRENT_REQUEST)
				throw new TooManyRequestException(
						String.format("Too many concurrent running request: %d. Please try later.", size));			
		}
		
		R2DRequest newR2DRequest = new R2DRequest(r2dQuery, eidasCitizenId);
		newR2DRequest = requestRepository.save(newR2DRequest);
		logger.info(String.format("Created persistent request: %s for URL: %s", newR2DRequest.getId(), newR2DRequest.getUri()));		
		
		return newR2DRequest;
	}

	
	@Override
	public R2DRequest startRequestProcessing(R2DRequest r2dRequest, String eidasPersonIdentifier) throws R2DException {
		if (r2dRequest.getStatus() != RequestStatus.NEW)
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to start it.", 
							r2dRequest.getStatus(), r2dRequest.getId()));

		try {
			R2DRequest equivalentRequest = null;
			// #1 checks if there is a cached response
			System.out.println(CACHE_DURATION_IN_DAYS);
			if (CACHE_DURATION_IN_DAYS > 0) {
				logger.debug("Looks for a valid cached response...");
				
				LocalDateTime toLtd = LocalDateTime.now();
				LocalDateTime fromLdt  = toLtd.minusDays(CACHE_DURATION_IN_DAYS);
				Date to = Date.from(toLtd.atZone(ZoneId.systemDefault()).toInstant());
				Date from = Date.from(fromLdt.atZone(ZoneId.systemDefault()).toInstant());
				
				System.out.println(from);
				System.out.println(to);				
				System.out.println(r2dRequest.getUri());
				System.out.println(eidasPersonIdentifier);
				List<R2DRequest> requests = requestRepository.findEquivalentValidRequest(
						eidasPersonIdentifier, r2dRequest.getUri(), 
						from, to);
				// retrieves the most recent equivalent request if one
				logger.debug(String.format("Found %d cached request(s)", requests.size()));
				if (requests.size() > 0)
					equivalentRequest = requests.get(0);
			} 
			
			if (equivalentRequest != null) {
				// #2.1 if there is an equivalent request
				logger.debug("Found a valid cached response: " + equivalentRequest.getId());
				r2dRequest.addResponseId(equivalentRequest.getId());
				r2dRequest.setStatus(RequestStatus.COMPLETED);
			} else {
				// #2.2 if there is no cached response sends the request to the EHR
				logger.debug("No cached response, sends request to EHR...");
				ehrService.sendRequest(r2dRequest, eidasPersonIdentifier);
				r2dRequest.setStatus(RequestStatus.RUNNING);
			}
			
			// #3 saves to DB the updated version of the R2DRequest
			return requestRepository.save(r2dRequest);
		} catch (Exception e) {
			r2dRequest.setStatus(RequestStatus.FAILED);
			r2dRequest.setFailureMessage(e.getMessage());
			requestRepository.save(r2dRequest);
			
			throw new R2DException(R2DException.COMMUNICATION_ERROR, e.getMessage());
		}
	}

	
	@Override
	public void requestCompletedSuccesfully(String requestId, String jsonBundle) throws R2DException {
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));

		
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() == RequestStatus.RUNNING || 
			theR2DRequest.getStatus() == RequestStatus.PARTIALLY_COMPLETED) {
			
			// #2.1 Parse results to verifies the bundle
			if (jsonBundle != null && jsonBundle.trim().length() > 0 ) {
				Bundle theBundle = null;
				try {
					theBundle = parser.parseResource(Bundle.class, jsonBundle);
					logger.debug(String.format("Response contains a valid Bundle with %d entries: ", theBundle.getEntry().size()));
					// TODO: check the signature of the resources
					
					// #2.2 Store response to the DB
					R2DResponse response = new R2DResponse();
					response.setResponse(jsonBundle);
					response.setCitizenId(theR2DRequest.getCitizenId());
					responseRepository.save(response);

					// #2.2 Update status of the request to the DB
					theR2DRequest.addResponseId(response.getId());
					theR2DRequest.setStatus(RequestStatus.COMPLETED);
					requestRepository.save(theR2DRequest);			
					logger.debug(String.format("Request %s succesfully completed the execution!", requestId));
					
				} catch (Exception e) {
					logger.error(String.format("Error while parsing the received bundle: %s ", e.getMessage()));
					logger.error(e.getMessage(), e);
					// update request status
					theR2DRequest.setStatus(RequestStatus.FAILED);
					String failureMsg = "The received bundle is not valid: " + e.getMessage();
					theR2DRequest.setFailureMessage(failureMsg);
					requestRepository.save(theR2DRequest);
				}			
			
			}
		} else {
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to elaborate it.", 
							theR2DRequest.getStatus(), requestId));
		}
		
	}
	

	@Override
	public void requestCompletedUnsuccesfully(String requestId, String failureMsg) throws R2DException {
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));
		
		
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() == RequestStatus.RUNNING || theR2DRequest.getStatus() == RequestStatus.PARTIALLY_COMPLETED) {
			// update request status
			theR2DRequest.setStatus(RequestStatus.FAILED);
			theR2DRequest.setFailureMessage(failureMsg);
			requestRepository.save(theR2DRequest);			
			logger.debug(String.format("Request %s unsuccesfully completed the execution!", requestId));
		}  else {
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id %s does not allow to elaborate it.", 
							theR2DRequest.getStatus(), requestId));
		}

	}
	
	
	@Override
	public void requestProducedPartialResult(String requestId, String jsonBundle) throws R2DException {
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));
		
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() == RequestStatus.RUNNING || theR2DRequest.getStatus() == RequestStatus.PARTIALLY_COMPLETED) {
			
			// #2.1 Parse results to verifies the bundle
			if (jsonBundle != null && jsonBundle.trim().length() > 0 ) {
				Bundle theBundle = null;
				try {
					theBundle = parser.parseResource(Bundle.class, jsonBundle);
					logger.debug(String.format("Response contains a valid Bundle with %d entries: ", theBundle.getEntry().size()));
					// TODO: check the signature of the resources
					
					// #2.2 Store response to the DB
					R2DResponse response = new R2DResponse();
					response.setResponse(jsonBundle);
					response.setCitizenId(theR2DRequest.getCitizenId());
					responseRepository.save(response);

					// #2.3 Update status of the request to the DB
					theR2DRequest.setStatus(RequestStatus.PARTIALLY_COMPLETED);
					theR2DRequest.addResponseId(response.getId());
					requestRepository.save(theR2DRequest);			
					logger.debug(String.format("Partial result of request %s succesfully stored!", requestId));

				} catch (Exception e) {
					logger.error(String.format("Error while parsing the received bundle: %s ", e.getMessage()));
					logger.error(e.getMessage(), e);
					// update request status
					theR2DRequest.setStatus(RequestStatus.FAILED);
					String failureMsg = "The received bundle is not valid: " + e.getMessage();
					theR2DRequest.setFailureMessage(failureMsg);
					requestRepository.save(theR2DRequest);

				}
			}
		} else {
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to elaborate it.", 
							theR2DRequest.getStatus(), requestId));
		}		
		
	}

}
