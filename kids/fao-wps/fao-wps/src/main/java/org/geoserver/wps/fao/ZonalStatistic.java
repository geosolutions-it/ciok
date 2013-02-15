/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ZonalStatistic {
    String areaLayer;

    LinkedHashMap<String, String> feature;

    String datalayer;

    String datafield;

    ArrayList<Statistics> stats = new ArrayList<Statistics>();
}
