package eu.interopehrate.r2d.rest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.business.RequestProcessor;
import eu.interopehrate.r2d.dao.RequestRepository;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.model.RequestStatus;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: REST methods for administrator to checj the status of the 
 * R2D Server.
 */

@RestController
@RequestMapping("/admin")
public class RestAdminController {
	private static final Logger logger = LoggerFactory.getLogger(RestAdminController.class);
	
	@Autowired(required = true)
	private RequestProcessor requestProcessor;

	@Autowired(required = true)
	private RequestRepository requestRepository;

	
	@GetMapping(path="/config", produces = "application/json")
	public String config() {
		
		return String.format("R2DAccess Service version: '%s'\n"
				+ "Deployed by: %s - %s\nConfiguration:\n"
				+ "\tEHRMW endpoint: '%s'\n"
				+ "\tStorage Path: '%s'\n"
				+ "\tMaxRunningRequest: %s\n"
				+ "\tConfigured EquivalencePeriodInDays: %s\n"
				+ "\tCurrent EquivalencePeriodInDays: %d\n"
				+ "\tExpirationTimeInDays: %s\n",
				Configuration.getProperty("r2da.version"),
				Configuration.getProperty("provenance.provider.name"),
				Configuration.getProperty("ehr.endpoint"),
				Configuration.getProperty("r2da.storage.path"),
				Configuration.getProperty("r2da.maxConcurrentRunningRequestPerDay"),
				Configuration.getProperty("r2da.equivalencePeriodInDays"),
				requestProcessor.getEquivalencePeriodInDays(),
				Configuration.getProperty("r2da.expirationTimeInDays"));
				
	}

	
	@PostMapping(path="/equivalenceperiod")
	public String updateEquivalencePeriod(@RequestParam(name = "period", defaultValue = "-1") int periodInDays) {
		requestProcessor.setEquivalencePeriodInDays(periodInDays);
		
		return String.format("EquivalencePeriod updated to: %d days.", 
				requestProcessor.getEquivalencePeriodInDays());
	}
	
	
	// List requests of a citizen
	@GetMapping(path="/requests", produces = "application/json")
	public List<R2DRequest> getCitizenRequests(
			@RequestParam(name="citizenid", required=true) String citizenId,
			@RequestParam(name="status", required=false) String status) {
		
		if (status == null || status.isEmpty())
			return requestRepository.findByCitizenId(citizenId);
		else
			return requestRepository.findByCitizenIdAndByStatus(citizenId, status.toUpperCase());
	}
	
	
	@PostMapping(path="/abort")
	public String abortRequest(@RequestParam(name = "requestid", required=true) String requestId, 
			HttpServletResponse httpResponse) throws IOException {
		Optional<R2DRequest> opt = requestRepository.findById(requestId);
		String message = "";
		if (!opt.isPresent()) {
			message = String.format("Request with id %s not found.", requestId);
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, message);
			return message;
		}
		
		// Return the results if the status is COMPLETED
		R2DRequest r2dRequest = opt.get();
		if (r2dRequest.getStatus() == RequestStatus.RUNNING || 
			r2dRequest.getStatus() == RequestStatus.PARTIALLY_COMPLETED) {
			r2dRequest.setStatus(RequestStatus.FAILED);
			message = "Request aborted by the administrator";
			r2dRequest.setFailureMessage(message);
			
			requestRepository.save(r2dRequest);
		} else {
			message = String.format("The status %s of the request %s does not allow to abort it.", 
					r2dRequest.getStatus(), r2dRequest.getId());	
			logger.error(message);
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
		}
		
		return message;
	}
	
}
