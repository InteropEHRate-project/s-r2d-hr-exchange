package eu.interopehrate.r2d.provenance;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Resource;

public class NodeFactory {
	
	public static ResourceNode createNode(Resource resource) {
		if (resource instanceof DiagnosticReport)
			return new DiagnosticReportNode((DiagnosticReport)resource);
		else if (resource instanceof Composition)
			return new CompositionNode((Composition)resource);
		else if (resource instanceof Bundle)
			return new BundleNode((Bundle)resource);
		else 
			return new ResourceNode(resource);
	}

}
