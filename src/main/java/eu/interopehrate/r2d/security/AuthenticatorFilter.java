package eu.interopehrate.r2d.security;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import eu.interopehrate.sr2dsm.SR2DSM;
import eu.interopehrate.sr2dsm.model.ResponseDetails;

public class AuthenticatorFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatorFilter.class);
	
	private static final String EHR_SERVICE_CREDENTIALS_INIT_PARAM = "EHR_SERVICE_CREDENTIALS";
	private static final String EHR_SERVICE_URLS_INIT_PARAM = "EHR_SERVICE_URLS";
	private static final String CITIZEN_URLS_INIT_PARAM = "CITIZEN_URLS";
	
	private String ehrServiceCredentials;
	@SuppressWarnings("unused")
	private String[] ehrServiceURLs;
	@SuppressWarnings("unused")
	private String[] citizenURLs;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Optional<String> o = Optional.fromNullable(filterConfig.getInitParameter(EHR_SERVICE_CREDENTIALS_INIT_PARAM));
		if (!o.isPresent())
			throw new ServletException("Missing " + EHR_SERVICE_CREDENTIALS_INIT_PARAM + " config parameter, cannot start!");
		ehrServiceCredentials = o.get();
		
		o = Optional.fromNullable(filterConfig.getInitParameter(EHR_SERVICE_URLS_INIT_PARAM));
		if (!o.isPresent())
			throw new ServletException("Missing " + EHR_SERVICE_URLS_INIT_PARAM + " config parameter, cannot start!");
		ehrServiceURLs = o.get().split(",");
		
		o = Optional.fromNullable(filterConfig.getInitParameter(CITIZEN_URLS_INIT_PARAM));
		if (!o.isPresent())
			throw new ServletException("Missing " + CITIZEN_URLS_INIT_PARAM + " config parameter, cannot start!");
		citizenURLs = o.get().split(",");		
	}

	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest hReq = (HttpServletRequest)request;
		HttpServletResponse hRes = (HttpServletResponse)response;
		LOGGER.debug("Authenticating request: " + hReq.getRequestURL());
		
		// Get the Authorization header parameter
		Optional<String> o = Optional.fromNullable(hReq.getHeader(SecurityConstants.AUTH_HEADER));
		if (!o.isPresent()) {
			LOGGER.error("The request lacks the Authorization header parameter, thus cannot be processed.");
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "The request lacks the Authorization header parameter, thus cannot be processed.");
			return;			
		}
	
		String authHeaderParam = o.get();
		
		if (authHeaderParam.startsWith(SecurityConstants.BASIC_PREFIX)) {
			// If basic authorization, only the EHR_SERVICE is authorized
			LOGGER.debug("Verifying Basic Auth...");
			String base64Credentials = authHeaderParam.substring(SecurityConstants.BASIC_PREFIX.length()).trim();
			final byte[] decodedBytes  = Base64.getDecoder().decode(base64Credentials.getBytes());
		    final String credentials = new String(decodedBytes);
		    if (!credentials.equals(ehrServiceCredentials)) {
				LOGGER.error("The provided credentials are not valid! The request cannot be processed.");
				hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "The provided credentials are not valid! The request cannot be processed.");			
				return;
		    }
		    
		    // check paths
			LOGGER.info("Request authenticated to EHR Service.");
		
		} else if (authHeaderParam.startsWith(SecurityConstants.OAUTH_PREFIX)) {
			// If oAuth authorization, only citizen is allowed
			try {
				LOGGER.debug("Verifying EIDAS token...");
				String oAuthToken = authHeaderParam.substring(SecurityConstants.OAUTH_PREFIX.length()).trim();
				ResponseDetails tokenDetails = SR2DSM.decode(oAuthToken);
				request.setAttribute(SecurityConstants.EIDAS_PERSON_ID_ATTR_NAME, tokenDetails.getUserDetails().getPersonIdentifier());
			    LOGGER.info("Request contains a valid authtentication token for citizen " + tokenDetails.getUserDetails().getPersonIdentifier());
			} catch (Exception e) {
				LOGGER.error("Authentication token is not valid! Request cannot be processed.", e);
				hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "Authentication token is not valid! The request cannot be processed.");	
				return;
			}
			// check paths
			
		} else {
			LOGGER.error("Authorization header is not used properly! The request cannot be processed.");
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "Authorization header is not used properly! The request cannot be processed.");
			return;
		}
		
	    chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() {}

}
