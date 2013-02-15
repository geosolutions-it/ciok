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
public class ShortLineWriter implements LineWriter {
    private final static Logger LOGGER = LoggerFactory.getLogger(ShortLineWriter.class);

    private final static char CSV_DELIM = ',';

    private String gaezid;

    private ShortLineWriter() {
        this(null);
    }

    public ShortLineWriter(String gaezid) {
        this.gaezid = gaezid;
    }

    

    private static final String HEADER_SHORT_STATS = "GAEZ_ID,GAUL_CODE,CATEGORY_CODE,CATEGORY_DESCRIPTION,COUNT,AREA\n";
    
    public String writeHeader(){
    	return HEADER_SHORT_STATS;
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
        String catCode;
        String catDesc;

//        if(categories=null) {
            catCode = "-1";
            catDesc = "";
//        } else {
//            catCode = String.valueOf(key.getKey(1));
//            catDesc = categories.get(catCode);
//
//            cp.addClassificationKeys(key);
//        }

        catDesc = catDesc == null ?
                    "UNKNOWN" :
                    escapeCsvField(catDesc, CSV_DELIM);

        sb.append(catCode).append(CSV_DELIM);
        sb.append(catDesc).append(CSV_DELIM);
//

        double sum = results.getSum();
        long cnt = results.getSize();

        sb.append(cnt).append(CSV_DELIM);
        appendOrSkip(sb, sum);

        return sb.toString();
    }

//    private String getEmptyClassifiedStatsLine(CartesianProduct.Pair<String, String> pair ) {
//        StringBuilder sb = new StringBuilder(100);
//
//        sb.append(gaezid).append(CSV_DELIM);
//        sb.append(pair.getVal0()).append(CSV_DELIM);
//        
//        String catDesc = categories.get(pair.getVal1());
//        catDesc = catDesc == null ?
//                    "UNKNOWN" :
//                    escapeCsvField(catDesc, CSV_DELIM);
//        
//        sb.append(pair.getVal1()).append(CSV_DELIM);
//        sb.append(catDesc).append(CSV_DELIM);
//        sb.append(0l).append(CSV_DELIM);
//        sb.append(0d);
//
//        return sb.toString();
//    }

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
