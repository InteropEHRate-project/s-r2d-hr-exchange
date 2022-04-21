package eu.interopehrate.r2d.security;

import java.security.PrivateKey;

import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.interopehrate.r2d.Configuration;
import iehr.security.CryptoManagementFactory;
import iehr.security.api.CryptoManagement;

public class ResourceSigner {
	
	public static final ResourceSigner INSTANCE = new ResourceSigner();
	private CryptoManagement cryptoManagement;
	private PrivateKey privateKey;
	private byte[] certificateData;
	private IParser parser;

	private ResourceSigner() {}
	
	public void initialize() throws Exception {
		// Creates the instance of CryptoManagement
		cryptoManagement = CryptoManagementFactory.create(
				Configuration.getProperty("signature.certificate"),
				Configuration.getProperty("signature.certificate.CA"));
		
		// retrieves the health org private key
		String certAlias = Configuration.getProperty("signature.certificate.alias");
        privateKey = cryptoManagement.getPrivateKey(certAlias);
		// retrieves the health org certificate: MUST BE DONE ONCE
		certificateData = cryptoManagement.getUserCertificate(certAlias);
		// Instantiate the parser
		parser = FhirContext.forR4().newJsonParser();
	}
	
	
	public String createJWSToken(Resource resource) throws Exception {
		// signs the resource
		String signedResource = cryptoManagement.signPayload(
				parser.encodeResourceToString(resource), privateKey);
		
		// Creates the JWS token
		return cryptoManagement.createDetachedJws(certificateData, signedResource);
	}

}
