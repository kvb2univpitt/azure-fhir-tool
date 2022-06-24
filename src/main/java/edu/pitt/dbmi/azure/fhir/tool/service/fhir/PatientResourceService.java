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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

/**
 *
 * May 10, 2022 8:39:35 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Service
public class PatientResourceService extends AbstractResourceService {

    @Autowired
    public PatientResourceService(@Value("${fhir.url}") String fhirUrl, FhirContext fhirContext) {
        super(fhirUrl, fhirContext);
    }

    public Patient getPatient(final OAuth2AccessToken accessToken, final String id) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(Patient.class)
                .where(Patient.RES_ID.exactly().identifier(id))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return bundle.getEntry().stream()
                .map(e -> (Patient) e.getResource())
                .findFirst()
                .orElse(null);
    }

    public List<Patient> getPatients(OAuth2AccessToken accessToken) {
        IGenericClient client = getClient(accessToken);

        Bundle bundle = client
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return fetchAllResources(client, bundle).stream()
                .map(e -> (Patient) e)
                .collect(Collectors.toList());
    }

    public List<Patient> getPatients(OAuth2AccessToken accessToken, int start, int length) {
        List<Patient> patients = new LinkedList<>();

        int size = start + length;
        int count = (size < 1000) ? size : 1000;
        IGenericClient client = getClient(accessToken);
        Bundle bundle = client
                .search()
                .forResource(Patient.class)
                .sort().ascending(Patient.NAME)
                .count(count)
                .returnBundle(Bundle.class)
                .execute();

        int offset = start;
        int index = 0;
        boolean fetchMore = index < size;
        for (Bundle.BundleEntryComponent component : bundle.getEntry()) {
            if (fetchMore) {
                if (offset <= index) {
                    patients.add((Patient) component.getResource());
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
                        patients.add((Patient) component.getResource());
                    }
                    index++;
                    fetchMore = index < size;
                }
            }
        }

        return patients;
    }

}
