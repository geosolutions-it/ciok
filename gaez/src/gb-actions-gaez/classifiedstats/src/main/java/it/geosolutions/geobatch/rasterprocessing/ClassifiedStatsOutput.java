/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
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

import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsAction.ClassificationStatsParams;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.media.jai.classifiedstats.Result;
import org.jdom.Element;

/**
 * Allow the customization of output of the computed ClassificationStats.<br/>
 * @author ETj (etj at geo-solutions.it)
 */
public interface ClassifiedStatsOutput {

    /**
     * Parse input parameters. This element is usually nested in the ClassifiedStatsAction's params
     *
     * @param output The Element containing the output config params. Each implementation can define its own format.
     */
    void parseParams(Element output);

    /**
     * Output the computed results.
     *
     * @param params The Action's input params.
     * @param result The computed stats.
     * @throws IOException if any ex was thrown during the write procedure
     */
    void writeResults(ClassificationStatsParams params, Map<MultiKey, List<Result>> result)
            throws IOException;
}
