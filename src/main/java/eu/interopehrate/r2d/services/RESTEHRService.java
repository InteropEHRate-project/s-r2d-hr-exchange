package eu.interopehrate.r2d.services;

import java.io.IOException;
import java.util.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.exceptions.R2DException;
import eu.interopehrate.r2d.model.R2DRequest;
import eu.interopehrate.r2d.security.SecurityConstants;

/**
 * 
 * Sends a request to the EHR using REST 
 * 
 * @author alessiograziani
 *
 */
@Component
public class RESTEHRService implements EHRService {
	
	private static final Log logger = LogFactory.getLog(RESTEHRService.class);
	/**
	 * key for storing the id of the R2D Eidas Token before sending it to EHR
	 */
	private static final String R2D_REQUEST_CITIZEN_PARAM_NAME = "eIDAS-Citizen-Token";

	@Override
	public void sendRequest(R2DRequest r2dRequest, String authToken) throws R2DException {
		// Creates the EHR-MW service URL 
		StringBuilder serviceURL = new StringBuilder(Configuration.getEHRMWContextPath());
		serviceURL.append(r2dRequest.getUri());
		
		// Creates the GET request
		HttpGet httpGet = new HttpGet(serviceURL.toString());

		// #2 Adds header parameters
		// #2.1 adds request id 
		httpGet.addHeader(SecurityConstants.R2D_REQUEST_ID_PARAM_NAME, r2dRequest.getId());
		// #2.2 adds R2D Access Server credentials with Basic Auth
		String credentials = Configuration.getProperty(Configuration.R2DA_CREDENTIALS);
		final byte[] encodedBytes = Base64.getEncoder().encode(credentials.getBytes());
		httpGet.addHeader(SecurityConstants.AUTH_HEADER, SecurityConstants.BASIC_PREFIX + new String(encodedBytes));
		// #2.3 adds eidas token
		httpGet.addHeader(R2D_REQUEST_CITIZEN_PARAM_NAME, authToken);

		// #3 Sends request
		try {
			logger.debug(String.format("Inovked EHR-MW service for request with id %s", r2dRequest.getId()));
			// Creates the HttpClient
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() != 200) {
			    String error = String.format("Received error %s from EHR-MW: %s", response.getStatusLine().getStatusCode(), "");
				logger.error(error);
				throw new R2DException(R2DException.COMMUNICATION_ERROR, error);
			} else
				logger.info(String.format("Request %s forwarded successfully to EHR-MW", r2dRequest.getId()));
		} catch (IOException ioe) {
			logger.error("Error while forwarding request to EHR-MW", ioe);
			throw new R2DException(R2DException.INTERNAL_ERROR, ioe);
		}
	}

}
