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
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.util.BundleUtil;
import java.util.LinkedList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 *
 * Jun 12, 2022 1:26:42 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
public abstract class AbstractResourceService {

    protected final String fhirUrl;
    protected final FhirContext fhirContext;

    public AbstractResourceService(String fhirUrl, FhirContext fhirContext) {
        this.fhirUrl = fhirUrl;
        this.fhirContext = fhirContext;
    }
    
    public Bundle findEncounter(Reference subject, IGenericClient client) {
        return client
                .search()
                .forResource(Encounter.class)
                .where(Patient.IDENTIFIER.exactly().systemAndValues("urn:oid:2.16.840.1.113883.3.552", subject.getReference()))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();
    }

    public Bundle findPatient(Reference subject, IGenericClient client) {
        return client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndValues("urn:oid:2.16.840.1.113883.3.552", subject.getReference()))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();
    }

    protected void deleteResources(Bundle searchBundle, int batchSize, IGenericClient client) {
        List<Bundle.BundleEntryComponent> entries = new LinkedList<>();
        searchBundle.getEntry().forEach(entries::add);

        while (searchBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            searchBundle = client
                    .loadPage()
                    .next(searchBundle)
                    .execute();

            searchBundle.getEntry()
                    .forEach(entry -> {
                        if (entries.size() == batchSize) {
                            deleteResources(entries, client);
                            entries.clear();
                        }

                        entries.add(entry);
                    });
        }

        deleteResources(entries, client);
        entries.clear();
    }

    private void deleteResources(List<Bundle.BundleEntryComponent> entries, IGenericClient client) {
        if (entries.isEmpty()) {
            return;
        }

        Bundle deleteBundle = new Bundle();
        deleteBundle.setType(Bundle.BundleType.BATCH);

        entries.forEach(e -> deleteBundle
                .addEntry()
                .getRequest().setUrl(e.getFullUrl())
                .setMethod(Bundle.HTTPVerb.DELETE));

        client.transaction().withBundle(deleteBundle).execute();
    }

    protected Bundle addResources(List<Resource> resources, String url, IGenericClient client) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        resources.forEach(resource -> bundle.addEntry()
                .setResource(resource)
                .getRequest()
                .setUrl(url)
                .setMethod(Bundle.HTTPVerb.POST));

        return client.transaction().withBundle(bundle).execute();
    }

    /**
     * Fetch all resources.
     *
     * @param client
     * @param bundle
     * @return a list of all resources
     */
    protected List<IBaseResource> fetchAllResources(IGenericClient client, Bundle bundle) {
        List<IBaseResource> resources = new LinkedList<>();

        // get the first set of resources
        BundleUtil.toListOfResources(fhirContext, bundle).forEach(resources::add);

        // get the rest of the patients
        while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
            // fetch more resources
            bundle = client.loadPage().next(bundle).execute();

            // add more resources
            BundleUtil.toListOfResources(fhirContext, bundle).forEach(resources::add);
        }

        return resources;
    }

    protected IGenericClient getClient(OAuth2AccessToken accessToken) {
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirUrl);
        client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken.getTokenValue()));

        return client;
    }

}
