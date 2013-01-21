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
package it.geosolutions.geobatch.gaez.utils.rules.ruleD;

import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class SingleClassifierLineWriter {
    private final static Logger LOGGER = LoggerFactory.getLogger(SingleClassifierLineWriter.class);

    private final static char CSV_DELIM = ',';

    public static final String OUTPUTFILE = "file";
    public static final String OUTPUTGID = "gaez_id";

    private File outputFile;
    private String gaezid;

    public SingleClassifierLineWriter() {
        this(null, null);
    }

    public SingleClassifierLineWriter(File outputFile, String gaezid) {
        this.outputFile = outputFile;
        this.gaezid = gaezid;
    }

    public void writeResults(Map<MultiKey, AggregableStats> result) throws IOException {

        if(outputFile == null)
            throw new IllegalStateException("Output file has not been set");

        FileWriter writer = null;
        try {
            if(LOGGER.isInfoEnabled())
                LOGGER.info("Writing stats to " + outputFile);
            writer = new FileWriter(outputFile);
            final Set<MultiKey> set = result.keySet();

            // write header
            writer.write(HEADER_LONG_STATS);

            // write lines
            for (MultiKey mkey : set) {
                AggregableStats r = result.get(mkey);
                if (r!=null){
                	String line = getLongStatsLine(mkey, r);
                	writer.write(line);
                	writer.write("\n");
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Error writing to " + outputFile + " : " + ex.getMessage());
            throw new IOException("Error writing to outputfile", ex);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        
    }

    private static final String HEADER_LONG_STATS = "GAEZ_ID,GAUL_CODE,COUNT,MIN,MAX,RANGE,MEAN,STD,SUM\n";
    /**
     * Write a CSV line according to the input, following GAEZ specifications
     * @param key
     * @param results
     * @param globalMinMax
     * @return
     */
    private String getLongStatsLine(MultiKey key, AggregableStats results) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(gaezid).append(CSV_DELIM);
        sb.append(key.getKey(0)).append(CSV_DELIM); // we assume that the classifier layers have
                                                    // been passed in the proper order

        double min = results.getMin();
        double max = results.getMax();
        double rng = results.getMax() - results.getMin();
        double avg = results.getMean();
        double std = Math.sqrt(results.getVariance());
        double sum = results.getSum();
        long cnt = results.getSize();

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

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }


}
