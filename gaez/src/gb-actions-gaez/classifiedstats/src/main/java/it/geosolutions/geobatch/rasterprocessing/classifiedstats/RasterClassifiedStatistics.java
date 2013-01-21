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

package it.geosolutions.geobatch.rasterprocessing.classifiedstats;

import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsConfiguration.ComputationMode;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.DivideByConstDescriptor;
import javax.media.jai.operator.MultiplyDescriptor;

import org.jaitools.numeric.Range;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.geotools.image.jai.Registry;
import org.geotools.resources.image.ImageUtilities;
import org.jaitools.media.jai.classifiedstats.ClassifiedStats;
import org.jaitools.media.jai.classifiedstats.ClassifiedStatsDescriptor;
import org.jaitools.media.jai.classifiedstats.ClassifiedStatsRIF;
import org.jaitools.media.jai.classifiedstats.Result;
import org.jaitools.numeric.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.jai.operator.ImageReadDescriptor;
import java.util.ArrayList;

/**
 * A process computing classified statistics based on a raster data set 
 * and a set of raster classification layers
 * 
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class RasterClassifiedStatistics {
    private final static Logger LOGGER = LoggerFactory.getLogger(RasterClassifiedStatistics.class);
    
    static {
        try{
            Registry.registerRIF(JAI.getDefaultInstance(), new ClassifiedStatsDescriptor(), new ClassifiedStatsRIF(), Registry.JAI_TOOLS_PRODUCT);
        } catch (Throwable e) {
            // swallow exception in case the op has already been registered.
        }
    }

    /**
     * Loads a {@link RenderedImage} from the specified file with the specified mode.
     * 
     * @param mode, {@link ComputationMode} can be {@link ComputationMode#DEFERRED} or {@link ComputationMode#IMMEDIATE} as per the JAI meaning.
     * @param input the input {@link File} where to read from.
     * 
     * @return
     * @throws IOException in case the creation of the {@link ImageInputStream} or the Immediate read with Imageio fails.
     */
    private static RenderedImage loadImage(ComputationMode mode, File input) throws IOException{
        // checks
        if(!input.isFile()||!input.exists()||!input.canRead()){
            throw new IllegalArgumentException("Unable to load image from file: "+ input.getAbsolutePath()+"\n" +
            		"isFile="+input.isFile()+"\n"+
            		"exists="+input.exists()+"\n"+
            		"canRead="+input.canRead());
        }

        switch (mode) {
            case IMMEDIATE:
                return ImageIO.read(input);

            case DEFERRED:
                final ImageInputStream inStream= ImageIO.createImageInputStream(input);
                if(inStream==null){
                    throw new IllegalArgumentException("Unable to create input stream from file: "+ input.getAbsolutePath()+"\n" +
                            "isFile="+input.isFile()+"\n"+
                            "exists="+input.exists()+"\n"+
                            "canRead="+input.canRead());
                }
                return ImageReadDescriptor.create(
                        inStream, 
                        Integer.valueOf(0), 
                        false,
                        false,
                        false, 
                        null, 
                        null,
                        null,
                        null, 
                        null);
            default:
                throw new IllegalArgumentException("Invalid input mode:"+mode);
        }
        
    }
    
    public Map<MultiKey, List<Result>> execute(ComputationMode mode,
            DataFile dataFile, DataFile dataMultiplierFile,
            List<DataFile> classificationFiles, EnumSet<Statistic> requestedStats) throws IOException {

        if (LOGGER.isDebugEnabled()) {
            String mult = dataMultiplierFile == null? "" :  " x " +dataMultiplierFile;
    		LOGGER.debug("Computing stats on " + dataFile + mult + " classifying by " + classificationFiles);
        }

        RenderedImage dataImage=null,statsOp = null;
        RenderedImage classifiers[] = null;
        Double classNoData[] = null;
        
        List<Range> dataNDR = null; // No Data Ranges for data image
        if(dataFile.getNoValue() != null) {
            Range nodata = new Range(dataFile.getNoValue(), true, dataFile.getNoValue(), true);
            dataNDR = new ArrayList<Range>();
            dataNDR.add(nodata);
        }
        
        try{
       	
        	dataImage = loadImage(mode, dataFile.getFile());
// TESTS
//        	RenderedImageBrowser.showChain(sampleImage, true);
        	if (dataMultiplierFile != null){
        		
        		// apply percentage ops
        		// sampleImage is recalculated as (Data*dataMultiplier)/100;
                if(dataMultiplierFile.getNoValue() != null) {
                    LOGGER.warn("Nodata value in multiplier file is not yet supported.");
                    // TODO !!! SET NODATA
                }
    			final RenderedOp op = MultiplyDescriptor.create(dataImage, loadImage(mode, dataMultiplierFile.getFile()), null);
    			dataImage = DivideByConstDescriptor.create(op, new double[]{100D}, null);
        	}

            classifiers = new RenderedImage[classificationFiles.size()];
            classNoData = new Double[classificationFiles.size()];
            
            int i = 0;
            for (DataFile dfile : classificationFiles) {
                classifiers[i] = loadImage(mode, dfile.getFile());
                classNoData[i] = dfile.getNoValue() == null? Double.NaN : dfile.getNoValue();
                i++;
            }
// TESTS
//        	RenderedImageBrowser.showChain(sampleImage, true);
            Statistic reqStatsArr[] = requestedStats.toArray(new Statistic[requestedStats.size()]);
    
            final ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
            pb.addSource(dataImage);
            pb.setParameter("classifiers", classifiers);
            pb.setParameter("stats", reqStatsArr);

            if(dataNDR != null)
                pb.setParameter("noDataRanges", dataNDR);
            
            pb.setParameter("noDataClassifiers", classNoData);
            
            // TODO make it configurable
            pb.setParameter("bands", new Integer[]{0});
    
            statsOp = JAI.create("ClassifiedStats", pb);
            ClassifiedStats stats = (ClassifiedStats)((RenderedOp) statsOp).getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);
    
            List<Map<MultiKey,List<Result>>> fullResults = stats.results();
            Map<MultiKey, List<Result>> results = fullResults.get(0);
    
            if(LOGGER.isDebugEnabled()) {
                for (MultiKey key : results.keySet()) {
                    for (Result r: results.get(key)){
                        LOGGER.debug("Key -> " + key + " stats -> " + r);
                    }
                }
            }
            return results;
        } finally {
            if(statsOp!=null){
                try{
                    ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(statsOp));
                }catch (Exception e) {
                    if(LOGGER.isDebugEnabled())
                        LOGGER.debug(e.getLocalizedMessage(),e);
                }
            }
            
            // TODO can be removed ? we are doing recursive clean up with the call above! 
            // SG: as long as the statistics operation is based on NullOpImage we cannot rely on 
            //     stats image sources to dispose the original images, but we need to do it ourselves.
            for(RenderedImage ri:classifiers){
                if(ri!=null){
                    try{
                        ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(ri));
                    }catch (Exception e) {
                        if(LOGGER.isDebugEnabled())
                            LOGGER.debug(e.getLocalizedMessage(),e);
                    }
                }    
            }

        }

//        System.out.println("Getting Max from the result coming from the 2nd stripe (The first classifier raster, with value = 1), " +
//                "\n and the second classifier raster with value = 50");
//        System.out.println(stats.band(0).statistic(Statistic.MAX).results().get(new MultiKey(1,50)).get(0));

    }

}
