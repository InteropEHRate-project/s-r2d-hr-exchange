package eu.interopehrate.r2d.provenance;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Resource;

import eu.interopehrate.r2d.security.ResourceSigner;

public class BundleProvenanceBuilder {
	
	private Organization providerOrg;
	
	public BundleProvenanceBuilder() {
		Organization providerOrg = new Organization();
		providerOrg.setId("1");
		
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
		
		
		// #4 adds all the provenances created to the bundle
        for (Provenance p : provenances) {
        	System.out.println("Adding provenance to Bundle: " + p.getId());
           	bundle.addEntry().setResource(p);
        }
	}
	
	
	private List<Provenance> addProvenanceToTree(ResourceNode root) throws Exception {
		final List<Provenance> provenances = new ArrayList<Provenance>();
		Provenance provenance;
		Resource resource;
		String jwsToken;
		
		for (ResourceNode child : root.getChildren()) {
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
