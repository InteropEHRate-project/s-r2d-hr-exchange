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

import org.apache.commons.text.StringTokenizer;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.model.Citizen;
import eu.interopehrate.sr2dsm.SR2DSM;
import eu.interopehrate.sr2dsm.model.ResponseDetails;
import eu.interopehrate.sr2dsm.model.UserDetails;

public class AuthenticatorFilter implements Filter {
	private static final String EHR_SERVICE_CREDENTIALS_INIT_PARAM = "EHR_SERVICE_CREDENTIALS";
	private static final String ADMIN_CREDENTIALS_INIT_PARAM = "ADMIN_CREDENTIALS";
	private static final String EHR_SERVICE_URLS_INIT_PARAM = "EHR_SERVICE_ALLOWED_URIS";
	private static final String ADMIN_URLS_INIT_PARAM = "ADMIN_ALLOWED_URIS";
	private static final String CITIZEN_URLS_INIT_PARAM = "CITIZEN_ALLOWED_URIS";
	private static final String ANONYMOUS_URLS_INIT_PARAM = "ANONYMOUS_ALLOWED_URIS";
	
	private final Logger logger = LoggerFactory.getLogger(AuthenticatorFilter.class);
	private String ehrServiceCredentials;
	private String adminCredentials;
	private String[] ehrServiceURIs;
	private String[] adminURIs;
	private String[] citizenURIs;
	private String[] anonymousURIs;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		ehrServiceCredentials = filterConfig.getInitParameter(EHR_SERVICE_CREDENTIALS_INIT_PARAM);
		if (ehrServiceCredentials == null)
			throw new ServletException("Missing " + EHR_SERVICE_CREDENTIALS_INIT_PARAM + " config parameter, cannot start!");

		adminCredentials = filterConfig.getInitParameter(ADMIN_CREDENTIALS_INIT_PARAM);
		if (adminCredentials == null)
			throw new ServletException("Missing " + ADMIN_CREDENTIALS_INIT_PARAM + " config parameter, cannot start!");

		String tmp = filterConfig.getInitParameter(EHR_SERVICE_URLS_INIT_PARAM);
		if (tmp == null)
			throw new ServletException("Missing " + EHR_SERVICE_URLS_INIT_PARAM + " config parameter, cannot start!");
		tmp = tmp.replaceAll("[\n\r]", "");
		ehrServiceURIs = new StringTokenizer(tmp, ",").getTokenArray();
		
		tmp = filterConfig.getInitParameter(CITIZEN_URLS_INIT_PARAM);
		if (tmp == null)
			throw new ServletException("Missing " + CITIZEN_URLS_INIT_PARAM + " config parameter, cannot start!");
		tmp = tmp.replaceAll("[\n\r]", "");
		citizenURIs = new StringTokenizer(tmp, ",").getTokenArray();

		tmp = filterConfig.getInitParameter(ADMIN_URLS_INIT_PARAM);
		if (tmp == null)
			throw new ServletException("Missing " + ADMIN_URLS_INIT_PARAM + " config parameter, cannot start!");
		tmp = tmp.replaceAll("[\n\r]", "");
		adminURIs = new StringTokenizer(tmp, ",").getTokenArray();

