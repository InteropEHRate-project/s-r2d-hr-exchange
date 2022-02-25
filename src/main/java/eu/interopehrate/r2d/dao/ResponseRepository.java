package eu.interopehrate.r2d.dao;

import java.util.Date;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import eu.interopehrate.r2d.model.R2DResponse;

@Repository
public interface ResponseRepository extends CrudRepository<R2DResponse, String>{
		
	@Query("SELECT r FROM R2DResponse r WHERE r.creationTime < :deletionLimit ")
	public Iterable<R2DResponse> deleteOldResponses(@Param("deletionLimit") Date deletionLimit );
	
}
