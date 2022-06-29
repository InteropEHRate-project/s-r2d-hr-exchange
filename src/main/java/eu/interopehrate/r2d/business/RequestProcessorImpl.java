package eu.interopehrate.r2d.business;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportMediaComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Media.MediaStatus;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
	
	private static final String PROVIDER_ID =  "provenance.provider.identifier";
	private static final String PROVIDER_NAME =  "provenance.provider.name";
	private static final String PROVIDER_ADDRESS =  "provenance.provider.address";
	private static final String PROVIDER_CITY =  "provenance.provider.city";
	private static final String PROVIDER_STATE =  "provenance.provider.state";
	private static final String PROVIDER_POSTAL_CODE =  "provenance.provider.postalcode";

	private final Logger logger = LoggerFactory.getLogger(RequestProcessorImpl.class);
	private int maxConcurrentRequest;
	private int equivalencePeriodInDays;

	@Autowired(required = true)
	private EHRService ehrService;
	
	@Autowired(required = true)
	private ResponseRepository responseRepository;

	@Autowired(required = true)
	private RequestRepository requestRepository;
	
	private String storagePath;
	private Organization defaultProviderOrg;
	
	public RequestProcessorImpl() {
		// Retrieves the storage path from the configuration file
		storagePath = Configuration.getDBPath();
		if (!storagePath.endsWith("/"))
			storagePath += "/";

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
		defaultProviderOrg = new Organization();
		String id = Configuration.getProperty(PROVIDER_ID);
		defaultProviderOrg.setId(id);
        Identifier idf = new Identifier();
        idf.setValue(id);
        defaultProviderOrg.addIdentifier(idf);
        defaultProviderOrg.setActive(true);
        defaultProviderOrg.setName(Configuration.getProperty(PROVIDER_NAME));
        defaultProviderOrg.addAddress().addLine(Configuration.getProperty(PROVIDER_ADDRESS))
        .setCity(Configuration.getProperty(PROVIDER_CITY))
        .setState(Configuration.getProperty(PROVIDER_STATE))
        .setPostalCode(Configuration.getProperty(PROVIDER_POSTAL_CODE))
        .setUse(Address.AddressUse.WORK);
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
		// #1 checks if request exists
		Optional<R2DRequest> optional = requestRepository.findById(requestId);
		if (!optional.isPresent())
			throw new R2DException(
					R2DException.REQUEST_NOT_FOUND, String.format("Request with id % not found.", requestId));

		// #1.1 checks request status
		R2DRequest theR2DRequest = optional.get();
		if (theR2DRequest.getStatus() != RequestStatus.RUNNING &&
			theR2DRequest.getStatus() != RequestStatus.PARTIALLY_COMPLETED) {
			throw new R2DException(
					R2DException.INVALID_STATE, 
					String.format("Current status (%s) of request with id % does not allow to elaborate it.", 
							theR2DRequest.getStatus(), requestId));
		}
		
		// #2 starts request processing
		Bundle theBundle = null;
		String phase = "parsing received JSON";
		try {
			final IParser parser = R2DAccessServer.FHIR_CONTEXT.newJsonParser();
			// #2.0 creates the name of the file that store the results
			final String ihsFhirFileName =  storagePath + requestId +  ".json";
			
			// #2.1 Parse results produced by IHS
			if (logger.isDebugEnabled())
				logger.debug("Parsing received JSON...");
			theBundle = parseReceivedBundle(new File(ihsFhirFileName), parser);		
			
			// #2.2 Adds the images that have been removed by the EHRMW
			phase = "adding removed images";
			if (logger.isDebugEnabled())
				logger.debug("Restoring removed images into the bundle...");
			restoreImagesInBundle(theBundle, requestId, storagePath);
			
			// #2.3 Adds the Provenance information to the resources in the bundle
			phase = "creating provenance info";
			Organization org = getOrganizationFromBundle(theBundle);
			final BundleProvenanceBuilder provenanceBuilder = new BundleProvenanceBuilder(org);
			if (logger.isDebugEnabled())
				logger.debug("Adding Provenance info to the Bundle...");
			provenanceBuilder.addProvenanceToBundleItems(theBundle);
			if (logger.isDebugEnabled())
				logger.debug("Added Provenance info to the Bundle, now contains {} entries", theBundle.getEntry().size());

			// #2.4 Writes signed Bundle to file
			if (logger.isDebugEnabled())
				logger.debug("Saving complete bundle to file...");
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
			logger.error("Error during execution of: " + phase, e);
			String msg = e.getMessage();
			if (msg.length() > 250)
				msg = msg.substring(0, 250);
			// logger.error(String.format("Error while %s: %s ", phase, msg));
			logger.error(String.format("Error while %s: %s ", phase, e.getClass().getName()));
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
		
		// clear tmp files
		if (logger.isDebugEnabled())
			logger.debug("Deleting temporary files for image anonymization....");

		Path filePath = Paths.get(Configuration.getDBPath());
		final String fileNamePattern = requestId + "_imagePlaceholder";
		try {
			Files.walk(filePath)
			.filter(Files::isRegularFile)
			.filter(tmpFile -> tmpFile.getName(tmpFile.getNameCount() - 1).toString().startsWith(fileNamePattern))
			.forEach(tmpFile -> {
				try {
					Files.deleteIfExists(tmpFile);
				} catch (Exception ioe) {
					logger.warn("Not able to delete tmp file {}", tmpFile.toString());
				}
			});
		} catch (IOException e) {
			logger.warn("Not able to delete tmp files for request {}", requestId);
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
	
	
	public Organization getOrganizationFromBundle(Bundle theBundle) {
		List<Organization> orgs = BundleUtil.toListOfResourcesOfType(R2DAccessServer.FHIR_CONTEXT, 
				theBundle, Organization.class);
		
		String orgId = Configuration.getProperty(PROVIDER_ID);
		
		// looks for the provider organization into the received bundle...
		for (Organization org : orgs) {
			if (org.getIdElement().getIdPart().equals(orgId)) {
				org.setActive(true);
				org.setName(Configuration.getProperty(PROVIDER_NAME));
				org.addAddress().addLine(Configuration.getProperty(PROVIDER_ADDRESS))
		        .setCity(Configuration.getProperty(PROVIDER_CITY))
		        .setState(Configuration.getProperty(PROVIDER_STATE))
		        .setPostalCode(Configuration.getProperty(PROVIDER_POSTAL_CODE))
		        .setUse(Address.AddressUse.WORK);
				return org;
			}
		}
		
		// if not found, returns default provider org
		return this.defaultProviderOrg;
	}
	
	
	private Bundle parseReceivedBundle(File inputFHIRFile, IParser parser) throws Exception {
		try (InputStream ihsFhirFile = new FileInputStream(inputFHIRFile)) {
			Bundle theBundle = (Bundle) parser.parseResource(ihsFhirFile);
			if (logger.isDebugEnabled())
				logger.debug("Response contains a valid FHIR Bundle with {} entries", 
						theBundle.getEntry().size());
			
			return theBundle;
		} catch (Exception | Error e) {
			logger.error("Unable to parse the received FHIR bundle: {}", e.getMessage());
			throw e;
		}		
	}
	
	
	private void restoreImagesInBundle(Bundle theBundle, String requestId, String storagePath) throws Exception {
		List<DiagnosticReport> allReports = BundleUtil.toListOfResourcesOfType(R2DAccessServer.FHIR_CONTEXT, 
				theBundle, DiagnosticReport.class);

		List<Media> allMedia = BundleUtil.toListOfResourcesOfType(R2DAccessServer.FHIR_CONTEXT, 
				theBundle, Media.class);

		if (logger.isDebugEnabled())
			logger.debug("Found {} Media in bundle to be restored...", allMedia.size());

		String filePrefix = storagePath + requestId; 
		String imgPlaceholder;
		DiagnosticReport parent;
		List<File> filesToDelete = new ArrayList<File>();
		// starts looping
		for(Media media : allMedia) {
			if (logger.isDebugEnabled())
				logger.debug("Processing Media {} ", media.getId());
			
			if (media.getContent() == null) {
				logger.warn("Unable to restore image from Media {}, attachment is null.", 
						media.getIdElement().getIdPart());
				continue;
			}
			
			if (media.getContent().getData() == null) {
				logger.warn("Unable to restore image from Media {}, data of attachment is null.",
						media.getIdElement().getIdPart());
				continue;
			}
			
			// retrieve placeholde from current Media content
			// the placeholder is used to determine what image 
			// must be added to the Media
			imgPlaceholder = new String(media.getContent().getData());
			if (logger.isDebugEnabled())
				logger.debug("Found placeholder: {} in Media {}", imgPlaceholder, media.getId());
			
			// image file = [requestId]_imagePlaceholder[id]
			File clearImage = new File(filePrefix + imgPlaceholder);
			filesToDelete.add(clearImage);
			// Copy original image to Media
			if (logger.isDebugEnabled())
				logger.debug("Restoring: original version of {}...", media.getId());
			copyImageFromFileToMedia(media, clearImage, null);

			// anon image file = [requestId]_imagePlaceholder[id]_anon
			File anonymizedImage = new File(filePrefix + imgPlaceholder + "_anon");
			if (anonymizedImage.exists()) {
				filesToDelete.add(anonymizedImage);
				parent = getParentReport(allReports, media);
				if (parent == null) {
					logger.warn(
					"No containing DiagnosticReport found for {}, anonymized image not added.",
					media.getId());
					continue;
				}
				// Creates anonymized Media and adds it to the bundle
				logger.debug("Adding anonymized version of {}...", media.getId());
				addAnonymizedImageToBundle(theBundle, media, anonymizedImage, parent);
				// only for load test
				// addAnonymizedImageToBundleForLoadTest(theBundle, media, anonymizedImage, parent);
			} else
				logger.warn("No anonymized version of {}", media.getId());
		}
		
		// deletes all no more needed images
		logger.debug("Deleting {} temporary image files...", filesToDelete.size());
		for (File filetoDelete : filesToDelete) {
			Files.deleteIfExists(filetoDelete.toPath());
		}
			
	}
	
	
	private void addAnonymizedImageToBundle(Bundle theBundle, Media clearMedia, 
			File anonymizedImage, DiagnosticReport parent) throws Exception {
		// creates the anonymized Media instance
		Media anonymizedMedia = new Media();
        Meta meta = new Meta();
        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Media-IEHR");
        anonymizedMedia.setMeta(meta);
		anonymizedMedia.setId(clearMedia.getIdElement().getIdPart() + "_anonymized");
		anonymizedMedia.setStatus(MediaStatus.COMPLETED);
		anonymizedMedia.setSubject(clearMedia.getSubject());
		anonymizedMedia.setOperator(clearMedia.getOperator());
		anonymizedMedia.setEncounter(clearMedia.getEncounter());
		// adds extension for anonymization
		Extension ext = new Extension(
				"http://interopehrate.eu/fhir/StructureDefinition/AnonymizationExtension-IEHR");
		ext.setValue(new Coding("http://interopehrate.eu/fhir/CodeSystem/AnonymizationType-IEHR",
				"anonymization", "Anonymization"));
		anonymizedMedia.addExtension(ext);
		
		// copy data from the anonymized file to the Media
		copyImageFromFileToMedia(anonymizedMedia, anonymizedImage, 
				clearMedia.getContent().getContentType());
		//adds the anonymized Media to the Diagnostic Report
		parent.addMedia().setLink(new Reference(anonymizedMedia));
		// adds the Media to the Bundle
		theBundle.addEntry().setResource(anonymizedMedia);
	}
	
	private void addAnonymizedImageToBundleForLoadTest(Bundle theBundle, Media clearMedia, 
			File anonymizedImage, DiagnosticReport parent) throws Exception {
		
		int maxLoad = Integer.parseInt(Configuration.getProperty("r2d.DuplicatedImagesSize"));
		
		Media duplicatededMedia = new Media();
        Meta meta = new Meta();
		for (int i = 0; i < maxLoad ; i++) {
			// creates the anonymized Media instance
	        meta.addProfile("http://interopehrate.eu/fhir/StructureDefinition/Media-IEHR");
	        duplicatededMedia.setMeta(meta);
			duplicatededMedia.setId(clearMedia.getIdElement().getIdPart() + "_anonymized_" + (i + 2));
			duplicatededMedia.setStatus(MediaStatus.COMPLETED);
			duplicatededMedia.setSubject(clearMedia.getSubject());
			duplicatededMedia.setOperator(clearMedia.getOperator());
			duplicatededMedia.setEncounter(clearMedia.getEncounter());
			// adds extension for anonymization
			Extension ext = new Extension(
					"http://interopehrate.eu/fhir/StructureDefinition/AnonymizationExtension-IEHR");
			ext.setValue(new Coding("http://interopehrate.eu/fhir/CodeSystem/AnonymizationType-IEHR",
					"anonymization", "Anonymization"));
			duplicatededMedia.addExtension(ext);
			
			// copy data from the anonymized file to the Media
			copyImageFromFileToMedia(duplicatededMedia, anonymizedImage, 
					clearMedia.getContent().getContentType());
			//adds the anonymized Media to the Diagnostic Report
			parent.addMedia().setLink(new Reference(duplicatededMedia));
			// adds the Media to the Bundle
			logger.debug("...Duplicating image {}", duplicatededMedia.getIdElement().getIdPart());
			theBundle.addEntry().setResource(duplicatededMedia);
		}
	}
	
	private void copyImageFromFileToMedia(Media media, File imageFile, String contentType) throws Exception {
		ByteArrayOutputStream imageData = new ByteArrayOutputStream();
		try (InputStream is = new FileInputStream(imageFile)) {
			IOUtils.copyLarge(is, imageData, new byte[1024]);
		}
		if (contentType != null && !contentType.isEmpty())
			media.getContent().setContentType(contentType);
		media.getContent().setData(imageData.toByteArray());
		media.getContent().setSize(imageData.size());
		imageData.close();
	}
	
	
	private DiagnosticReport getParentReport(List<DiagnosticReport> reports, Media media) {
		final String mediaId = media.getId();
		String currentMediaId;
		
		for (DiagnosticReport report : reports) {
			for (DiagnosticReportMediaComponent mc : report.getMedia()) {
				currentMediaId = ((Media)mc.getLink().getResource()).getId();
				if (mediaId.equals(currentMediaId)) 
					return report;
			}
		}
		
		return null;
	}
	

}