		tmp = filterConfig.getInitParameter(ANONYMOUS_URLS_INIT_PARAM);
		if (tmp == null)
			throw new ServletException("Missing " + ANONYMOUS_URLS_INIT_PARAM + " config parameter, cannot start!");
		tmp = tmp.replaceAll("[\n\r]", "");
		anonymousURIs = new StringTokenizer(tmp, ",").getTokenArray();
	}

	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest hReq = (HttpServletRequest)request;
		HttpServletResponse hRes = (HttpServletResponse)response;
		
		if (hReq.getMethod().equals("PUT") || hReq.getMethod().equals("DELETE")) {
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "Method not allowed. Request NOT AUTHORIZED!");
			return;
		}
				
		// Detect the type of user that submits the request
		User user = null;
		try {
			user = detectUser(hReq);
		} catch (InvalidTokenException ite) {
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "Invalid token. Request NOT AUTHORIZED!");
			return;
		}
		
		// Checks if user is allowed to access request path
		if (isAllowed(user, hReq.getRequestURI().toString())) {
			// sets the user as a request attribute
			hReq.setAttribute(SecurityConstants.USER_ATTR_NAME, user);
			
			if (user.getRole() == UserRole.CITIZEN) {
				// sets the citizen as a request attribute
				hReq.setAttribute(SecurityConstants.CITIZEN_ATTR_NAME, user.getCitizen());
				logger.info("Request '{}' AUTHORIZED to user citizen {}", hReq.getRequestURI(), 
						user.getCitizen().getPersonIdentifier());
			} else
				logger.info("Request '{}' AUTHORIZED to user role {}", hReq.getRequestURI(), user.getRole());
		    chain.doFilter(request, response);			
		} else {
			logger.info("Request '{}' NOT AUTHORIZED to user role {}", hReq.getRequestURI(), user.getRole());
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "Request NOT AUTHORIZED!");
			return;			
		}
	}
	
	
	private User detectUser(HttpServletRequest hReq) throws InvalidTokenException {
		String authHeader = hReq.getHeader(SecurityConstants.AUTH_HEADER);
		
		// Checks for anonymous user
		if (hReq.getHeader(SecurityConstants.AUTH_HEADER) == null)
			return new User(UserRole.ANONYMOUS);

		// Checks for Admin or EHR_Service
		if (authHeader.startsWith(SecurityConstants.BASIC_PREFIX)) {
			// If basic authorization, only the EHR_SERVICE is authorized
			String base64Credentials = authHeader.substring(SecurityConstants.BASIC_PREFIX.length()).trim();
			final byte[] decodedBytes  = Base64.getDecoder().decode(base64Credentials.getBytes());
		    final String credentials = new String(decodedBytes);
		    
		    if (credentials.equals(ehrServiceCredentials))
				return new User(UserRole.EHR_SERVICE);
		    else if (credentials.equals(adminCredentials))
				return new User(UserRole.ADMIN);
		    else {
				logger.warn("Request with Basic Authorization contains wrong username/password!!");
				return new User(UserRole.ANONYMOUS);
		    }
		}
		
		if (authHeader.startsWith(SecurityConstants.OAUTH_PREFIX)) {
			// If oAuth authorization, only citizen is allowed
			try {
				String oAuthToken = authHeader.substring(SecurityConstants.OAUTH_PREFIX.length()).trim();
				ResponseDetails tokenDetails = SR2DSM.decode(oAuthToken);
				return new User(createCitizen(tokenDetails));
			} catch (Exception e) {
				throw new InvalidTokenException();
			}
		}
		
		return null;
	}

	
	private boolean isAllowed(User user, String requestedPath) {
		if (user.getRole() == UserRole.ADMIN) 
			return match(this.adminURIs, requestedPath);
		else if (user.getRole() == UserRole.EHR_SERVICE)
			return match(this.ehrServiceURIs, requestedPath);
		else if (user.getRole() == UserRole.CITIZEN)
			return match(this.citizenURIs, requestedPath);
		else if (user.getRole() == UserRole.ANONYMOUS)
			return match(this.anonymousURIs, requestedPath);
			
		return false;
	}
	
	private boolean match(String[] allowedPaths, String requestedPath) {
		for (String allowedPath : allowedPaths) {
			if (allowedPath.endsWith("*")) {
				allowedPath = allowedPath.substring(0, allowedPath.length() - 1);
				if (requestedPath.toLowerCase().startsWith(allowedPath.trim()))
					return true;
			} else if (requestedPath.equalsIgnoreCase(allowedPath.trim()))
				return true;
		}

		return false;
	}
	
	private Citizen createCitizen(ResponseDetails tokenDetails) {
		UserDetails user = tokenDetails.getUserDetails();
		
		Citizen c = new Citizen();
		c.setFirstName(user.getFirstName());
		c.setFamilyName(user.getFamilyName());
		c.setDateOfBirth(user.getDateOfBirth());
		c.setPersonIdentifier(user.getPersonIdentifier());
		
		return c;
	}
	
	@Override
	public void destroy() {}

}
