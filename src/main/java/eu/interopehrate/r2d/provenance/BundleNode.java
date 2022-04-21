package eu.interopehrate.r2d.provenance;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ResourceType;

public class BundleNode extends ResourceNode {
	ResourceType[] allowedTypes = {
			(new Observation()).getResourceType(),
			(new DocumentReference()).getResourceType(),
			(new Media()).getResourceType(),
			(new Condition()).getResourceType(),
			(new Medication()).getResourceType(),
			(new MedicationStatement()).getResourceType(),
			(new MedicationRequest()).getResourceType(),
			(new Encounter()).getResourceType(),
			(new DiagnosticReport()).getResourceType(),
			(new Composition()).getResourceType(),
			(new CarePlan()).getResourceType(),
	};
	
	BundleNode(Bundle resource) {
		super(resource);
	}

	@Override
	void loadChildren(ResourceNode root) {
		ResourceType resourceType;
		Bundle bundle = (Bundle)resource;
		ResourceNode n;
		
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {  
        	resourceType = entry.getResource().getResourceType();
			if (isAllowed(resourceType)) {			
				n = root.searchNodeByResourceId(entry.getResource().getId());
				if (n == null) {
					n = NodeFactory.createNode(entry.getResource());
					n.loadChildren(this);
					n.setParent(this);
				}
			}
        }	
	}
	
	private boolean isAllowed(ResourceType resourceType) {
		for (ResourceType c : allowedTypes) {
			if (c.equals(resourceType))
				return true;
		}
		
		return false;
	}
	
	void printTree(int level) {
		System.out.println("root");
		level++;
		for (ResourceNode n : children)
			n.printTree(level);	
	}
}
