package it.geosolutions.geobatch.gaez.utils.rules.ruleD;

import it.geosolutions.geobatch.gaez.utils.AggregatingRules;
import it.geosolutions.geobatch.gaez.utils.aggregator.AggregableStats;
import it.geosolutions.geobatch.gaez.utils.aggregator.Aggregator;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.MultiplyDescriptor;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.io.FilenameUtils;
import org.geotools.image.jai.Registry;
import org.jaitools.media.jai.classifiedstats.ClassifiedStats;
import org.jaitools.media.jai.classifiedstats.ClassifiedStatsDescriptor;
import org.jaitools.media.jai.classifiedstats.ClassifiedStatsRIF;
import org.jaitools.media.jai.classifiedstats.Result;
import org.jaitools.media.jai.rangelookup.RangeLookupDescriptor;
import org.jaitools.media.jai.rangelookup.RangeLookupRIF;
import org.jaitools.media.jai.rangelookup.RangeLookupTable;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.jai.operator.ImageReadDescriptor;

/**
 * 
 * @author ETj (etj at geo-solutions.it)
 */
public abstract class RuleD {
	
	static {
        try{
            Registry.registerRIF(JAI.getDefaultInstance(), new RangeLookupDescriptor(), new RangeLookupRIF(), Registry.JAI_TOOLS_PRODUCT);
            Registry.registerRIF(JAI.getDefaultInstance(), new ClassifiedStatsDescriptor(), new ClassifiedStatsRIF(), Registry.JAI_TOOLS_PRODUCT);
        } catch (Throwable e) {
            // swallow exception in case the op has already been registered.
        }
    }

	
	protected final static Logger LOGGER = LoggerFactory.getLogger(RuleD.class);
	
