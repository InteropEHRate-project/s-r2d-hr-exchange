package eu.interopehrate.r2d.exceptions;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Generic exception sent to clients.
 */
public class R2DException extends Exception {

	private static final long serialVersionUID = 7064164021613625133L;

	public static final int NOT_YET_IMPLEMENTD = 100;

	public static final int COMMUNICATION_ERROR = 300;
	
	public static final int BAD_REQUEST = 400;
	public static final int TOO_MANY_RUNNING_REQUEST = 410;
	public static final int REQUEST_NOT_FOUND = 420;
	public static final int INVALID_STATE = 430;
	public static final int INVALID_BUNDLE = 440;
	
	public static final int INTERNAL_ERROR = 500;

	public int code; 
	
	public R2DException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public R2DException(int code, String message) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
	
}
