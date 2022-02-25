package eu.interopehrate.r2d.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.OperationOutcome;

import com.fasterxml.jackson.annotation.JsonFormat;

public class RequestOutcome {
	
	private Date transactionTime;
	private String request;
	private boolean requiresAccessToken = true;
	
	private List<RequestOutput> output = new ArrayList<RequestOutput>();
	private List<OperationOutcome> error = new ArrayList<OperationOutcome>();
	
	public RequestOutcome(String requestURI) {
		transactionTime = new Date();
		request = requestURI;
	}
	
	public void addOutput(RequestOutput out) {
		output.add(out);
	}
	
	
	public void addError(OperationOutcome out) {
		error.add(out);
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

	public List<RequestOutput> getOutput() {
		return output;
	}

	public void setOutput(List<RequestOutput> output) {
		this.output = output;
	}

	public List<OperationOutcome> getError() {
		return error;
	}

	public void setError(List<OperationOutcome> error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "RequestOutcome [transactionTime=" + transactionTime + ", request=" + request + ", requiresAccessToken="
				+ requiresAccessToken + ", output=" + output + ", error=" + error + "]";
	}
	
}