	/**
	 * 
	 * C (continuous int), C4 (continuous int/float)
	 * 
	 * @param outputDir
	 * @param dataFile
	 * @param areaRatio
	 * @param gaulCodes
	 * @return
	 * @throws Exception
	 * @deprecated under development
	 */
	public static Map<String, Object> ruleD(final AggregatingRules aggregatedObject, final double multiplier,
            final File dataFile, final double nodataData,
			final File outputDir) throws Exception {
		
		final RenderedImage[][] areaRatio=aggregatedObject.getAreas();
		final double[][] nodataRatio=aggregatedObject.getNoDataAreas();
        final RenderedImage[][] gaulCodes=aggregatedObject.getGauls();
        final double[][] nodataGaul=aggregatedObject.getNoDataGauls();
		// checks
		final int areaLevels = areaRatio.length;
		final int gaulLevels = gaulCodes.length;

		if (areaLevels != gaulLevels) {
			throw new IllegalArgumentException("Unmatching levels size");
		}

		// checks
		final int areaSplits = areaRatio[0].length;
		final int gaulSplits = gaulCodes[0].length;

		if (areaSplits != gaulSplits) {
			throw new IllegalArgumentException("Unmatching max split size");
		}

		ImageReader dataReader = new TIFFImageReaderSpi().createReaderInstance();
		FileImageInputStream dataIS = null;
		try {
			dataIS = new FileImageInputStream(dataFile);
			dataReader.setInput(dataIS);
	
			ImageReadParam rp = new ImageReadParam();
	
			RenderedOp dataImage = ImageReadDescriptor.create(dataIS, 0, false, false,
					false, null, null, rp, dataReader, null);
	
			// return object
			final Map<String, Object> retMap = new HashMap<String, Object>();
	
			/*
	         * Loop on all GAUL levels
			 */
			for (int level = 0; level < gaulLevels; level++) {
	
				final List<Map<MultiKey, List<Result>>> subResults =
	                    new ArrayList<Map<MultiKey, List<Result>>>(gaulSplits);
	
				/*
	             * Loop on all raster splits
				 */
				for (int split = 0; split < gaulSplits; split++) {
	
	                if (LOGGER.isDebugEnabled())
						LOGGER.debug("Classifying level#" + level + " split#" + split);
	
					final RenderedImage areaRatioImage = areaRatio[level][split];
					final RenderedImage gaulImage      = gaulCodes[level][split];
					

					RenderedImage multiplied;
					
					if (multiplier!=1){ // TODO better control (epsilon)
						// mask nodata setting to NaN
						RangeLookupTable<Double, Double> tableData = new RangeLookupTable<Double, Double>(true);
		                Range<Double> r = Range.create(nodataData);
		                tableData.add(r, Double.NaN);
		                multiplied = RangeLookupDescriptor.create(dataImage, tableData, null);
		                multiplied=MultiplyConstDescriptor.create(multiplied, new double[]{multiplier}, null);
					}
					else
						multiplied=dataImage;
					
					multiplied = multiply(
							multiplied, new double[]{nodataData},
	                        areaRatioImage, new double[]{nodataRatio[level][split]});
//					
//					RenderedImage statsRI = doStats(multiplied, nodataData, gaulImage, nodataGaul[level][split]);
					RenderedImage statsRI = doStats(multiplied, nodataData, gaulImage, nodataGaul[level][split]);
	
					ClassifiedStats stats = (ClassifiedStats) ((RenderedOp) statsRI)
							.getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);
	
					subResults.add(stats.results().get(0));
				}
	
	            // Merge all the stats computed on different split of the same level
	            Map<MultiKey, AggregableStats> resultForLevel = mergeStats(subResults, gaulSplits);
	            
	            // GAEZ_ID
				final String gaez_id = FilenameUtils.getBaseName(dataFile.getName());
				
	            // Write out the stats in csv format
				final File csvOutputFile = new File(outputDir, "F" + level + "_" + gaez_id + ".csv");
				
	            SingleClassifierLineWriter writer = new SingleClassifierLineWriter(csvOutputFile,gaez_id);
//	            writer.setOutputFile(csvOutputFile);
//	            writer.setGaezid(gaez_id);

				writer.writeResults(resultForLevel);
	
	            // Remember the output file
				retMap.put(level+"_csv", csvOutputFile);

			} // for gaul levels
			return retMap;	
		}catch (IOException e){
			LOGGER.error(e.getLocalizedMessage(),e);
		}finally {
			if (dataIS!=null)
				dataIS.close();
		}
return null;
	}
	

    /**
     *
     */
    private static Map<MultiKey, AggregableStats> mergeStats(List<Map<MultiKey, List<Result>>> splittedResults, int numSplits) {

        // First scan to initialize all the keys
        Set<MultiKey> gaulCodes = new HashSet<MultiKey>();

        for (int split = 0; split < numSplits; split++) {
            Map<MultiKey, List<Result>> resultsInSplit = splittedResults.get(split);
            Set<MultiKey> keysInSplit = resultsInSplit.keySet();
            gaulCodes.addAll(keysInSplit);
        }

        if(LOGGER.isDebugEnabled())
            LOGGER.debug("Found " + gaulCodes.size() + " multikeys");

        Map<MultiKey, AggregableStats> output = new HashMap<MultiKey, AggregableStats>();

        for (MultiKey key : gaulCodes) {
            AggregableStats finalStats=new AggregableStats();

            for (int split = 0; split < numSplits; split++) {
                Map<MultiKey, List<Result>> res = splittedResults.get(split);
                List<Result> results = res.get(key);
                    //SUM, MAX, MIN, RANGE, MEAN, VARIANCE
                if (results!=null){
                	finalStats = Aggregator.aggregate(finalStats,buildAggregableStats(results));
                }
            }

            output.put(key, finalStats);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Merged #");
            }
        }

        return output;
    }

    static final Statistic reqStatsArr[] = new Statistic[] { Statistic.SUM, Statistic.MAX, Statistic.MIN, Statistic.MEAN, Statistic.VARIANCE };

    private static RenderedImage doStats(
			final RenderedImage sourceImage, final Double noDataImage, 
            final RenderedImage classifier, final double noData) {

		final ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
		pb.addSource(sourceImage);
		
		Collection<Range<Double>> noDataRanges=new ArrayList<Range<Double>>();
		noDataRanges.add(new Range<Double>(noDataImage, true, noDataImage, true));
		pb.setParameter("noDataRanges", noDataRanges);
		
		pb.setParameter("classifiers", new RenderedImage[] { classifier });
		pb.setParameter("nodataClassifiers", new Double[] { noData });
		pb.setParameter("stats", reqStatsArr);

		// TODO make it configurable
		pb.setParameter("bands", new Integer[] { 0 });

		return JAI.create("ClassifiedStats", pb);
	}
    
    
    protected static AggregableStats buildAggregableStats(final List<Result> results) {
		final AggregableStats stat=new AggregableStats();
		
		if (results.size()>0)
			stat.setSize(results.get(0).getNumAccepted());
		else
			throw new IllegalArgumentException("The passed results list is empty");
		
		for (Result res : results) {
			switch (res.getStatistic()){
			case SUM:
				stat.setSum(res.getValue());
				break;
			case MEAN:
				stat.setMean(res.getValue());
				break;
			case MAX:
				stat.setMax(res.getValue());
				break;
			case MIN:
				stat.setMin(res.getValue());
				break;
			case VARIANCE:
				stat.setVariance(res.getValue());
				break;
			default:
				break;
			}
		}
		return stat;

	}
	
	private static RenderedImage multiply(
            RenderedImage dataImage,      double[] dataNoData,
            RenderedImage areaRatioImage, double[] areaNoData ) {

	    RangeLookupTable<Double, Double> tableData = new RangeLookupTable<Double, Double>(true);

        boolean doMaskData = false;
	    for (double noData : dataNoData){
            if( ! Double.isNaN(noData)) {
                final Range<Double> r = Range.create(noData);
                tableData.add(r, Double.NaN);
                doMaskData = true;
            }
	    }

        boolean doMaskArea = false;
	    RangeLookupTable<Double, Double> tableArea = new RangeLookupTable<Double, Double>(true);
	    for (double noData : areaNoData){
            if( ! Double.isNaN(noData)) {
            	final Range<Double> r2 = Range.create(noData);
        	    tableArea.add(r2, Double.NaN);
                doMaskArea = true;
            }
	    }
	    
	    RenderedImage maskedData = doMaskData ? 
                RangeLookupDescriptor.create(dataImage, tableData, null) :
                dataImage;
	    
	    RenderedImage maskedArea = doMaskArea ? 
                RangeLookupDescriptor.create(areaRatioImage, tableArea, null) :
                areaRatioImage;
	    
	    RenderedImage product = MultiplyDescriptor.create(maskedData, maskedArea, null);
	    
	    return product;
    }
	
	

	
}
