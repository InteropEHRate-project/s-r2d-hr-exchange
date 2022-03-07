package eu.interopehrate.r2d.security;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *     
 *     Security constants
 */

public final class SecurityConstants {
	
	/**
	 * Name of the header attribute that stores the authentication token
	 */
	public final static String AUTH_HEADER = "Authorization";
	
	/**
	 * Prefix of the value of the header attribute named AUTH_HEADER
	 */
	public final static String OAUTH_PREFIX = "Bearer ";

	/**
	 * Prefix of the value of the header attribute named AUTH_HEADER
	 */
	public final static String BASIC_PREFIX = "Basic ";
	
	/**
	 * key for the instance of authenticated user stored as request attribute
	 */
	public final static String CITIZEN_ATTR_NAME = "eIDAS-Citizen";
	
	/**
	 * key for storing the id of the R2D Request before sending it to EHR
	 */
	public static final String R2D_REQUEST_ID_PARAM_NAME = "R2D-Request-Id";

}
