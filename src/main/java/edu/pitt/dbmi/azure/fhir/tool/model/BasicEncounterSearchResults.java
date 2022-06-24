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
package edu.pitt.dbmi.azure.fhir.tool.model;

import java.io.Serializable;
import java.util.List;

/**
 *
 * Jun 24, 2022 12:01:13 AM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
public class BasicEncounterSearchResults implements Serializable {

    private static final long serialVersionUID = -8581126371448512547L;

    private int recordsTotal;

    private int recordsFiltered;

    private List<BasicEncounter> basicEncounters;

    public BasicEncounterSearchResults() {
    }

    public int getRecordsTotal() {
        return recordsTotal;
    }

    public void setRecordsTotal(int recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public int getRecordsFiltered() {
        return recordsFiltered;
    }

    public void setRecordsFiltered(int recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public List<BasicEncounter> getBasicEncounters() {
        return basicEncounters;
    }

    public void setBasicEncounters(List<BasicEncounter> basicEncounters) {
        this.basicEncounters = basicEncounters;
    }

}
