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
import edu.pitt.dbmi.fhir.resource.mapper.r4.brainai.PatientResourceMapper;
import edu.pitt.dbmi.fhir.resource.mapper.util.Delimiters;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    public List<Patient> getPatients(List<String> lines, int start, int length) {
        List<Patient> patients = new LinkedList<>();

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
                    patients.add(PatientResourceMapper.getPatient(delimiter.split(line)));
                } catch (ParseException exception) {
                    exception.printStackTrace(System.err);
                }
            }
            index++;
        }

        return patients;
    }

    public List<Patient> getPatients(MultipartFile multipartFile, int start, int length) {
        if (multipartFile == null) {
            return Collections.EMPTY_LIST;
        }

        List<Patient> patients = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()))) {
            reader.readLine();

            Pattern delimiter = Delimiters.TAB_DELIM;
            int size = start + length;
            int offset = start;
            int index = 0;
            for (String line = reader.readLine(); line != null && index < size; line = reader.readLine()) {
                if (offset <= index) {
                    patients.add(PatientResourceMapper.getPatient(delimiter.split(line)));
                }
                index++;
            }
        } catch (IOException | ParseException exception) {
            exception.printStackTrace(System.err);
        }

        return patients;
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
