/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import org.opengis.feature.simple.SimpleFeature;

public class Statistics {
    public Statistics(SimpleFeature f) {
        max = (Double) f.getAttribute("max");
        min = (Double) f.getAttribute("min");
        if (max != null && min != null) {
            range = max - min;
        }
        mean = (Double) f.getAttribute("avg");
        std = (Double) f.getAttribute("stddev");
        sum = (Double) f.getAttribute("sum");
        count = (Long) f.getAttribute("count");
    }

    Double max;

    Double min;

    Double range;

    Double mean;

    Double std;

    Double sum;

    Long count;
    
    String rangeAttribute;
}
