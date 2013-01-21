/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import org.geoserver.wps.ppio.XStreamPPIO;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

public class ZonalStasisticsPPIO extends XStreamPPIO {

    protected ZonalStasisticsPPIO() {
        super(ZonalStatistics.class);
    }

    @Override
    protected XStream buildXStream() {
        XStream xs = new XStream() {
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new PackageStrippingMapper(next) {
                    @Override
                    public String serializedClass(Class type) {
                        if(type == Statistics.class) {
                            return "stats";
                        }
                        return super.serializedClass(type);
                    }
                };
            };
        };
        xs.addImplicitCollection(ZonalStatistics.class, "statistics");
        xs.registerLocalConverter(ZonalStatistic.class, "feature", new FlatMapConverter(xs));
        xs.registerConverter(new DoubleConverter(xs));
        xs.addImplicitCollection(ZonalStatistic.class, "stats");
        xs.useAttributeFor(Statistics.class, "rangeAttribute");
        xs.aliasField("range", Statistics.class, "rangeAttribute");

        return xs;
    }

}
