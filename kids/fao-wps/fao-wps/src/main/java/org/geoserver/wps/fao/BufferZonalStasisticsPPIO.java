/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import org.geoserver.wps.ppio.XStreamPPIO;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;

public class BufferZonalStasisticsPPIO extends XStreamPPIO {

    protected BufferZonalStasisticsPPIO() {
        super(BufferZonalStatistics.class);
    }

    @Override
    protected XStream buildXStream() {
        XStream xs = new XStream() {
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new PackageStrippingMapper(next);
            };
        };
        xs.addImplicitCollection(BufferZonalStatistics.class, "statistics");
        xs.registerConverter(new DoubleConverter(xs));

        return xs;
    }

}
