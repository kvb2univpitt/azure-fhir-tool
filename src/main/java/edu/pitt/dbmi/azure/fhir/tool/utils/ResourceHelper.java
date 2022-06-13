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
package edu.pitt.dbmi.azure.fhir.tool.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pitt.dbmi.azure.fhir.tool.model.Code;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

/**
 *
 * Jun 12, 2022 2:15:10 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
public final class ResourceHelper {

    private final ObjectMapper objectMapper;

    public ResourceHelper() {
        this.objectMapper = new ObjectMapper();
    }

    public String getExtensionValue(Type type) {
        if (type instanceof StringType stringType) {
            return stringType.getValue();
        } else if (type instanceof CodeType codeType) {
            return codeType.getValue();
        } else if (type instanceof DecimalType decimalType) {
            return decimalType.getValueAsString();
        } else if (type instanceof Address address) {
            StringBuilder sb = new StringBuilder("Address [");
            sb.append(String.format("City=%s, ", address.getCity()));
            sb.append(String.format("State-%s, ", address.getState()));
            sb.append(String.format("Country=%s", address.getCountry()));
            sb.append("]");

            return sb.toString();
        } else {
            return "";
        }
    }

    public String getPatientAddressLine(List<StringType> lines) {
        return lines.stream()
                .map(line -> line.getValueAsString())
                .collect(Collectors.joining(" "));
    }

    public String getCodingAsJson(List<Coding> codings) {
        List<Code> codes = codings.stream()
                .map(code -> new Code(code.getSystem(), code.getCode(), code.getDisplay()))
                .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(codes);
        } catch (JsonProcessingException exception) {
            return exception.getLocalizedMessage();
        }
    }

    public String getPatientOfficialName(Patient patient) {
        return patient.getName().stream()
                .filter(name -> name.getUse() == HumanName.NameUse.OFFICIAL)
                .map(name -> String.format("%s %s", name.getGivenAsSingleString(), name.getFamily()))
                .collect(Collectors.joining(", "))
                .trim();
    }

}
