/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import org.geoserver.wps.jts.SpringBeanProcessFactory;

/**
 * Factory for FAO specific processes
 * @author Andrea Aime - OpenGeo
 */
public class FAOProcessFactory extends SpringBeanProcessFactory {

    public FAOProcessFactory() {
        super("FAO custom processes", "fao", FAOProcess.class);
    }
}
