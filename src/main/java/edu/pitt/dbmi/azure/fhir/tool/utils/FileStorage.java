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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Jun 25, 2022 4:46:31 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
public final class FileStorage {

    private static final Map<String, List<String>> storage = new HashMap<>();

    private FileStorage() {
    }

    public static List<String> get(String fileName) {
        return storage.containsKey(fileName)
                ? storage.get(fileName)
                : Collections.EMPTY_LIST;
    }

    public static void add(String fileName, List<String> lines) {
        storage.put(fileName, lines);
    }

    public static void clearAll() {
        storage.clear();
    }

    public static int size() {
        return storage.size();
    }

}
