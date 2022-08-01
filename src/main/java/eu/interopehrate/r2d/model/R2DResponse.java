package eu.interopehrate.r2d.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: persistent class representing a response to a request
 * made by a citizen.
 */

@Entity
@Table(name = "TBL_R2D_RESPONSE")
public class R2DResponse {
	
	@Id
	private String id;
	@Column(nullable = false)
	private String citizenId;
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date creationTime;
	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date downloadTime;
	@Column
	private String responseFileName;
	
	public R2DResponse() {
		this.creationTime = new Date();
		this.id = UUID.randomUUID().toString();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCitizenId() {
		return citizenId;
	}

	public void setCitizenId(String citizenId) {
		this.citizenId = citizenId;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	public Date getDownloadTime() {
		return downloadTime;
	}

	public void setDownloadTime(Date downloadTime) {
		this.downloadTime = downloadTime;
	}

	public String getResponseFileName() {
		return responseFileName;
	}

	public void setResponseFileName(String responseFileName) {
		this.responseFileName = responseFileName;
	}

	@Override
	public String toString() {
		return "R2DResponse [id=" + id + ", citizenId=" + citizenId + ", creationTime=" + creationTime
				+ ", downloadTime=" + downloadTime + ", responseFileName=" + responseFileName + "]";
	}

}
