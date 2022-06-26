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

import edu.pitt.dbmi.azure.fhir.tool.model.BasicEncounter;
import edu.pitt.dbmi.azure.fhir.tool.model.BasicEncounterSearchResults;
import edu.pitt.dbmi.azure.fhir.tool.service.ResourceCountService;
import edu.pitt.dbmi.azure.fhir.tool.service.fhir.EncounterResourceService;
import edu.pitt.dbmi.azure.fhir.tool.utils.DateFormatters;
import edu.pitt.dbmi.azure.fhir.tool.utils.FileStorage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * May 29, 2022 1:08:49 AM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@RestController
@RequestMapping("/fhir/api/encounter")
public class EncounterRestController {

    private final EncounterResourceService encounterResourceService;
    private final ResourceCountService resourceCountService;

    @Autowired
    public EncounterRestController(EncounterResourceService encounterResourceService, ResourceCountService resourceCountService) {
        this.encounterResourceService = encounterResourceService;
        this.resourceCountService = resourceCountService;
    }
    
    @PostMapping(value = "load", produces = MediaType.APPLICATION_JSON_VALUE)
    public void uploadEncounters(@RequestParam("file") MultipartFile file) throws IOException {
        FileStorage.clearAll();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            FileStorage.add(file.getOriginalFilename(), reader.lines().skip(1).collect(Collectors.toList()));
        }
    }

    @GetMapping(value = "file", produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicEncounterSearchResults getEncountersFromFile(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authClient,
            @RequestParam("start") Optional<Integer> start,
            @RequestParam("length") Optional<Integer> length,
            @RequestParam("file") Optional<String> file) {
        int counts = 0;
        List<Encounter> encounters;
        if (start.isPresent() && length.isPresent() && file.isPresent()) {
            List<String> lines = FileStorage.get(file.get());
            encounters = encounterResourceService.getEncounters(lines, start.get(), length.get());
            counts = lines.size();
        } else {
            encounters = Collections.EMPTY_LIST;
        }

        BasicEncounterSearchResults results = new BasicEncounterSearchResults();
        results.setBasicEncounters(encounters.stream().map(e -> toBasicEncounter(e)).collect(Collectors.toList()));
        results.setRecordsTotal(counts);
        results.setRecordsFiltered(counts);

        return results;
    }

    @GetMapping(value = "azure", produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicEncounterSearchResults getBasicEncounters(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authClient,
            @RequestParam("start") Optional<Integer> start,
            @RequestParam("length") Optional<Integer> length) {
        List<Encounter> encounters = (start.isPresent() && length.isPresent())
                ? encounterResourceService.getEncounters(authClient.getAccessToken(), start.get(), length.get())
                : encounterResourceService.getEncounters(authClient.getAccessToken());
        int counts = resourceCountService.getEncounterCounts(authClient.getAccessToken());

        BasicEncounterSearchResults results = new BasicEncounterSearchResults();
        results.setBasicEncounters(encounters.stream().map(e -> toBasicEncounter(e)).collect(Collectors.toList()));
        results.setRecordsTotal(counts);
        results.setRecordsFiltered(counts);

        return results;
    }

    private BasicEncounter toBasicEncounter(Encounter encounter) {
        List<String> types = new LinkedList<>();
        encounter.getType()
                .forEach(type -> {
                    type.getCoding()
                            .forEach(coding -> types.add(coding.getDisplay()));
                });

        BasicEncounter basicEncounter = new BasicEncounter();
        basicEncounter.setId(encounter.getIdElement().getIdPart());
        basicEncounter.setEnd(DateFormatters.formatToMonthDayYearHourMinute(encounter.getPeriod().getEnd()));
        basicEncounter.setStart(DateFormatters.formatToMonthDayYearHourMinute(encounter.getPeriod().getStart()));
        basicEncounter.setStatus(encounter.getStatus().getDisplay());
        basicEncounter.setType(types.stream().collect(Collectors.joining(", ")));

        return basicEncounter;
    }

}
