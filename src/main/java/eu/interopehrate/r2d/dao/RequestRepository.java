package eu.interopehrate.r2d.dao;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import eu.interopehrate.r2d.model.R2DRequest;

@Repository
public interface RequestRepository extends CrudRepository<R2DRequest, String>{
	
	// Looks for all the requests of a citizen
	@Query("SELECT r FROM R2DRequest r WHERE r.citizenId=:citizenId ORDER BY r.creationTime DESC")
	public List<R2DRequest> findByCitizenId(@Param("citizenId") String citizenId);

	
	// Looks for a specific request of the citizen
	@Query("SELECT r FROM R2DRequest r WHERE r.id=:requestId AND r.citizenId=:citizenId")
	public Optional<R2DRequest> findByRequestIdAndCitizenId(@Param("requestId") String requestId, 
			@Param("citizenId") String citizenId);
	

	// Counts all the RUNNING requests of a citizen
	@Query("SELECT r FROM R2DRequest r "
			+ "WHERE r.citizenId = :citizenId "
			+ "AND r.status = 'RUNNING' "
			+ "ORDER BY r.creationTime DESC ")
	public List<R2DRequest> findRunningRequestOfTheCitizen(@Param("citizenId") String citizenId);

	// Counts all the requests of a citizen in a certain state
	@Query("SELECT r FROM R2DRequest r "
			+ "WHERE r.citizenId = :citizenId "
			+ "AND r.status = :status "
			+ "ORDER BY r.creationTime DESC ")
	public List<R2DRequest> findByCitizenIdAndByStatus(@Param("citizenId") String citizenId,
			@Param("status") String status);

	
	// Counts all the RUNNING requests of a citizen
	@Query("SELECT COUNT(r) FROM R2DRequest r "
			+ "WHERE r.citizenId = :citizenId "
			+ "AND r.status = 'RUNNING' "
			+ "AND r.lastUpdateTime between :from AND :to ")
	public long countRunningRequestOfTheCitizenInPeriod(@Param("citizenId") String citizenId,
			@Param("from") Date from, @Param("to") Date to);

	
	// Looks for a cached response for a request
	@Query("SELECT r FROM R2DRequest r "
			+ " WHERE r.citizenId = :citizenId "
			+ " AND r.uri = :uri "
			+ " AND r.status = 'COMPLETED' "
			+ " AND r.lastUpdateTime between :from AND :to "
			+ " ORDER BY r.lastUpdateTime DESC ")
	public List<R2DRequest> findEquivalentValidRequest(@Param("citizenId") String citizenId,
			@Param("uri") String uri, @Param("from") Date from, @Param("to") Date to);
	
}
