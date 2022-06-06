package eu.interopehrate.r2d.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "TBL_R2D_REQUEST", indexes = @Index(columnList = "citizenId"))
public class R2DRequest {
	
	private final static String CHAR_SEP = "::";
	
	@Id
	private String id;
	@Column(nullable = false)
	private String citizenId;
	@Column(nullable = false)
	private String uri;
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date creationTime;
	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdateTime;
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RequestStatus status;
	@Column
	private String failureMessage;
	@Column
	private String responseIds;
	@Column
	private String preferredLanguages;
	
	public R2DRequest() {
		this.setCreationTime(new Date());
		this.id = UUID.randomUUID().toString();
		this.status = RequestStatus.NEW;
	}

	public R2DRequest(String uri, String citizenId) {
		this();
		this.uri = uri;
		this.citizenId = citizenId;
	}
	
	public String getId() {
		return id;
	}
	
	void setId(String id) {
		this.id = id;
	}
	
	@JsonIgnore
	public String getCitizenId() {
		return citizenId;
	}

	public void setCitizenId(String citizenIdentifier) {
		this.citizenId = citizenIdentifier;
	}

	public String getUri() {
		return uri;
	}
	
	void setUri(String uri) {
		this.uri = uri;
	}

	public RequestStatus getStatus() {
		return status;
	}
	
	public void setStatus(RequestStatus status) {
		this.status = status;
		this.lastUpdateTime = new Date();
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", locale="")
	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
		this.lastUpdateTime = creationTime;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	public Date getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}
	
	public String getFailureMessage() {
		return failureMessage;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public String[] getResponses() {
		if (responseIds == null)
			return null;
		
		return responseIds.split(CHAR_SEP);
	}
	
	@JsonIgnore
	public String getFirstResponseId() {
		if (responseIds != null)
			return getResponses()[0];
		
		return null;
	}

	public void addResponseId(String responseId) {
		if (responseIds == null)
			responseIds = responseId;
		else
			responseIds = responseIds + CHAR_SEP + responseId;
		
	}

	public String getPreferredLanguages() {
		return preferredLanguages;
	}

	public void setPreferredLanguages(String preferredLanguages) {
		this.preferredLanguages = preferredLanguages;
	}

	@Override
	public String toString() {
		return "R2DRequest [id=" + id + ", citizenId=" + citizenId + ", uri=" + uri + ", creationTime=" + creationTime
				+ ", lastUpdateTime=" + lastUpdateTime + ", status=" + status + ", failureMessage=" + failureMessage
				+ ", responseIds=" + responseIds + ", preferredLanguages=" + preferredLanguages + "]";
	}

}
