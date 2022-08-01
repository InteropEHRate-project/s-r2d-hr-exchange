package eu.interopehrate.r2d.utils;

import eu.interopehrate.r2d.R2DATypes;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: utility for managing URLs
 */

public class URLUtility {

	/**
	 * 
	 * @param fullURL
	 * @return
	 */
	public static String extractR2DSubQuery(String fullURL) {
		int idx = 0;
		for (String type : R2DATypes.getTypes()) {
			if ((idx = fullURL.indexOf(type)) > 0) {
				return fullURL.substring(idx - 1);
			}
		}
		
		throw new IllegalArgumentException("Provided URL does not contain a valid R2D query.");
	}
	
}
