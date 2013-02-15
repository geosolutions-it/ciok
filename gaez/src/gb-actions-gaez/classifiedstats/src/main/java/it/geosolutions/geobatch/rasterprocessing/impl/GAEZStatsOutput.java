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
package it.geosolutions.geobatch.rasterprocessing.impl;

import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsAction;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsOutput;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jaitools.media.jai.classifiedstats.Result;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A ClassifiedStatsOutput implementation for the csv files needed by GAEZ.
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class GAEZStatsOutput implements ClassifiedStatsOutput {
    private final static Logger LOGGER = LoggerFactory.getLogger(GAEZStatsOutput.class);

    private final static char CSV_DELIM = ',';

    public static final String OUTPUTFILE = "file";
    public static final String OUTPUTGID = "gaez_id";

    public static final String CATEGORIES = "categories";
    public static final String CATEGORY = "category";
    public static final String CATCODE = "code";
    public static final String CATDESC = "description";

    private File outputFile = null;
    private String gaezid = null;

    private Map<String, String> categories = new HashMap<String, String>();

    public void parseParams(Element outputElem) {
        // parse output file
        {
            String outputFilename = outputElem.getChildText(OUTPUTFILE);
            File outputFile = new File(outputFilename);
            String outputParentName = FilenameUtils.getPath(outputFilename);
            File outputDir = new File(outputParentName);
            // if( ! outputDir.canWrite())
            // throw new ActionException(this, "Can't write to output dir " + outputParentName);
            this.outputFile = outputFile;
        }

        // parse output params
        this.gaezid = outputElem.getChildText(OUTPUTGID);

        // parse categories
        Element categoriesE = outputElem.getChild(CATEGORIES);
        if(categoriesE != null) {
            for (Element catE : (List<Element>)categoriesE.getChildren(CATEGORY)) {
                String code = catE.getChildText(CATCODE);
                String desc = catE.getChildText(CATDESC);
                categories.put(code, desc);
            }

            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Read " + categories.size() + " categories");
            }
        } else {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("No categories defined");
            }
        }
    }
    
    public void writeResults(ClassifiedStatsAction.ClassificationStatsParams params, Map<MultiKey, List<Result>> result)
            throws IOException {
//        File outputFile = this.outputFile;

        int clsf = params.classifiers.size();
        if (clsf != 1 && clsf != 2)
            throw new IllegalStateException("unexpected classifier size " + clsf);

        boolean existMultiplier = params.dataMultiplier != null;
        boolean shortStats = existMultiplier || clsf == 2;
        writeResults(result, shortStats, existMultiplier);
        
    }

    public void writeResults(Map<MultiKey, List<Result>> result, boolean shortStats,
            boolean existMultiplier) throws IOException {
        FileWriter writer = null;
        try {
            if(LOGGER.isInfoEnabled())
                LOGGER.info("Writing stats to " + outputFile);
            writer = new FileWriter(outputFile);
            final Set<MultiKey> set = result.keySet();
            final CartesianProduct cartesianProduct = new CartesianProduct();

            // write header
            String header = shortStats ?
                            HEADER_CLASSIFIED_STATS :
                            HEADER_LONG_STATS;
                writer.write(header);

            // write lines
            for (MultiKey mkey : set) {
                List<Result> r = result.get(mkey);
                String line = shortStats ?
                        getClassifiedStatsLine(mkey, r, existMultiplier, cartesianProduct) :
                        getLongStatsLine(mkey, r) ;
                writer.write(line);
                writer.write("\n");
            }

            for (CartesianProduct.Pair<String, String> pair : cartesianProduct.getMissingProducts()) {
                String line = getEmptyClassifiedStatsLine(pair);
                writer.write(line);
                writer.write("\n");
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
    private String getLongStatsLine(MultiKey key, List<Result> results) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(gaezid).append(CSV_DELIM);
        sb.append(key.getKey(0)).append(CSV_DELIM); // we assume that the classifier layers have
                                                    // been passed in the proper order

        double min = Double.NaN;
        double max = Double.NaN;
        double rng = Double.NaN;
        double avg = Double.NaN;
        double std = Double.NaN;
        double sum = Double.NaN;
        long cnt = results.get(0).getNumAccepted();
        for (Result result : results) {
            switch (result.getStatistic()) {
            case MIN:
                min = result.getValue();
                break;
            case MAX:
                max = result.getValue();
                break;
            case RANGE:
                rng = result.getValue();
                break;
            case MEAN:
                avg = result.getValue();
                break;
            case SDEV:
                std = result.getValue();
                break;
            case SUM:
                sum = result.getValue();
                break;
            }
        }

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

    private static final String HEADER_CLASSIFIED_STATS = "GAEZ_ID,GAUL_CODE,CATEGORY_CODE,CATEGORY_DESCRIPTION,COUNT,AREA\n";

    private String getClassifiedStatsLine(MultiKey key, List<Result> results, boolean existMultiplier, CartesianProduct cp) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(gaezid).append(CSV_DELIM);

        String key0 = String.valueOf(key.getKey(0));
        sb.append(key0).append(CSV_DELIM); // we assume that the classifier layers have
                                                    // been passed in the proper order
        String catCode;
        String catDesc;

        if(existMultiplier) {
            catCode = "-1";
            catDesc = "";
        } else {
            catCode = String.valueOf(key.getKey(1));
            catDesc = categories.get(catCode);

            cp.addClassificationKeys(key);
        }

        catDesc = catDesc == null ?
                    "UNKNOWN" :
                    escapeCsvField(catDesc, CSV_DELIM);

        sb.append(catCode).append(CSV_DELIM);
        sb.append(catDesc).append(CSV_DELIM);

        double sum = -1;
        long cnt = results.get(0).getNumAccepted();
        for (Result result : results) {
            switch (result.getStatistic()) {
            case SUM:
                sum = result.getValue();
                break;
            }
        }

        sb.append(cnt).append(CSV_DELIM);
        sb.append(sum);

        return sb.toString();
    }

    private String getEmptyClassifiedStatsLine(CartesianProduct.Pair<String, String> pair ) {
        StringBuilder sb = new StringBuilder(100);

        sb.append(gaezid).append(CSV_DELIM);
        sb.append(pair.val0).append(CSV_DELIM);
        
        String catDesc = categories.get(pair.val1);
        catDesc = catDesc == null ?
                    "UNKNOWN" :
                    escapeCsvField(catDesc, CSV_DELIM);
        
        sb.append(pair.val1).append(CSV_DELIM);
        sb.append(catDesc).append(CSV_DELIM);
        sb.append(0l).append(CSV_DELIM);
        sb.append(0d);

        return sb.toString();
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

}
