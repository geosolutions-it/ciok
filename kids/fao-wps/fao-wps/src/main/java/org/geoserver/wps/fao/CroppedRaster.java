/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.net.URL;

import org.geoserver.catalog.CoverageInfo;
import org.geotools.styling.Style;

/**
 * Collects informations about the various raster
 */
public class CroppedRaster {
    String layerName;

    transient CoverageInfo coverage;

    transient Style style;

    String requestedStyle;

    String exception;

    URL fileUrl;

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public URL getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(URL location) {
        this.fileUrl = location;
    }

    public void setCoverage(CoverageInfo coverageInfo) {
        this.coverage = coverageInfo;
    }

    public CoverageInfo getCoverage() {
        return coverage;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public String getRequestedStyle() {
        return requestedStyle;
    }

    public void setRequestedStyle(String styleName) {
        this.requestedStyle = styleName;
    }

}