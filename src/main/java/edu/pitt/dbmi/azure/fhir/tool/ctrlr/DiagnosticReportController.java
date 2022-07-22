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
import edu.pitt.dbmi.azure.fhir.tool.service.fhir.DiagnosticReportResourceService;
import edu.pitt.dbmi.azure.fhir.tool.utils.FileStorage;
import java.text.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * Jul 21, 2022 3:44:34 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
@Controller
public class DiagnosticReportController {

    private final DiagnosticReportResourceService diagnosticReportResourceService;
    private final IParser jsonParser;

    @Autowired
    public DiagnosticReportController(DiagnosticReportResourceService diagnosticReportResourceService, IParser jsonParser) {
        this.diagnosticReportResourceService = diagnosticReportResourceService;
        this.jsonParser = jsonParser;
    }

    @PostMapping(value = "/fhir/diagnostic-reports/delete")
    public String deleteAll(@RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authzClient) throws ParseException {
        diagnosticReportResourceService.deleteDiagnosticReports(authzClient.getAccessToken());

        return "redirect:/fhir/diagnostic-reports";
    }

    @PostMapping(value = "/fhir/diagnostic-reports/upload")
    public String upload(
            @RequestParam("filename") String fileName,
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authzClient) throws ParseException {
        diagnosticReportResourceService.uploadDiagnosticReports(FileStorage.get(fileName), authzClient.getAccessToken());

        return "redirect:/fhir/diagnostic-reports";
    }

    @GetMapping("/fhir/diagnostic-reports")
    public String showDiagnosticReportResourceListPage(
            @RegisteredOAuth2AuthorizedClient("azure") final OAuth2AuthorizedClient authorizedClient,
            final Model model) {
        model.addAttribute("authenName", authorizedClient.getPrincipalName());
        model.addAttribute("diagnosticReport", true);

        FileStorage.clearAll();

        return "fhir/diagnostic_reports";
    }

}
