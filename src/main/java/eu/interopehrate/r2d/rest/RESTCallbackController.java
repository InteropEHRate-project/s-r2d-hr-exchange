package eu.interopehrate.r2d.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.interopehrate.r2d.business.RequestProcessor;
import eu.interopehrate.r2d.exceptions.R2DException;

@RestController
@RequestMapping("/callbacks")
public class RESTCallbackController {
	private static final Log logger = LogFactory.getLog(RESTCallbackController.class);
	
	@Autowired(required = true)
	private RequestProcessor requestProcessor;

	
	/**
	 * 
	 * @param theRequestId
	 * @param theBody
	 * @param httpRequest
	 * @param httpResponse
	 * @throws IOException
	 */
	@PostMapping(value = "/{theRequestId}/partial-result-produced", consumes = "application/json")
	public void onPartialResultReady(@PathVariable String theRequestId, 
			@RequestBody String theBody,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws IOException {

		logger.info(String.format("Received a notification that request %s produce a partial result.", theRequestId));
		if (theBody != null && theBody.trim().length() > 0 )
			logger.debug(String.format("The request produced a result of size %d: ", theBody.length()));
		
		try {
			requestProcessor.requestProducedPartialResult(theRequestId, theBody);
		} catch (R2DException r2de) {
			if (r2de.getCode() == R2DException.REQUEST_NOT_FOUND) {
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, r2de.getMessage());					
			} else
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, r2de.getMessage());
		}		
		
	}

	
	/**
	 * 
	 * @param theRequestId
	 * @param theBody
	 * @param httpRequest
	 * @param httpResponse
	 * @throws IOException
	 */
	@PostMapping(value = "/{theRequestId}/completed-succesfully", consumes = "application/json")
	public void onRequestCompletedSuccesfully(@PathVariable String theRequestId, 
			@RequestBody(required = false) String theBody, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws IOException {
				
		logger.info(String.format("Received a notification that request %s completed succesfully.", theRequestId));
		if (theBody != null && theBody.trim().length() > 0 )
			logger.debug(String.format("The request produced a result of size %d: ", theBody.length()));
		
		try {
			requestProcessor.requestCompletedSuccesfully(theRequestId, theBody);
		} catch (R2DException r2de) {
			if (r2de.getCode() == R2DException.REQUEST_NOT_FOUND) {
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, r2de.getMessage());					
			} else
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, r2de.getMessage());
		}
	}

	
	/**
	 * 
	 * @param theRequestId
	 * @param httpRequest
	 * @param httpResponse
	 * @throws IOException
	 */
	@PostMapping("/{theRequestId}/completed-unsuccesfully")
	public void onRequestCompletedUnsuccesfully(@PathVariable String theRequestId, 
			@RequestBody(required = false) String theBody,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws IOException {
		
		logger.info(String.format("Received a notification that request %s completed unsuccesfully.", theRequestId));
		
		try {
			String failureMsg = "";
			if (theBody != null && theBody.trim().length() > 0 )
				failureMsg = theBody;
			
			requestProcessor.requestCompletedUnsuccesfully(theRequestId, failureMsg);
		} catch (R2DException r2de) {
			if (r2de.getCode() == R2DException.REQUEST_NOT_FOUND) {
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, r2de.getMessage());					
			} else
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, r2de.getMessage());
		}
	}
	
}
