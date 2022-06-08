package eu.interopehrate.r2d.business;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Base64.Decoder;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.BundleUtil;
import eu.interopehrate.fhir.provenance.BundleProvenanceBuilder;
import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.MemoryLogger;
import eu.interopehrate.r2d.R2DAccessServer;
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
	private static final String MAX_CONCURRENT_REQUEST_PROPERTY =  "r2da.maxConcurrentRunningRequestPerDay";
	private static final String EQUIVALENCE_PERIOD_PROPERTY =  "r2da.equivalencePeriodInDays";

	private final Logger logger = LoggerFactory.getLogger(RequestProcessorImpl.class);
	private int maxConcurrentRequest;
	private int equivalencePeriodInDays;

	@Autowired(required = true)
	private EHRService ehrService;
	
	@Autowired(required = true)
	private ResponseRepository responseRepository;

	@Autowired(required = true)
	private RequestRepository requestRepository;
	
	private BundleProvenanceBuilder provenanceBuilder;
	
	
	public RequestProcessorImpl() {
		try {
		// Retrieves MAX_CONCURRENT_REQUEST from properties file
		maxConcurrentRequest = Integer.parseInt(Configuration.getProperty(MAX_CONCURRENT_REQUEST_PROPERTY));
		// Retrieves CACHE_DURATION_IN_DAYS from properties file
		equivalencePeriodInDays = Integer.parseInt(Configuration.getProperty(EQUIVALENCE_PERIOD_PROPERTY));
		} catch (Exception e) {
			logger.error("Please check these properties in the configuration file {} {}", 
					MAX_CONCURRENT_REQUEST_PROPERTY, EQUIVALENCE_PERIOD_PROPERTY);
			throw e;
		}
		
    	// Creates the instance of BundleProvenanceBuilder to add 
		// Provenance info to bundles produced by EHR-MW
		Organization providerOrg = new Organization();
		providerOrg.setId("1");
        Identifier id = new Identifier();
        id.setValue(Configuration.getProperty("provenance.provider.identifier"));
        providerOrg.addIdentifier(id);
        providerOrg.setActive(true);
        providerOrg.setName(Configuration.getProperty("provenance.provider.name"));
        providerOrg.addAddress().addLine(Configuration.getProperty("provenance.provider.address"))
        .setCity(Configuration.getProperty("provenance.provider.city"))
        .setState(Configuration.getProperty("provenance.provider.state"))
        .setPostalCode(Configuration.getProperty("provenance.provider.postalcode"))
        .setUse(Address.AddressUse.WORK);
    	provenanceBuilder = new BundleProvenanceBuilder(providerOrg);
	}


	@Override
	public synchronized R2DRequest newIncomingRequest(final String r2dUrl, 
			final String eidasCitizenId, final String preferredLanguages) throws R2DException {
		String r2dQuery = URLUtility.extractR2DSubQuery(r2dUrl);
		
		// Before accepting the request some checks must be executed
		
		// #1 Checks how many running requests the citizen has
		if (maxConcurrentRequest > 0) {
			LocalDateTime now = LocalDateTime.now();
			Date to = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
			
			LocalDateTime _24HoursFromNow = now.minusHours(24);
			Date from = Date.from(_24HoursFromNow.atZone(ZoneId.systemDefault()).toInstant());
			
			long size = requestRepository.countRunningRequestOfTheCitizenInPeriod(eidasCitizenId,
					from, to);
			
			if (size == maxConcurrentRequest)
				throw new TooManyRequestException(
						String.format("Too many concurrent running request: %d. Please try later.", size));			
		}
		
		R2DRequest newR2DRequest = new R2DRequest(r2dQuery, eidasCitizenId);
		newR2DRequest.setPreferredLanguages(preferredLanguages);
		newR2DRequest = requestRepository.save(newR2DRequest);
		logger.info(String.format("Created persistent id: %s to request: %s", newR2DRequest.getId(), newR2DRequest.getUri()));		
		
		return newR2DRequest;
	}

	
	@Override
	public R2DRequest startRequestProcessing(final R2DRequest r2dRequest, 
			final String eidasPersonIdentifier, final String authToken) throws R2DException {
		if (r2dRequest.getStatus() != RequestStatus.NEW)
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to start it.", 
							r2dRequest.getStatus(), r2dRequest.getId()));

		try {
			R2DRequest equivalentRequest = null;
			// #1 checks if there is a cached response
			if (equivalencePeriodInDays > 0) {
				if (logger.isDebugEnabled())
					logger.debug("Looks for a valid cached response...");
				
				LocalDateTime toLtd = LocalDateTime.now();
				LocalDateTime fromLdt  = toLtd.minusDays(equivalencePeriodInDays);
				Date to = Date.from(toLtd.atZone(ZoneId.systemDefault()).toInstant());
				Date from = Date.from(fromLdt.atZone(ZoneId.systemDefault()).toInstant());

				List<R2DRequest> requests = requestRepository.findEquivalentValidRequest(
						eidasPersonIdentifier, r2dRequest.getUri(), 
						from, to);
				// retrieves the most recent equivalent request if one
				if (requests.size() > 0)
					equivalentRequest = requests.get(0);
			} 
			
			if (equivalentRequest != null) {
				// #2.1 if there is an equivalent request
				if (logger.isDebugEnabled())
					logger.debug("Found a valid cached response from request: {}", equivalentRequest.getId());
				r2dRequest.addResponseId(equivalentRequest.getFirstResponseId());
				r2dRequest.setStatus(RequestStatus.COMPLETED);
			} else {
				// #2.2 if there is no cached response sends the request to the EHR
				if (logger.isDebugEnabled())
					logger.debug("No cached response, sending request to EHR...");
				ehrService.sendRequest(r2dRequest, authToken);
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
	public void requestCompletedSuccesfully(final String requestId) throws R2DException {
		// checks request if present
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));

		// checks request status
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() != RequestStatus.RUNNING &&
			theR2DRequest.getStatus() != RequestStatus.PARTIALLY_COMPLETED) {
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to elaborate it.", 
							theR2DRequest.getStatus(), requestId));
		}
		
		Bundle theBundle = null;
		String phase = "parsing received JSON";
		try {
			// #2.1 Parse results produced by IHS
			final IParser parser = R2DAccessServer.FHIR_CONTEXT.newJsonParser();
			// creates the name of the file that store the results
			final String ihsFhirFileName = Configuration.getDBPath() + requestId +  ".json";
			try (InputStream ihsFhirFile = new FileInputStream(new File(ihsFhirFileName))) {
				theBundle = (Bundle) parser.parseResource(ihsFhirFile);
				if (logger.isDebugEnabled())
					logger.debug("Response contains a valid FHIR Bundle with {} entries", 
							theBundle.getEntry().size());
			} catch (DataFormatException dfe) {
				logger.error("Response contains a not valid FHIR Bundle: {}", dfe.getMessage());
				throw dfe;
			}
			
			// #2.2 Adds the images that have been removed by the EHRMW
			phase = "adding removed images";
			replaceImagesInBundle(theBundle, requestId);
			if (logger.isDebugEnabled())
				logger.debug("Added removed images to the bundle");
			
			// #2.3 Adds the Provenance information to the resources in the bundle
			phase = "creating provenance info";
			provenanceBuilder.addProvenanceToBundleItems(theBundle);
			if (logger.isDebugEnabled())
				logger.debug("Added Provenance info to the Bundle, now contains {} entries", theBundle.getEntry().size());

			// #2.4 Writes signed Bundle to file
			try(Writer writer = new BufferedWriter(new FileWriter(ihsFhirFileName, false))) {
				parser.setPrettyPrint(true);
				parser.encodeResourceToWriter(theBundle, writer);				
			}
			
			// #2.5 Store response to the DB
			phase = "saving response to DB";
			R2DResponse response = new R2DResponse();
			response.setResponseFileName(ihsFhirFileName);
			response.setCitizenId(theR2DRequest.getCitizenId());
			responseRepository.save(response);
			
			// #2.6 Update status of the request to the DB
			theR2DRequest.addResponseId(response.getId());
			theR2DRequest.setStatus(RequestStatus.COMPLETED);
			requestRepository.save(theR2DRequest);			
			logger.info(String.format("Response of request %s succesfully saved to database. Execution completed!", requestId));
			theBundle = null;
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg.length() > 250)
				msg = msg.substring(0, 250);
			logger.error(String.format("Error while %s: %s ", phase, msg));
			logger.error(e.getMessage(), e);
			// update request status
			theR2DRequest.setStatus(RequestStatus.FAILED);
			theR2DRequest.setFailureMessage(msg);
			requestRepository.save(theR2DRequest);
		}
		MemoryLogger.logMemory();
	}
	

	@Override
	public void requestCompletedUnsuccesfully(final String requestId, final String failureMsg) throws R2DException {
		// checks request if present
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));
		

		// checks request status		
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() != RequestStatus.RUNNING &&
				theR2DRequest.getStatus() != RequestStatus.PARTIALLY_COMPLETED) {
				throw new R2DException(
						R2DException.INVALID_STATE, 
						String.format("Current status (%s) of request with id % does not allow to elaborate it.", 
								theR2DRequest.getStatus(), requestId));
		}		
		
		// update request status
		theR2DRequest.setStatus(RequestStatus.FAILED);
		theR2DRequest.setFailureMessage(failureMsg);
		requestRepository.save(theR2DRequest);			
		logger.info(String.format("Request %s unsuccesfully completed the execution!", requestId));
	}


	public int getMaxConcurrentRequest() {
		return maxConcurrentRequest;
	}


	public void setMaxConcurrentRequest(int maxConcurrentRequest) {
		this.maxConcurrentRequest = maxConcurrentRequest;
	}


	@Override
	public int getEquivalencePeriodInDays() {
		return equivalencePeriodInDays;
	}


	@Override
	public void setEquivalencePeriodInDays(int equivalencePeriodInDays) {
		this.equivalencePeriodInDays = equivalencePeriodInDays;
	}
	
	
	private void replaceImagesInBundle(Bundle theBundle, String requestId) throws Exception {
		List<Media> media = BundleUtil.toListOfResourcesOfType(R2DAccessServer.FHIR_CONTEXT, 
				theBundle, Media.class);

		String filePrefix = Configuration.getDBPath() + requestId + "_"; 
		String imgPlaceholder;
		int counter = 1;
		for(Media m : media) {
			//imgPlaceholder = new String(m.getContent().getData());
			replaceImage(m, new File( filePrefix + "imagePlaceholder" + counter++));
		}		
	}
	
	
	private void replaceImage(Media media, File imageFile) throws Exception {
		StringWriter imageData = new StringWriter();
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(imageFile))) {
			IOUtils.copyLarge(isr, imageData, new char[10000]);
		}
		
		Base64BinaryType b64 = new Base64BinaryType();
		b64.setValueAsString(imageData.toString());
		media.getContent().setDataElement(b64);
		imageData.close();
	}
	
	
	/*
	@Override
	public void requestProducedPartialResult(String requestId, String jsonBundle) throws R2DException {
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));
		
		// checks request status
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() != RequestStatus.RUNNING &&
			theR2DRequest.getStatus() != RequestStatus.PARTIALLY_COMPLETED) {
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to elaborate it.", 
							theR2DRequest.getStatus(), requestId));
		}
			
		// #2.1 Parse results to verifies the bundle
		Bundle theBundle = null;
		try {
			IParser parser = R2DAccessServer.FHIR_CONTEXT.newJsonParser();
			theBundle = parser.parseResource(Bundle.class, jsonBundle);
			if (logger.isDebugEnabled())
				logger.debug(String.format("Response contains a valid Bundle with %d entries: ", theBundle.getEntry().size()));
			// TODO: adds the signature of the resources
			
			// #2.2 Store response to the DB
			R2DResponse response = new R2DResponse();
			response.setResponse(jsonBundle);
			response.setCitizenId(theR2DRequest.getCitizenId());
			responseRepository.save(response);

			// #2.3 Update status of the request to the DB
			theR2DRequest.setStatus(RequestStatus.PARTIALLY_COMPLETED);
			theR2DRequest.addResponseId(response.getId());
			requestRepository.save(theR2DRequest);			
			if (logger.isDebugEnabled())
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
	*/

}
