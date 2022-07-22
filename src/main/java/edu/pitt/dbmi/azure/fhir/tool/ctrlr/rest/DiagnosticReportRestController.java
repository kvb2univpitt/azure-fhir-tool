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

import edu.pitt.dbmi.azure.fhir.tool.model.BasicDiagnosticReport;
import edu.pitt.dbmi.azure.fhir.tool.model.BasicDiagnosticReportSearchResults;
import edu.pitt.dbmi.azure.fhir.tool.service.ResourceCountService;
import edu.pitt.dbmi.azure.fhir.tool.service.fhir.DiagnosticReportResourceService;
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
import org.hl7.fhir.r4.model.DiagnosticReport;
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
 * Jul 21, 2022 2:28:11 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@RestController
@RequestMapping("/fhir/api/diagnostic-report")
public class DiagnosticReportRestController {

    private final DiagnosticReportResourceService diagnosticReportResourceService;
    private final ResourceCountService resourceCountService;

    @Autowired
    public DiagnosticReportRestController(DiagnosticReportResourceService diagnosticReportResourceService, ResourceCountService resourceCountService) {
        this.diagnosticReportResourceService = diagnosticReportResourceService;
        this.resourceCountService = resourceCountService;
    }

    @PostMapping(value = "load", produces = MediaType.APPLICATION_JSON_VALUE)
    public void uploadDiagnosticReports(@RequestParam("file") MultipartFile file) throws IOException {
        FileStorage.clearAll();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            FileStorage.add(file.getOriginalFilename(), reader.lines().skip(1).collect(Collectors.toList()));
        }
    }

    @GetMapping(value = "file", produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicDiagnosticReportSearchResults getDiagnosticReportsFromFile(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authClient,
            @RequestParam("start") Optional<Integer> start,
            @RequestParam("length") Optional<Integer> length,
            @RequestParam("file") Optional<String> file) {
        int counts = 0;
        List<DiagnosticReport> diagnosticReports;
        if (start.isPresent() && length.isPresent() && file.isPresent()) {
            List<String> lines = FileStorage.get(file.get());
            diagnosticReports = diagnosticReportResourceService.getDiagnosticReports(lines, start.get(), length.get());
            counts = lines.size();
        } else {
            diagnosticReports = Collections.EMPTY_LIST;
        }

        BasicDiagnosticReportSearchResults results = new BasicDiagnosticReportSearchResults();
        results.setBasicDiagnosticReports(diagnosticReports.stream().map(e -> toBasicDiagnosticReport(e)).collect(Collectors.toList()));
        results.setRecordsTotal(counts);
        results.setRecordsFiltered(counts);

        return results;
    }

    @GetMapping(value = "azure", produces = MediaType.APPLICATION_JSON_VALUE)
    public BasicDiagnosticReportSearchResults getBasicDiagnosticReports(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authClient,
            @RequestParam("start") Optional<Integer> start,
            @RequestParam("length") Optional<Integer> length) {
        List<DiagnosticReport> diagnosticReports = (start.isPresent() && length.isPresent())
                ? diagnosticReportResourceService.getDiagnosticReports(authClient.getAccessToken(), start.get(), length.get())
                : diagnosticReportResourceService.getDiagnosticReports(authClient.getAccessToken());
        int counts = resourceCountService.getDiagnosticReportCounts(authClient.getAccessToken());

        BasicDiagnosticReportSearchResults results = new BasicDiagnosticReportSearchResults();
        results.setBasicDiagnosticReports(diagnosticReports.stream().map(e -> toBasicDiagnosticReport(e)).collect(Collectors.toList()));
        results.setRecordsTotal(counts);
        results.setRecordsFiltered(counts);

        return results;
    }

    private BasicDiagnosticReport toBasicDiagnosticReport(DiagnosticReport diagnosticReport) {
        List<String> categories = new LinkedList<>();
        diagnosticReport.getCategory().forEach(type -> {
            type.getCoding().forEach(coding -> {
                categories.add(coding.getDisplay());
            });
        });

        BasicDiagnosticReport basicDiagnosticReport = new BasicDiagnosticReport();
        basicDiagnosticReport.setId(diagnosticReport.getIdElement().getIdPart());
        basicDiagnosticReport.setStatus(diagnosticReport.getStatus().getDisplay());
        basicDiagnosticReport.setCategory(categories.stream().collect(Collectors.joining(", ")));
        basicDiagnosticReport.setEffectiveDate(DateFormatters.formatToMonthDayYearHourMinute(diagnosticReport.getEffectiveDateTimeType().getValue()));

        return basicDiagnosticReport;
    }

}
