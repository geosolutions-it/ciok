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
package it.geosolutions.geobatch.gaez.utils.rules;

import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class LongLineWriter implements LineWriter {
    private final static Logger LOGGER = LoggerFactory.getLogger(LongLineWriter.class);

    private final static char CSV_DELIM = ',';

    private String gaezid;

    private LongLineWriter() {
        this(null);
    }

    public LongLineWriter(String gaezid) {
        this.gaezid = gaezid;
    }

    private static final String HEADER_LONG_STATS = "GAEZ_ID,GAUL_CODE,COUNT,MIN,MAX,RANGE,MEAN,STD,SUM\n";
    
    public String writeHeader(){
    	return HEADER_LONG_STATS;
    }
    
    /**
     * Write a CSV line according to the input, following GAEZ specifications
     * @param key
     * @param results
     * @param globalMinMax
     * @return
     */
    public String writeLine(MultiKey key, AggregableStats results) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(gaezid).append(CSV_DELIM);
        sb.append(key.getKey(0)).append(CSV_DELIM); // we assume that the classifier layers have
                                                    // been passed in the proper order

        double min = results.getMin();
        double max = results.getMax();
        double rng = max - min;
        double avg = results.getMean();
        double std = Math.sqrt(results.getVariance());
        double sum = results.getSum();
        long cnt = results.getSize();
//        GAEZ_ID,GAUL_CODE,COUNT,MIN,MAX,RANGE,MEAN,STD,SUM
        sb.append(cnt).append(CSV_DELIM);
        appendOrSkip(sb, min).append(CSV_DELIM);
        appendOrSkip(sb, max).append(CSV_DELIM);
        appendOrSkip(sb, rng).append(CSV_DELIM);
        appendOrSkip(sb, avg).append(CSV_DELIM);
        appendOrSkip(sb, std).append(CSV_DELIM);
        appendOrSkip(sb, sum);

        return sb.toString();
    }

    private StringBuilder appendOrSkip(StringBuilder sb, double d) {
        if(  ! Double.isNaN(d) && ! Double.isInfinite(d) )
            sb.append(d);
        return sb;
    }


    /**
     * Encodes according to rfc4180
     */
    private String escapeCsvField(String field, char delim) {
        if(field.indexOf(delim) != -1 || field.indexOf('"') != -1 ) {
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        } else
            return field;
    }

    public void setGaezid(String gaezid) {
        this.gaezid = gaezid;
    }


}
