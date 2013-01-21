/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.util.List;

/**
 * Bean collecting all the cropped raster informations
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CroppedRasters {
    List<CroppedRaster> rasters;

    public CroppedRasters(List<CroppedRaster> rasters) {
        this.rasters = rasters;
    }
}