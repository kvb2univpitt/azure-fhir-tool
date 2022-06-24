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
 * Jun 23, 2022 10:58:45 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
public class BasicObservationSearchResults implements Serializable {

    private static final long serialVersionUID = -8571603348070672847L;

    private int recordsTotal;

    private int recordsFiltered;

    private List<BasicObservation> basicObservations;

    public BasicObservationSearchResults() {
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

    public List<BasicObservation> getBasicObservations() {
        return basicObservations;
    }

    public void setBasicObservations(List<BasicObservation> basicObservations) {
        this.basicObservations = basicObservations;
    }

}
