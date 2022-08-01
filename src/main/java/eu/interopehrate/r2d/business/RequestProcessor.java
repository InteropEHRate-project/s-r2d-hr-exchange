package eu.interopehrate.r2d.business;

import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.R2DRequest;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: class that handles all the fundamental steps of the 
 * processing of a request: creation, start, end with success, end with
 * failure. 
 * 
 * Methods of this class are invoked by the FHIR Provider classes, and by
 * the REST services of the R2DA server that receives callback from the 
 * EHR-Middleware.
 */
public interface RequestProcessor {

	/**
	 * 
	 * @return
	 */
	public int getEquivalencePeriodInDays();

	
	/**
	 * 
	 * @param equivalencePeriodInDays
	 */
	public void setEquivalencePeriodInDays(int equivalencePeriodInDays);
	
	
	/**
	 * 
	 * @param r2dUrl: URL of the requested service
	 * @param eidasPersonIdentifier: eidasIdentifier of the requesting citizen
	 * @param preferredLanguages: list of preferred languages of the citizen
	 * 
	 * @return
	 * @throws R2DException
	 */
	public R2DRequest newIncomingRequest(String r2dUrl, 
			String eidasPersonIdentifier,
			String preferredLanguages) throws R2DException;


	/**
	 * 
	 * @param r2dRequest: r2d request
	 * @param eidasPersonIdentifier: eidasIdentifier of the requesting citizen
	 * 
	 * @return
	 * @throws R2DException
	 */
	public R2DRequest startRequestProcessing(R2DRequest r2dRequest, String eidasPersonIdentifier, String authToken) throws R2DException;

		
	/**
	 * 
	 * @param requestId: id of the request
	 * @param jsonBundle: json FHIR bundle containing the health data
	 * 
	 * @return
	 * @throws R2DException
	public void requestProducedPartialResult(String requestId, String jsonBundle) throws R2DException;
	 */

	
	/**
	 * 
	 * @param requestId: id of the request
	 * @param jsonBundle: json FHIR bundle containing the health data
	 * 
	 * @return
	 * @throws R2DException
	 */
	public void requestCompletedSuccesfully(String requestId) throws R2DException;

	
	/**
	 * 
	 * @param requestId
	 * @param failureMsg
	 * 
	 * @return
	 * @throws R2DException
	 */
	public void requestCompletedUnsuccesfully(String requestId,  String failureMsg) throws R2DException;

}
