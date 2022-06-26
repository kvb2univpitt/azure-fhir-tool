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
import edu.pitt.dbmi.fhir.resource.mapper.r4.brainai.EncounterResourceMapper;
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
 * May 28, 2022 7:55:18 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Service
public class EncounterResourceService extends AbstractResourceService {

    @Autowired
    public EncounterResourceService(@Value("${fhir.url}") String fhirUrl, FhirContext fhirContext) {
        super(fhirUrl, fhirContext);
    }

    public void deleteEncounters(OAuth2AccessToken accessToken) {
        int batchSize = 500;
        deleteResources(getEncounterBundle(accessToken, batchSize), batchSize, getClient(accessToken));
    }

    public void uploadEncounters(List<String> data, OAuth2AccessToken accessToken) throws ParseException {
        if (data == null || data.isEmpty()) {
            return;
        }

        Map<String, Patient> patientReferences = new HashMap<>();
        int batchSize = 500;
        List<String> batch = new LinkedList<>();
        for (String line : data) {
            if (batch.size() == batchSize) {
                addEncounters(batch, patientReferences, accessToken);
                batch.clear();
            }

            batch.add(line);
        }

        // upload the rest of the data
        addEncounters(batch, patientReferences, accessToken);
        batch.clear();
    }

    private void addEncounters(List<String> batch, Map<String, Patient> patientReferences, OAuth2AccessToken accessToken) throws ParseException {
        List<Resource> resources = new LinkedList<>();
        for (String line : batch) {
            Encounter encounter = EncounterResourceMapper.getEncounter(Delimiters.TAB_DELIM.split(line));

            Patient patient = patientReferences.get(encounter.getSubject().getReference());
            if (patient == null) {
                Resource resource = findPatient(encounter.getSubject(), getClient(accessToken)).getEntryFirstRep().getResource();
                if (resource != null) {
                    patient = (Patient) resource;
                    patientReferences.put(encounter.getSubject().getReference(), patient);
                }
            }
            if (patient != null) {
                encounter.setSubject(new Reference()
                        .setReference("Patient/" + patient.getIdElement().getIdPart())
                        .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));
            }

            resources.add(encounter);
        }

        addResources(resources, "Encounter", getClient(accessToken));
    }

    public Encounter getEncounter(final OAuth2AccessToken accessToken, final String id) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(Encounter.class)
                .where(Encounter.RES_ID.exactly().identifier(id))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return bundle.getEntry().stream()
                .map(e -> (Encounter) e.getResource())
                .findFirst()
                .orElse(null);
    }

    public Bundle getEncounterBundle(OAuth2AccessToken accessToken, int batchSize) {
        return getClient(accessToken)
                .search()
                .forResource(Encounter.class)
                .returnBundle(Bundle.class)
                .count(batchSize)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();
    }

    public List<Encounter> getEncounters(OAuth2AccessToken accessToken) {
        IGenericClient client = getClient(accessToken);

        Bundle bundle = client
                .search()
                .forResource(Encounter.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return fetchAllResources(client, bundle).stream()
                .map(e -> (Encounter) e)
                .collect(Collectors.toList());
    }

    public List<Encounter> getEncounters(List<String> lines, int start, int length) {
        List<Encounter> encounters = new LinkedList<>();

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
                    encounters.add(EncounterResourceMapper.getEncounter(delimiter.split(line)));
                } catch (ParseException exception) {
                    exception.printStackTrace(System.err);
                }
            }
            index++;
        }

        return encounters;
    }

    public List<Encounter> getEncounters(OAuth2AccessToken accessToken, int start, int length) {
        List<Encounter> encounters = new LinkedList<>();

        int size = start + length;
        int count = (size < 1000) ? size : 1000;
        IGenericClient client = getClient(accessToken);
        Bundle bundle = client
                .search()
                .forResource(Encounter.class)
                .sort().ascending(Encounter.DATE)
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
                    encounters.add((Encounter) component.getResource());
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
                        encounters.add((Encounter) component.getResource());
                    }
                    index++;
                    fetchMore = index < size;
                }
            }
        }

        return encounters;
    }

}
