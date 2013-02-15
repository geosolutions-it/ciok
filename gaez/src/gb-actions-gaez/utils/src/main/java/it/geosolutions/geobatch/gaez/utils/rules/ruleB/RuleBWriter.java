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
package it.geosolutions.geobatch.gaez.utils.rules.ruleB;

import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;
import it.geosolutions.geobatch.gaez.utils.rules.LineWriter;
import it.geosolutions.geobatch.gaez.utils.rules.LongLineWriter;
import it.geosolutions.geobatch.gaez.utils.rules.ShortLineWriter;

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
 * A ClassifiedStatsOutput implementation for the csv files needed by GAEZ.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class RuleBWriter {
    private final static Logger LOGGER = LoggerFactory.getLogger(RuleBWriter.class);

    public enum LineType {
    	LONG,
    	SHORT
    }

    private File outputFile;
    private LineWriter lineWriter;

    private RuleBWriter() {
        
    }

    public RuleBWriter(final LineType lineType, final File outputFile, String gaezid) {
        this.outputFile = outputFile;

        switch (lineType){
        	case LONG:
        		lineWriter=new LongLineWriter(gaezid);
        		break;
        	case SHORT:
        		lineWriter=new ShortLineWriter(gaezid);
        		break;	
        }
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
            writer.write(lineWriter.writeHeader());

            // write lines
            for (MultiKey mkey : set) {
                AggregableStats r = result.get(mkey);
                String line = lineWriter.writeLine(mkey, r);
                writer.write(line);
                writer.write("\n");
            }
        } catch (IOException ex) {
        	if (LOGGER.isErrorEnabled())
        		LOGGER.error("Error writing to " + outputFile + " : " + ex.getMessage());
            throw new IOException("Error writing to outputfile", ex);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        
    }
    
//    public void writeResults(Map<MultiKey, List<Result>> result) throws IOException {
//
//        if(outputFile == null)
//            throw new IllegalStateException("Output file has not been set");
//
//        FileWriter writer = null;
//        try {
//            if(LOGGER.isInfoEnabled())
//                LOGGER.info("Writing stats to " + outputFile);
//            writer = new FileWriter(outputFile);
//            final Set<MultiKey> set = result.keySet();
//
//            // write header
//            writer.write(lineWriter.writeHeader());
//
//            // write lines
//            for (MultiKey mkey : set) {
//                List<Result> r = result.get(mkey);
//                String line = lineWriter.writeLine(mkey, r);
//                writer.write(line);
//                writer.write("\n");
//            }
//        } catch (IOException ex) {
//            LOGGER.error("Error writing to " + outputFile + " : " + ex.getMessage());
//            throw new IOException("Error writing to outputfile", ex);
//        } finally {
//            IOUtils.closeQuietly(writer);
//        }
//        
//    }

//    public void setGaezid(String gaezid) {
//        this.gaezid = gaezid;
//    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }


}
