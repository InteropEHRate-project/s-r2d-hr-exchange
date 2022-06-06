package eu.interopehrate.r2d.rest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.dao.RequestRepository;
import eu.interopehrate.r2d.dao.ResponseRepository;
import eu.interopehrate.r2d.model.Citizen;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.model.R2DResponse;
import eu.interopehrate.r2d.model.RequestOutcome;
import eu.interopehrate.r2d.model.RequestOutput;
import eu.interopehrate.r2d.model.RequestStatus;
import eu.interopehrate.r2d.security.SecurityConstants;

@RestController
@RequestMapping("/requests")
public class RESTRequestController {
	private static final Logger logger = LoggerFactory.getLogger(RESTRequestController.class);
	
	@Autowired(required = true)
	private RequestRepository requestRepository;

	@Autowired(required = true)
	private ResponseRepository responseRepository;
	
	
	
	/**
	 * Returns the list of all the request of the citizen
	 * 
	 * @param theRequest
	 * @param theResponse
	 * @return
	 */
	@GetMapping(produces = "application/json")
	public Collection<R2DRequest> listAllCitizenRequests(HttpServletRequest theRequest,
			HttpServletResponse theResponse) {

		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		
		return requestRepository.findByCitizenId(citizen.getPersonIdentifier());
	}
	
	/**
	 * Returns the most recent requests made by the citizen.
	 * 
	 * @param theRequest
	 * @param theResponse
	 * @return
	 */
	@GetMapping(path="/last", produces = "application/json")
	public Collection<R2DRequest> listLastCitizenRequests(@RequestParam(name = "n", defaultValue = "5", required=false) int n,
			HttpServletRequest theRequest,
			HttpServletResponse theResponse) {

		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		
		Collection<R2DRequest> allRequests = requestRepository.findByCitizenId(citizen.getPersonIdentifier());
		int max = allRequests.size() > n ? n : allRequests.size();
		List<R2DRequest> lastRequests = new ArrayList<R2DRequest>();
		allRequests.forEach(r2dRequest -> {
			if (lastRequests.size() < max)
				lastRequests.add(r2dRequest);
			else
				return;
		});
		
		return lastRequests;
	}
	
