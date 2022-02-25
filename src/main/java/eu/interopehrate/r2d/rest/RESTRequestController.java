package eu.interopehrate.r2d.rest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.dao.RequestRepository;
import eu.interopehrate.r2d.dao.ResponseRepository;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.model.R2DResponse;
import eu.interopehrate.r2d.model.RequestOutcome;
import eu.interopehrate.r2d.model.RequestOutput;
import eu.interopehrate.r2d.model.RequestStatus;
import eu.interopehrate.r2d.security.SecurityConstants;

@RestController
@RequestMapping("/requests")
public class RESTRequestController {
	private static final Log logger = LogFactory.getLog(RESTRequestController.class);
	
	@Autowired(required = true)
	private RequestRepository requestRepository;

	@Autowired(required = true)
	private ResponseRepository responseRepository;
	
	
	@GetMapping(produces = "application/json")
	public Collection<R2DRequest> listRequests(HttpServletRequest theRequest,
			HttpServletResponse theResponse) {
		String eidasPersonIdentifier = 
				(String)theRequest.getAttribute(SecurityConstants.EIDAS_PERSON_ID_ATTR_NAME);
		
		return requestRepository.findByCitizenId(eidasPersonIdentifier);
	}
	
	@GetMapping(path = "/test", produces = "application/json")
	public List<R2DRequest> test(HttpServletRequest theRequest, 
			HttpServletResponse theResponse) throws IOException {
		String eidasPersonIdentifier = 
				(String)theRequest.getAttribute(SecurityConstants.EIDAS_PERSON_ID_ATTR_NAME);
		
		
		// #1 Checks if citizen has already submitted 2 requests in the last 24 hours
		
		/*
		reqs = requestRepository.findSameRequestInTheDay(
				"/Encounter?status=finished",
				eidasPersonIdentifier);
		*/
		
		// Checks if in the last 
		LocalDateTime toLtd = LocalDateTime.now();
		LocalDateTime fromLdt  = toLtd.minusDays(5);
		Date to = Date.from(toLtd.atZone(ZoneId.systemDefault()).toInstant());
		Date from = Date.from(fromLdt.atZone(ZoneId.systemDefault()).toInstant());
		
		System.out.println(from);
		System.out.println(to);
		System.out.println(eidasPersonIdentifier);
		List<R2DRequest> requests = requestRepository.findEquivalentValidRequest(
				eidasPersonIdentifier, "/Encounter?status=finished",
				from, to);

		return requests;
	}
	
	@GetMapping(path = "/{theRequestId}", produces = "application/json")
	public R2DRequest getRequestById(@PathVariable String theRequestId, HttpServletRequest theRequest, 
			HttpServletResponse theResponse) throws IOException {
		String eidasPersonIdentifier = 
				(String)theRequest.getAttribute(SecurityConstants.EIDAS_PERSON_ID_ATTR_NAME);
		
		Optional<R2DRequest> opt = requestRepository.findByRequestIdAndCitizenId(theRequestId, eidasPersonIdentifier);
		if (!opt.isPresent()) {
			theResponse.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(
					"Request with id %s not found or not belonging to requesting citizen.", theRequestId));
			return null;
		} else
			return opt.get();
	}

	
	@GetMapping(path = "/{theRequestId}/status", produces = "application/json")
	public RequestOutcome monitorRequestStatus(@PathVariable String theRequestId,
			HttpServletRequest theRequest, HttpServletResponse theResponse) throws IOException {
		
		String eidasPersonIdentifier = 
				(String)theRequest.getAttribute(SecurityConstants.EIDAS_PERSON_ID_ATTR_NAME);
		
		Optional<R2DRequest> opt = requestRepository.findByRequestIdAndCitizenId(theRequestId, eidasPersonIdentifier);
		if (opt.isPresent()) {
			R2DRequest theR2DRequest = opt.get();
			if (theR2DRequest.getStatus() == RequestStatus.RUNNING) {
				theResponse.sendError(HttpServletResponse.SC_ACCEPTED, 
						"Your request is still under processing, please use again this URL to monitor it.");
				return null;
			} else if (theR2DRequest.getStatus() == RequestStatus.COMPLETED) {
				Optional<R2DResponse> optResp = responseRepository.findById(theR2DRequest.getFirstResponseId());
				
				StringBuilder responseURL = new StringBuilder(Configuration.getR2DServicesContextPath());
				responseURL.append("/requests/").append(theR2DRequest.getId())
				.append("/response/").append(optResp.get().getId());
				
				RequestOutcome outcome = new RequestOutcome(theR2DRequest.getUri());
				outcome.addOutput(new RequestOutput("Bundle", responseURL.toString()));	
				
				return outcome;
			} else
				return null;
		} else {
			theResponse.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(
					"Request with id %s not found or not belonging to requesting citizen.", theRequestId));
			return null;
		}
	}
	
	
	@GetMapping(path = "/{theRequestId}/response/{theResponseId}", produces = "application/json")
	@ResponseBody
	public String getRequestResults(@PathVariable String theRequestId,
			@PathVariable String theResponseId,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

		// Checks if requests belongs to requesting citizen
		String eidasPersonIdentifier = 
				(String)httpRequest.getAttribute(SecurityConstants.EIDAS_PERSON_ID_ATTR_NAME);
		Optional<R2DRequest> opt = requestRepository.findByRequestIdAndCitizenId(theRequestId, eidasPersonIdentifier);
		if (!opt.isPresent()) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, String.format(
					"Request with id %s not found or not belonging to requesting citizen.", theRequestId));
			return null;
		}
		
		// Return the results if the status is COMPLETED
		R2DRequest r2dRequest = opt.get();
		if (r2dRequest.getStatus() == RequestStatus.COMPLETED) {
			Optional<R2DResponse> optResponse = responseRepository.findById(r2dRequest.getFirstResponseId());
			
			return optResponse.isPresent() ? optResponse.get().getResponse() : "";
		} else {
			String msg = String.format("The status %s of the request %s does not allow to retrieve the results.", 
					r2dRequest.getStatus(), r2dRequest.getId());	
			logger.error(msg);
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
		}
		
		return null;
	}

}
