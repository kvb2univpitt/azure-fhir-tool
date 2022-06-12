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
import edu.pitt.dbmi.azure.fhir.tool.service.ResourceCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * Jun 11, 2022 4:04:25 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
@Controller
public class ApplicationController {

    private final ResourceCountService resourceCountService;
    private final IParser jsonParser;

    @Autowired
    public ApplicationController(ResourceCountService resourceCountService, IParser jsonParser) {
        this.resourceCountService = resourceCountService;
        this.jsonParser = jsonParser;
    }

    @GetMapping("/")
    public String showIndexPage(final Authentication authen) {
        return (authen == null)
                ? "login"
                : "redirect:/fhir";
    }

    @GetMapping("/fhir")
    public String showMainResourcePage(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authClient,
            final Model model) {
        model.addAttribute("authenName", authClient.getPrincipalName());
        model.addAttribute("patientCounts", resourceCountService.getPatientCounts(authClient.getAccessToken()));
        model.addAttribute("encounterCounts", resourceCountService.getEncounterCounts(authClient.getAccessToken()));
        model.addAttribute("observationCounts", resourceCountService.getObservationCounts(authClient.getAccessToken()));

        return "fhir/dashboard";
    }

}
