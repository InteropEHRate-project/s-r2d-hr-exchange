package eu.interopehrate.r2d.model;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: this class represent an output produced by a R2DRequest. The
 * URL allows a client to retrieve this result.
 */
public class RequestOutput {

	private String type;
	private String url;
	
	public RequestOutput() {
		super();
	}

	public RequestOutput(String type, String uRL) {
		super();
		this.type = type;
		url = uRL;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String uRL) {
		url = uRL;
	}
	
}
