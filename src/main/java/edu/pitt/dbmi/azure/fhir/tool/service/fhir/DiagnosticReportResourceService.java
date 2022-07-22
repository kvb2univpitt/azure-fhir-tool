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
package edu.pitt.dbmi.azure.fhir.tool.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.pitt.dbmi.azure.fhir.tool.service.AbstractResourceService;
import edu.pitt.dbmi.fhir.resource.mapper.r4.brainai.DiagnosticReportResourceMapper;
import edu.pitt.dbmi.fhir.resource.mapper.util.Delimiters;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

/**
 *
 * Jul 21, 2022 2:12:27 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Service
public class DiagnosticReportResourceService extends AbstractResourceService {

    @Autowired
    public DiagnosticReportResourceService(@Value("${fhir.url}") String fhirUrl, FhirContext fhirContext) {
        super(fhirUrl, fhirContext);
    }

    public void deleteDiagnosticReports(OAuth2AccessToken accessToken) {
        int batchSize = 500;
        deleteResources(getDiagnosticReportBundle(accessToken, batchSize), batchSize, getClient(accessToken));
    }

    public void uploadDiagnosticReports(List<String> data, OAuth2AccessToken accessToken) throws ParseException {
        if (data == null || data.isEmpty()) {
            return;
        }

        Map<String, Patient> patientReferences = new HashMap<>();
        Map<String, Encounter> encounterReferences = new HashMap<>();
        int batchSize = 500;
        List<String> batch = new LinkedList<>();
        for (String line : data) {
            if (batch.size() == batchSize) {
                addDiagnosticReports(batch, patientReferences, encounterReferences, accessToken);
                batch.clear();
            }

            batch.add(line);
        }

        // upload the rest of the data
        addDiagnosticReports(batch, patientReferences, encounterReferences, accessToken);
        batch.clear();
    }

    private void addDiagnosticReports(List<String> batch, Map<String, Patient> patientReferences, Map<String, Encounter> encounterReferences, OAuth2AccessToken accessToken) throws ParseException {
        List<Resource> resources = new LinkedList<>();
        for (String line : batch) {
            DiagnosticReport diagnosticReport = DiagnosticReportResourceMapper.getDiagnosticReport(Delimiters.TAB_DELIM.split(line));

            Patient patient = patientReferences.get(diagnosticReport.getSubject().getReference());
            if (patient == null) {
                Resource resource = findPatient(diagnosticReport.getSubject(), getClient(accessToken)).getEntryFirstRep().getResource();
                if (resource != null) {
                    patient = (Patient) resource;
                    patientReferences.put(diagnosticReport.getSubject().getReference(), patient);
                }
            }
            Encounter encounter = encounterReferences.get(diagnosticReport.getEncounter().getReference());
            if (encounter == null) {
                Resource resource = findEncounter(diagnosticReport.getEncounter(), getClient(accessToken)).getEntryFirstRep().getResource();
                if (resource != null) {
                    encounter = (Encounter) resource;
                    encounterReferences.put(diagnosticReport.getEncounter().getReference(), encounter);
                }
            }

            if (!(patient == null || encounter == null)) {
                diagnosticReport.setSubject(new Reference()
                        .setReference("Patient/" + patient.getIdElement().getIdPart())
                        .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
                diagnosticReport.setEncounter(new Reference()
                        .setReference("Encounter/" + encounter.getIdElement().getIdPart()));
            }

            resources.add(diagnosticReport);
        }

        addResources(resources, "DiagnosticReport", getClient(accessToken));
    }

    public DiagnosticReport getDiagnosticReport(final OAuth2AccessToken accessToken, final String id) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(DiagnosticReport.class)
                .where(DiagnosticReport.RES_ID.exactly().identifier(id))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return bundle.getEntry().stream()
                .map(e -> (DiagnosticReport) e.getResource())
                .findFirst()
                .orElse(null);
    }

    public Bundle getDiagnosticReportBundle(OAuth2AccessToken accessToken, int batchSize) {
        return getClient(accessToken)
                .search()
                .forResource(DiagnosticReport.class)
                .returnBundle(Bundle.class)
                .count(batchSize)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();
    }

    public List<DiagnosticReport> getDiagnosticReports(OAuth2AccessToken accessToken) {
        IGenericClient client = getClient(accessToken);

        Bundle bundle = client
                .search()
                .forResource(DiagnosticReport.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return fetchAllResources(client, bundle).stream()
                .map(e -> (DiagnosticReport) e)
                .collect(Collectors.toList());
    }

    public List<DiagnosticReport> getDiagnosticReports(List<String> lines, int start, int length) {
        List<DiagnosticReport> diagnosticReports = new LinkedList<>();

        Pattern delimiter = Delimiters.TAB_DELIM;
        int size = start + length;
        int offset = start;
        int index = 0;
        for (String line : lines) {
            if (index >= size) {
                break;
            }

            if (offset <= index) {
                try {
                    diagnosticReports.add(DiagnosticReportResourceMapper.getDiagnosticReport(delimiter.split(line)));
                } catch (ParseException exception) {
                    exception.printStackTrace(System.err);
                }
            }
            index++;
        }

        return diagnosticReports;
    }

    public List<DiagnosticReport> getDiagnosticReports(OAuth2AccessToken accessToken, int start, int length) {
        List<DiagnosticReport> diagnosticReports = new LinkedList<>();

        int size = start + length;
        int count = (size < 1000) ? size : 1000;
        IGenericClient client = getClient(accessToken);
        Bundle bundle = client
                .search()
                .forResource(DiagnosticReport.class)
                .sort().ascending(DiagnosticReport.DATE)
                .count(count)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        int offset = start;
        int index = 0;
        boolean fetchMore = index < size;
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            if (fetchMore) {
                if (offset <= index) {
                    diagnosticReports.add((DiagnosticReport) component.getResource());
                }
                index++;
                fetchMore = index < size;
            }
        }
        while (fetchMore && bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            bundle = client.loadPage().next(bundle).execute();
            for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
                if (fetchMore) {
                    if (offset <= index) {
                        diagnosticReports.add((DiagnosticReport) component.getResource());
                    }
                    index++;
                    fetchMore = index < size;
                }
            }
        }

        return diagnosticReports;
    }

}
