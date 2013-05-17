/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wps;

import org.fao.data.map.geoserver.layer.dynamic.DynamicLayer;
import org.fao.data.map.geoserver.layer.dynamic.GetDynamicLayerInfoResponse;
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
            @DescribeParameter(name = "Layer", description = "The complete layer name in the form NAMESPACE:LAYERNAME") String layer,
            @DescribeParameter(name = "QueryMDX", description = "The QueryMDX as json") String queryMDX,
            @DescribeParameter(name = "DynamicStyle", description = "The DynamicStyle as json") String dynamicStyle

    ) {

        final GetDynamicLayerInfoResponse response = DynamicLayer.getDynamicLayerInfo(layer, queryMDX,
                dynamicStyle);

        return String
                .format("{\"RunTime\":\"%s\",\"Error\":\"%s\",\"QueryId\":\"%s\",\"DynamicStyleId\":\"%s\"}",
                        response.getRunTime(),
                        response.getError() == null ? "null" : response.getError(),
                        response.getQueryId(), response.getDynamicStyleId());

    }
}
