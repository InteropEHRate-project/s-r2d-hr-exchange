R2D-Access-server - The InteropEHRate project (www.interopEHRate.eu)

This project implements the R2D Access Server a service provided by 
an Healthcare Organization (HCO) to allow a citizen to import his / her 
health data on the mobile phone. 


The R2D Access Server is a customization of the HAPI Plain Restful Server
in addition to its FHIR nature, the R2D Access Server provides uses a 
personal Spring context to load classes needed for
1) managing persistence of data
2) managing communication with the EHR Middleware
3) signing the Proveresources


The R2D Access Server provides an interface FHIR compliant, it implements 
a small subset of the whole FHIR Restful APIs specification. This is the list 
of the provided operations (they are all executed in the compartment of the 
authenticated citizen):
1) Search of Encounter
2) Search of Observation
3) Search of DiagnosticReport
4) Search of DocumentReference
5) Search of DocumentManifest
6) Search of Composition
7) Search of Condition
8) Search of AllergyIntolerance
9) Search of Immunization
10) Search of Procedure
11) Search of MedicationRequest
12) Operation Patient$everything
13) Operation (non standard) Patient$patient-summary
14) Operation Composition$everything
15) Operation Encounter$everything

The R2D Access Server forwards each incoming calls to another service provided by the
HCO called EHR-Middleware, this service will retrieve data from the EHR of the HCO and
will return them to the R2D Access Server in the FHIR / JSON format defined by the
InteropEHRate Interoperability Implementation Guides. The whole transaction is executed
in an asynchrnous way, the asynchrnicity is managed by the R2D Access Server, that replies
with a 202 to the initial request and then provides to client an URL to be used for polling
the status of the request. When the request ends, requesting the polling URL will return
all data needed to access the produced data.

The R2DAccess Server has a security layer based on eIDAS, it only allows requests having
a valid eIDAS Token. The citizen referenced by the eIDAS Token MUST be identified by the
HCO using name, surname and date of birth. 



