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
package edu.pitt.dbmi.azure.fhir.tool.conf;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.pitt.dbmi.azure.fhir.tool.utils.ResourceHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Jun 11, 2022 4:02:28 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
@Configuration
public class AppConfig {

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = FhirContext.forR4();
        fhirContext.getRestfulClientFactory().setSocketTimeout(200 * 1000);

        return fhirContext;
    }

    @Bean
    public IParser jsonParser(FhirContext fhirContext) {
        return fhirContext.newJsonParser();
    }

    @Bean
    public ResourceHelper resourceHelper() {
        return new ResourceHelper();
    }

}
