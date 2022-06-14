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
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

/**
 *
 * May 28, 2022 8:07:35 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Service
public class ObservationResourceService extends AbstractResourceService {

    @Autowired
    public ObservationResourceService(@Value("${fhir.url}") String fhirUrl, FhirContext fhirContext) {
        super(fhirUrl, fhirContext);
    }

    public Observation getObservation(final OAuth2AccessToken accessToken, final String id) {
        Bundle bundle = getClient(accessToken)
                .search()
                .forResource(Observation.class)
                .where(Observation.RES_ID.exactly().identifier(id))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return bundle.getEntry().stream()
                .map(e -> (Observation) e.getResource())
                .findFirst()
                .orElse(null);
    }

    public List<Observation> getObservations(OAuth2AccessToken accessToken) {
        IGenericClient client = getClient(accessToken);

        Bundle bundle = client
                .search()
                .forResource(Observation.class)
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();

        return fetchAllResources(client, bundle).stream()
                .map(e -> (Observation) e)
                .collect(Collectors.toList());
    }

}
