package eu.interopehrate.r2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import eu.interopehrate.r2d.interceptors.AsyncRequestHandler;
import eu.interopehrate.r2d.interceptors.CapabilityStatementCustomizer;
import eu.interopehrate.r2d.providers.AllergyIntoleranceResourceProvider;
import eu.interopehrate.r2d.providers.CompositionResourceProvider;
import eu.interopehrate.r2d.providers.ConditionResourceProvider;
import eu.interopehrate.r2d.providers.DiagnosticReportResourceProvider;
import eu.interopehrate.r2d.providers.DocumentManifestResourceProvider;
import eu.interopehrate.r2d.providers.DocumentReferenceResourceProvider;
import eu.interopehrate.r2d.providers.EncounterResourceProvider;
import eu.interopehrate.r2d.providers.ImmunizationResourceProvider;
import eu.interopehrate.r2d.providers.MedicationRequestResourceProvider;
import eu.interopehrate.r2d.providers.ObservationResourceProvider;
import eu.interopehrate.r2d.providers.PatientResourceProvider;
import eu.interopehrate.r2d.providers.ProcedureResourceProvider;

public class R2DAccessServer extends RestfulServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7367855477396438198L;
	private static final Logger logger = LoggerFactory.getLogger(R2DAccessServer.class);

	public R2DAccessServer() {
		super(FhirContext.forR4());
	}
	
	@Override
	protected void initialize() throws ServletException {
		logger.debug("Initializing R2DAccessServer...");
		
		/*
		 * Two resource providers are defined. Each one handles a specific
		 * type of resource.
		 */
		List<IResourceProvider> providers = new ArrayList<>();
		providers.add(R2DContextProvider.getApplicationContext().getBean(EncounterResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(PatientResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(AllergyIntoleranceResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(CompositionResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(ConditionResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(DiagnosticReportResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(DocumentManifestResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(DocumentReferenceResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(ImmunizationResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(MedicationRequestResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(ObservationResourceProvider.class));
		providers.add(R2DContextProvider.getApplicationContext().getBean(ProcedureResourceProvider.class));
		setResourceProviders(providers);
		
		/*
		 * Use a narrative generator. This is a completely optional step, 
		 * but can be useful as it causes HAPI to generate narratives for
		 * resources which don't otherwise have one.
		 */
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);

		/*
		 * Enable CORS
		 */
		CorsConfiguration config = new CorsConfiguration();
		CorsInterceptor corsInterceptor = new CorsInterceptor(config);
		config.addAllowedHeader("Accept");
		config.addAllowedHeader("Content-Type");
		config.addAllowedOrigin("*");
		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		//config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
		config.setAllowedMethods(Arrays.asList("GET"));
		registerInterceptor(corsInterceptor);

		/*
		 * This server interceptor causes the server to return nicely
		 * formatter and coloured responses instead of plain JSON/XML if
		 * the request is coming from a browser window. It is optional,
		 * but can be nice for testing.
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());
		registerInterceptor(new CapabilityStatementCustomizer());
		registerInterceptor(new AsyncRequestHandler());
		
		/*
		 * Tells the server to return pretty-printed responses by default
		 */
		setDefaultPrettyPrint(true);
	}



}
