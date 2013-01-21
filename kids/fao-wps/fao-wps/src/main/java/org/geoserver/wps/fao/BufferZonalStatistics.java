/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.util.List;

public class BufferZonalStatistics {
    List<BufferZonalStatistic> statistics;
    
    public BufferZonalStatistics(List<BufferZonalStatistic> stats) {
        this.statistics = stats;
    }

    
}
