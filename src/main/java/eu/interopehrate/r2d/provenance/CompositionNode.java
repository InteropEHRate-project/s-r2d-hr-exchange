package eu.interopehrate.r2d.provenance;

import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Composition.SectionComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public class CompositionNode extends ResourceNode {

	CompositionNode(Composition resource) {
		super(resource);
	}

	@Override
	void loadChildren(ResourceNode root) {
		Composition composition = (Composition)resource;
		
		Resource currentResource;
		for (SectionComponent currentSection : composition.getSection()) {
        	
        	for (Reference currentRef : currentSection.getEntry()) {
        		currentResource = (Resource) currentRef.getResource();
        		if (currentResource != null) {
        			
	    			ResourceNode n = root.searchNodeByResourceId(currentResource.getId());
	    			if (n == null) {
	    				n = NodeFactory.createNode(currentResource);
					    n.loadChildren(this);
	    			} 
					n.setParent(this);
        		}        		
        	}
        }
	}

	Provenance addProvenance(DomainResource provider) {
		Composition composition = (Composition)resource;
		
		DomainResource author = provider;
		if (composition.getAuthorFirstRep() != null)
			author = (DomainResource)composition.getAuthorFirstRep().getResource();
		// creates the Provenance
		Provenance provenance = ProvenanceBuilder.build(composition, author, provider);
		// adds extension to children
		Extension provExt = composition.getExtensionByUrl(ProvenanceBuilder.PROV_EXT_NAME);
		for (ResourceNode child : children)
			child.addProvenanceExtension(provExt);
		
		return provenance;
	}
	
}
