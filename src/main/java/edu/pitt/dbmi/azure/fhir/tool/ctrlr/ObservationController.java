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
package edu.pitt.dbmi.azure.fhir.tool.ctrlr;

import ca.uhn.fhir.parser.IParser;
import edu.pitt.dbmi.azure.fhir.tool.service.fhir.ObservationResourceService;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 * May 29, 2022 12:54:50 AM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Controller
public class ObservationController {

    private final ObservationResourceService observationResourceService;
    private final IParser jsonParser;

    @Autowired
    public ObservationController(ObservationResourceService observationResourceService, IParser jsonParser) {
        this.observationResourceService = observationResourceService;
        this.jsonParser = jsonParser;
    }

    @GetMapping("/fhir/Observation/{id}")
    public String showPatientResourceLPage(
            @PathVariable final String id,
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authorizedClient,
            final Model model) {
        Observation observation = observationResourceService.getObservation(authorizedClient.getAccessToken(), id);

        model.addAttribute("authenName", authorizedClient.getPrincipalName());
        model.addAttribute("observation", observation);
        model.addAttribute("json", jsonParser.encodeResourceToString(observation));

        return "fhir/observation";
    }

    @GetMapping("/fhir/observations")
    public String showObservationResourceListPage(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authorizedClient,
            final Model model) {
        model.addAttribute("authenName", authorizedClient.getPrincipalName());
        model.addAttribute("observation", true);

        return "fhir/observations";
    }

}
