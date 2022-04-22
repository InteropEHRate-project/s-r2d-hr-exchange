package eu.interopehrate.r2d.provenance;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.interopehrate.r2d.Configuration;
import eu.interopehrate.r2d.security.ResourceSigner;

public class BundleProvenanceBuilder {
	private static final Logger logger = LoggerFactory.getLogger(BundleProvenanceBuilder.class);
	
	private Organization providerOrg;
	
	public BundleProvenanceBuilder() {
		providerOrg = new Organization();
		providerOrg.setId("1");
		
        Identifier id = new Identifier();
        id.setValue(Configuration.getProperty("provenance.provider.identifier"));
        providerOrg.addIdentifier(id);

        providerOrg.setActive(true);

        providerOrg.setName(Configuration.getProperty("provenance.provider.name"));

        providerOrg.addAddress().addLine(Configuration.getProperty("provenance.provider.address"))
        .setCity(Configuration.getProperty("provenance.provider.city"))
        .setState(Configuration.getProperty("provenance.provider.state"))
        .setPostalCode(Configuration.getProperty("provenance.provider.postalcode"))
        .setUse(Address.AddressUse.WORK);
	}
	
	/**
	 * Inspects a bundle and build the Provenance information for each 
	 * resource that must be signed.
	 * 
	 * @param bundle
	 * @param provider
	 * @throws Exception
	 */
	public void addProvenanceToBundleItems(Bundle bundle) throws Exception {
		// #1 creates a tree representation of the items in the bundle
		// recreating a hierarchical structure, simpler to navigate.
		ResourceNode root = createTree(bundle);
		
		// #2 Adds the provenance with the signature to each item at level 1 
		List<Provenance> provenances = addProvenanceToTree(root);
		
		
		// #4 adds all the provenances created and the provider to the bundle
        for (Provenance p : provenances)
           	bundle.addEntry().setResource(p);
        
        bundle.addEntry().setResource(providerOrg);
	}
	
	
	private List<Provenance> addProvenanceToTree(ResourceNode root) throws Exception {
		final List<Provenance> provenances = new ArrayList<Provenance>();
		Provenance provenance;
		Resource resource;
		String jwsToken;
		
		for (ResourceNode child : root.getChildren()) {
			if (logger.isDebugEnabled())
				logger.debug("Creating provenance for resource: " + child.getResource());		
			// Adds the provenance to the child
			provenance = child.addProvenance(providerOrg);
			// Gets the resource linked to the current node
			resource = child.getResource();
			// signs the resource and creates the jwsToken
			jwsToken = ResourceSigner.INSTANCE.createJWSToken(resource);
			// adds the jwsToken to the provenance
			provenance.getSignatureFirstRep().setData(jwsToken.getBytes());
			
			provenances.add(provenance);
		}
		
		return provenances;
	}
	
	
	private ResourceNode createTree(Bundle bundle) {
		ResourceNode root = NodeFactory.createNode(bundle);
		root.loadChildren(root);
		return root;
	}

}
