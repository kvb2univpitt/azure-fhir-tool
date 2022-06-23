/*
 * Copyright (C) 2022 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.pitt.dbmi.azure.fhir.tool.ctrlr.rest;

import edu.pitt.dbmi.azure.fhir.tool.model.BasicPatient;
import edu.pitt.dbmi.azure.fhir.tool.model.BasicPatientSearchResults;
import edu.pitt.dbmi.azure.fhir.tool.service.ResourceCountService;
import edu.pitt.dbmi.azure.fhir.tool.service.fhir.PatientResourceService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * May 20, 2022 10:46:38 AM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@RestController
@RequestMapping("/fhir/api/patient")
public class PatientRestController {

    private final PatientResourceService patientResourceService;
    private final ResourceCountService resourceCountService;

    @Autowired
    public PatientRestController(PatientResourceService patientResourceService, ResourceCountService resourceCountService) {
        this.patientResourceService = patientResourceService;
        this.resourceCountService = resourceCountService;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicPatientSearchResults getPatients(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authClient,
            @RequestParam("start") Optional<Integer> start,
            @RequestParam("length") Optional<Integer> length) {
        List<Patient> patients = (start.isPresent() && length.isPresent())
                ? patientResourceService.getPatients(authClient.getAccessToken(), start.get(), length.get())
                : patientResourceService.getPatients(authClient.getAccessToken());
        int counts = resourceCountService.getPatientCounts(authClient.getAccessToken());

        BasicPatientSearchResults results = new BasicPatientSearchResults();
        results.setBasicPatients(patients.stream().map(e -> toBasicPatient(e)).collect(Collectors.toList()));
        results.setRecordsTotal(counts);
        results.setRecordsFiltered(counts);

        return results;
    }

    private BasicPatient toBasicPatient(Patient patient) {
        BasicPatient basicPatient = new BasicPatient();
        basicPatient.setId(patient.getIdElement().getIdPart());
        basicPatient.setGender(patient.getGender().getDisplay());

        // extract first name and last name
        patient.getName().stream()
                .filter(name -> name.getUse() == HumanName.NameUse.OFFICIAL)
                .forEach(name -> {
                    basicPatient.setLastName(name.getFamily());
                    basicPatient.setFirstName(name.getGivenAsSingleString());
                });

        return basicPatient;
    }

}
