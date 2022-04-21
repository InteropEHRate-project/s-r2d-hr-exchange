package eu.interopehrate.r2d.provenance;

import java.util.List;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportMediaComponent;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public class DiagnosticReportNode extends ResourceNode {

	DiagnosticReportNode (DiagnosticReport resource) {
		super(resource);
		this.resource = resource;
	}

	@Override
	void loadChildren(ResourceNode root) {
		List<Reference> resRefs = ((DiagnosticReport)resource).getResult();
		Resource currentResource;
        for (Reference ref : resRefs) {
        	currentResource = (Resource)ref.getResource();
    		if (currentResource != null) {
    			ResourceNode n = root.searchNodeByResourceId(currentResource.getId());
    			if (n == null) {
    				n = NodeFactory.createNode(currentResource);
				    n.loadChildren(this);
    			} 
			    n.setParent(this);
    		}
        }
        
        List<DiagnosticReportMediaComponent> mediaList = ((DiagnosticReport)resource).getMedia();
        Media media;
        for (DiagnosticReportMediaComponent mediaComp : mediaList) {
        	media = (Media) mediaComp.getLink().getResource();
        	if (media != null) {
    			ResourceNode n = root.searchNodeByResourceId(media.getId());
    			if (n == null) {
    				n = NodeFactory.createNode(media);
				    n.loadChildren(this);
    			} 
				n.setParent(this);
        	}
        }
	}
	
	
	Provenance addProvenance(DomainResource provider) {
		DiagnosticReport diagnosticReport = (DiagnosticReport)getResource();
		
		DomainResource author = provider;
		if (diagnosticReport.getPerformerFirstRep() != null)
			author = (DomainResource)diagnosticReport.getPerformerFirstRep().getResource();
		// creates the Provenance
		Provenance provenance = ProvenanceBuilder.build(diagnosticReport, author, provider);
		// adds extension to children
		Extension provExt = diagnosticReport.getExtensionByUrl(ProvenanceBuilder.PROV_EXT_NAME);
		for (ResourceNode child : children)
			child.addProvenanceExtension(provExt);
		
		return provenance;
	}
	
}
