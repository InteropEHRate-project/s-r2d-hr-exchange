package eu.interopehrate.r2d.providers;

import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.interopehrate.r2d.business.RequestProcessor;

public abstract class AbstractResourceProvider implements IResourceProvider {	

	@Autowired(required = true)
	protected RequestProcessor requestProcessor;

}