	/**
	 * Returns the most recent requests made by the citizen.
	 * 
	 * @param theRequest
	 * @param theResponse
	 * @return
	 */
	@GetMapping(path="/running", produces = "application/json")
	public Collection<R2DRequest> listRunningCitizenRequests(HttpServletRequest theRequest,
			HttpServletResponse theResponse) {

		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);		
		return requestRepository.findRunningRequestOfTheCitizen(citizen.getPersonIdentifier());
	}
	
	/**
	 * Shows data of a request
	 * 
	 * @param theRequestId
	 * @param theRequest
	 * @param theResponse
	 * @return
	 * @throws IOException
	 */
	@GetMapping(path = "/{theRequestId}", produces = "application/json")
	public R2DRequest getRequestById(@PathVariable String theRequestId, HttpServletRequest theRequest, 
			HttpServletResponse theResponse) throws IOException {
		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		
		Optional<R2DRequest> opt = requestRepository.findByRequestIdAndCitizenId(theRequestId, citizen.getPersonIdentifier());
		if (!opt.isPresent()) {
			theResponse.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(
					"Request with id %s not found or not belonging to requesting citizen.", theRequestId));
			return null;
		} else
			return opt.get();
	}

	
	/**
	 * Monitor the status of a request
	 * 
	 * @param theRequestId
	 * @param theRequest
	 * @param theResponse
	 * @return
	 * @throws IOException
	 */
	@GetMapping(path = "/{theRequestId}/status", produces = "application/json")
	public RequestOutcome monitorRequestStatus(@PathVariable String theRequestId,
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws IOException {
		
		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		
		Optional<R2DRequest> opt = requestRepository.findByRequestIdAndCitizenId(theRequestId, citizen.getPersonIdentifier());
		if (!opt.isPresent()) {
			theResponse.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(
					"Request with id %s not found or not belonging to requesting citizen.", theRequestId));
			return null;
		}
		

		R2DRequest theR2DRequest = opt.get();
		if (theR2DRequest.getStatus() == RequestStatus.RUNNING) {
			theResponse.sendError(HttpServletResponse.SC_ACCEPTED, 
					"Your request is still under processing, please use again this URL to monitor it.");
			return null;
		} 
		
		if (theR2DRequest.getStatus() == RequestStatus.FAILED) {
			RequestOutcome outcome = new RequestOutcome(theR2DRequest.getUri());
			outcome.setError(theR2DRequest.getFailureMessage());
			
			return outcome;
		}
		
		if (theR2DRequest.getStatus() == RequestStatus.COMPLETED) {
			Optional<R2DResponse> optResp = responseRepository.findById(theR2DRequest.getFirstResponseId());
			
			StringBuilder responseURL = new StringBuilder(Configuration.getR2DServicesContextPath());
			responseURL.append("/requests/").append(theR2DRequest.getId())
			.append("/response/").append(optResp.get().getId());
			
			RequestOutcome outcome = new RequestOutcome(theR2DRequest.getUri());
			outcome.addOutput(new RequestOutput("Bundle", responseURL.toString()));	
			
			return outcome;
		} 

		return null;
	}
	
	
	/**
	 * Return the response of a request.
	 * 
	 * @param theRequestId
	 * @param theResponseId
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 * @throws IOException
	 */
	@GetMapping(path = "/{theRequestId}/response/{theResponseId}", produces = "application/json")
	public void getRequestResults(@PathVariable String theRequestId,
			@PathVariable String theResponseId,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

		// Checks if requests belongs to requesting citizen
		Citizen citizen = (Citizen)httpRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		Optional<R2DRequest> opt = requestRepository.findByRequestIdAndCitizenId(theRequestId, citizen.getPersonIdentifier());
		if (!opt.isPresent()) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(
					"Request with id %s not found or not belonging to requesting citizen.", theRequestId));
			return;
		}
		
		// Return the results if the status is COMPLETED
		R2DRequest r2dRequest = opt.get();
		if (r2dRequest.getStatus() == RequestStatus.COMPLETED) {
			Optional<R2DResponse> optResponse = responseRepository.findById(r2dRequest.getFirstResponseId());
			if (optResponse.isPresent()) {
				String responseFileName =  optResponse.get().getResponseFileName();
				File fhirFile = new File(responseFileName);
				if (!fhirFile.exists()) {
					String msg = "This request is too old, the response bundle has expired.";	
					logger.error(msg);
					httpResponse.sendError(HttpServletResponse.SC_NO_CONTENT, msg);
				}
				
				try (InputStream response = new BufferedInputStream(new FileInputStream(fhirFile))) {
					IOUtils.copy(response, httpResponse.getOutputStream());
				}
			}
		} else {
			String msg = String.format("The status %s of the request %s does not allow to retrieve the results.", 
					r2dRequest.getStatus(), r2dRequest.getId());	
			logger.error(msg);
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
		}
	}

	/**
	 * Aborts a running request. 
	 * Method allowed only to administrator to abort a RUNNING or PARTIALLY_COMPLETED request.
	 * 
	 * @param theRequestId
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 * @throws IOException
	 */
	@PostMapping(path = "/{theRequestId}/abort")
	public String abortRequest(@PathVariable String theRequestId, HttpServletRequest httpRequest, 
			HttpServletResponse httpResponse) throws IOException {
		
		Optional<R2DRequest> opt = requestRepository.findById(theRequestId);
		String message = "";
		if (!opt.isPresent()) {
			message = String.format("Request with id %s not found.", theRequestId);
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, message);
			return message;
		}
		
		// Return the results if the status is COMPLETED
		R2DRequest r2dRequest = opt.get();
		if (r2dRequest.getStatus() == RequestStatus.RUNNING || 
			r2dRequest.getStatus() == RequestStatus.PARTIALLY_COMPLETED) {
			r2dRequest.setStatus(RequestStatus.FAILED);
			message = "Request aborted by the citizen";
			r2dRequest.setFailureMessage(message);
			
			requestRepository.save(r2dRequest);
		} else {
			message = String.format("The status %s of the request %s does not allow to aborti t.", 
					r2dRequest.getStatus(), r2dRequest.getId());	
			logger.error(message);
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
		}
		
		return message;
	}
	
}
