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
package edu.pitt.dbmi.azure.fhir.tool.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

/**
 *
 * Jun 11, 2022 8:55:17 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Service
public class ResourceCountService {

    private final String fhirUrl;
    private final FhirContext fhirContext;

    @Autowired
    public ResourceCountService(
            @Value("${fhir.url}") String fhirUrl,
            FhirContext fhirContext) {
        this.fhirUrl = fhirUrl;
        this.fhirContext = fhirContext;
    }

    private IGenericClient getClient(OAuth2AccessToken accessToken) {
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirUrl);
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken.getTokenValue()));

        return client;
    }

    public int getPatientCounts(OAuth2AccessToken accessToken) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(Patient.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .summaryMode(SummaryEnum.COUNT)
                .execute();

        return bundle.getTotal();
    }

    public int getEncounterCounts(OAuth2AccessToken accessToken) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(Encounter.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .summaryMode(SummaryEnum.COUNT)
                .execute();

        return bundle.getTotal();
    }

    public int getObservationCounts(OAuth2AccessToken accessToken) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(Observation.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .summaryMode(SummaryEnum.COUNT)
                .execute();

        return bundle.getTotal();
    }

}
