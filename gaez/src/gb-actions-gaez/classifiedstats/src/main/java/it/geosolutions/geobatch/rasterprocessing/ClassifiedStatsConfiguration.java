/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.geobatch.rasterprocessing;

import it.geosolutions.geobatch.catalog.Configuration;
import it.geosolutions.geobatch.configuration.event.action.ActionConfiguration;

/**
 * @author Emanuele Tajariol, GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class ClassifiedStatsConfiguration extends ActionConfiguration implements Configuration {
    public enum ComputationMode{
        IMMEDIATE, DEFERRED;
        public ComputationMode getDefault(){
            return DEFERRED;
        }
    }

    private String outDir;
    
    private ComputationMode computationMode=ComputationMode.DEFERRED;
    
	public ComputationMode getComputationMode() {
        return computationMode;
    }

    public void setComputationMode(ComputationMode computationMode) {
        this.computationMode = computationMode;
    }

    public ClassifiedStatsConfiguration(String id, String name, String description) {
        super(id, name, description);
    }

    /**
     * @return the outDir
     */
    public String getOutDir() {
        return outDir;
    }

    /**
     * @param outDir
     *            the outDir to set
     */
    public void setOutDir(final String outDir) {
        this.outDir = outDir;
    }

    @Override
    public ClassifiedStatsConfiguration clone() {
        final ClassifiedStatsConfiguration ret = (ClassifiedStatsConfiguration)super.clone();
        
        return ret;
    }
}
