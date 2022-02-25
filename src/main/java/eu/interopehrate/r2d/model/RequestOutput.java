package eu.interopehrate.r2d.model;

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
