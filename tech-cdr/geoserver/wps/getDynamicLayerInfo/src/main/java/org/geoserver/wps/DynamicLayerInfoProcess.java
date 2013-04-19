/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;

/**
 * A GeoServer WPS Process Calculate the dynamic parameters needed to render a dynamic style and returns a digest representing the required
 * combination of DynamicStyle and ViewParams
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 */
@DescribeProcess(title = "GetDynamicLayerInfo", version = "1.0", description = "Calculate the dynamic parameters "
        + "needed to render a dynamic style and returns a digest representing the required combination of DynamicStyle and ViewParams")
public class DynamicLayerInfoProcess implements GSProcess {
    @DescribeResult(name = "result", description = "The collection of result polygons")
    public String execute(
            @DescribeParameter(name = "input", description = "The json to be split") String input) {
        return "The input is: " + input; // TODO implement here
    }
}
