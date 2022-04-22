package eu.interopehrate.r2d.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.R2DContextProvider;
import eu.interopehrate.r2d.business.RequestProcessor;
import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.Citizen;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.security.SecurityConstants;

@Interceptor
public class AsyncRequestHandler {

	private static final Logger logger = LoggerFactory.getLogger(AsyncRequestHandler.class);
	private static final String[] requestToBeSkipped = {"/metadata"};

	private RequestProcessor requestProcessor;
	
	public AsyncRequestHandler() {
		super();
		requestProcessor = R2DContextProvider.getApplicationContext().getBean(RequestProcessor.class);
	}

	
	@Hook(value = Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	/**
	 * This method is called if the client has requested an R2D Access operation. 
	 * Only R2D Access requests are processed, any valid FHIR request that is not 
	 * part of R2D Access operations will not be processed by this method, because 
	 * it is blocked before by other Interceptors.
	 * 
	 * @param theRequestDetails
	 * @param theRequest
	 */
	public void startAsyncRequestProcessing (RequestDetails theRequestDetails, HttpServletRequest theRequest) throws R2DException {
		// Creates the request unique id
		if (hasToBeSkipped(theRequest.getRequestURL().toString())) {
			if (logger.isDebugEnabled())
				logger.debug(String.format("Received request %s that will be handled synchronously.", theRequestDetails.getCompleteUrl()));
			return;
		}
		
		if (logger.isDebugEnabled())
			logger.debug(String.format("Received R2DA request: %s", theRequestDetails.getCompleteUrl()));
		// Retrieves the Citizen and the lang from the request
		Citizen citizen = (Citizen)theRequest.getAttribute(SecurityConstants.CITIZEN_ATTR_NAME);
		
		// Creates the new R2DRequest
		R2DRequest r2dRequest = requestProcessor.newIncomingRequest(
				theRequestDetails.getCompleteUrl(), 
				citizen.getPersonIdentifier(), 
				theRequest.getHeader("Accept-Language"));

		// store the R2DRequest id in the HttpRequest 
		theRequest.setAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME, r2dRequest);
	}
	
	
	/** 
	 * This method is called if the Resource Provider throws an exception 
	 * during the execution of a R2D operation, or if there is a 
	 * missing (mandatory) parameter in the request.
	 * 
	 * @param outcome
	 */
	@Hook(value = Pointcut.SERVER_OUTGOING_FAILURE_OPERATIONOUTCOME)
	public void handleUnableToStartAsyncRequestProcessing(IBaseOperationOutcome outcome, RequestDetails theRequestDetails)  {
		R2DRequest r2dRequest = (R2DRequest) theRequestDetails.getAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME);
		if (r2dRequest != null) {
			OperationOutcome o = (OperationOutcome)outcome;
			o.getIssueFirstRep().setDiagnostics("The request could not be processed due to an internal error.");	 
		}
	}
	
	
	/**
	 * This method is called at the end of the execution of an R2D operation 
	 * (performed by a Resource Provider)
	 *  
	 * @param theResponseDetails
	 * @param theResponse
	 */
	@Hook(value = Pointcut.SERVER_OUTGOING_RESPONSE)
	public void handleStartedAsyncRequestProcessing(ResponseDetails theResponseDetails, 
			HttpServletRequest theRequest, HttpServletResponse theResponse)  {
		
		if (hasToBeSkipped(theRequest.getRequestURL().toString()))
			return;
		
		R2DRequest r2dRequest = (R2DRequest) theRequest.getAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME);
		//r2dRequest = requestProcessor.requestStartedExecution(r2dRequest.getId());
		// store the updated R2DRequest id in the HttpRequest 
		// theRequest.setAttribute(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME, r2dRequest);
		// Adds the 202 return code
		theResponseDetails.setResponseCode(202);
		
		StringBuilder url = new StringBuilder();
		url.append(Configuration.getR2DServicesContextPath());
		url.append("/requests/").append(r2dRequest.getId()).append("/status");
		
		OperationOutcome o = new OperationOutcome();
		o.addIssue()
		.setSeverity(IssueSeverity.INFORMATION)
		.setCode(IssueType.INFORMATIONAL)
		.setDiagnostics(String.format("Your request is under processing, use this URL to monitor the status: %s", url.toString()));
		theResponseDetails.setResponseResource(o);
		
		// Adds the header with the url for polling
		theResponse.addHeader("Content-Location", url.toString());

	}
	
	/**
	 * Checks if the request has to be handled synchronously
	 * 
	 * @param url
	 * @return
	 */
	private boolean hasToBeSkipped(String url) {
		for (String context : requestToBeSkipped) {
			if (url.endsWith(context))
				return true;
		}
		
		return false;
	}

}
