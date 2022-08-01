package eu.interopehrate.r2d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentManifest;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: declares the list of resource type managed by the R2DAccessServer
 */
public class R2DATypes {
	
	private static final List<String> r2daTypes = new ArrayList<String>();
	
	static {
		r2daTypes.add((new Patient()).fhirType());
		r2daTypes.add((new DocumentReference()).fhirType());
		r2daTypes.add((new DocumentManifest()).fhirType());
		r2daTypes.add((new DiagnosticReport()).fhirType());
		r2daTypes.add((new MedicationRequest()).fhirType());
		r2daTypes.add((new Condition()).fhirType());
		r2daTypes.add((new Immunization()).fhirType());
		r2daTypes.add((new AllergyIntolerance()).fhirType());
		r2daTypes.add((new Observation()).fhirType());
		r2daTypes.add((new Encounter()).fhirType());
		r2daTypes.add((new Composition()).fhirType());
		r2daTypes.add((new Procedure()).fhirType());
	}

	public static Stream<String> getTypesAsStream() {
		return r2daTypes.stream();
	}

	public static Collection<String> getTypes() {
		return r2daTypes;
	}

}
