package eu.interopehrate.r2d.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class RequestOutcome {
	
	private Date transactionTime;
	private String request;
	private boolean requiresAccessToken = true;
	private String error;
	
	private List<RequestOutput> output = new ArrayList<RequestOutput>();
	
	public RequestOutcome(String requestURI) {
		transactionTime = new Date();
		request = requestURI;
	}
	
	public void addOutput(RequestOutput out) {
		output.add(out);
	}
	

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	public Date getTransactionTime() {
		return transactionTime;
	}

	public void setTransactionTime(Date transactionTime) {
		this.transactionTime = transactionTime;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public boolean isRequiresAccessToken() {
		return requiresAccessToken;
	}

	public void setRequiresAccessToken(boolean requiresAccessToken) {
		this.requiresAccessToken = requiresAccessToken;
	}
	
	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public List<RequestOutput> getOutput() {
		return output;
	}

	public void setOutput(List<RequestOutput> output) {
		this.output = output;
	}

	@Override
	public String toString() {
		return "RequestOutcome [transactionTime=" + transactionTime + ", request=" + request + ", requiresAccessToken="
				+ requiresAccessToken + ", output=" + output + ", error=" + error + "]";
	}
	
}
