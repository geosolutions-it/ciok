package it.geosolutions.geobatch.gaez.utils.rules.ruleB;

import it.geosolutions.geobatch.gaez.utils.AggregatingRules;
import it.geosolutions.geobatch.gaez.utils.aggregator.Aggregator;
import it.geosolutions.geobatch.gaez.utils.rules.FileType;
import it.geosolutions.geobatch.gaez.utils.rules.ruleB.RuleBWriter.LineType;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
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

public abstract class RuleB {
	
	static {
        try{
            Registry.registerRIF(JAI.getDefaultInstance(), new RangeLookupDescriptor(), new RangeLookupRIF(), Registry.JAI_TOOLS_PRODUCT);
            Registry.registerRIF(JAI.getDefaultInstance(), new ClassifiedStatsDescriptor(), new ClassifiedStatsRIF(), Registry.JAI_TOOLS_PRODUCT);
        } catch (Throwable e) {
            // swallow exception in case the op has already been registered.
        }
    }

	/**
	 * Default logger
	 */
	protected final static Logger LOGGER = LoggerFactory.getLogger(RuleB.class);

	/**
	 * 
	 * B: accepted types P4 (percentage) , C (continuous int), C4 (continuous
	 * int/float)
	 * 
	 * @param runnungContextDir
	 * @param imageFile
	 * @param areas
	 * @param gauls
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> ruleBC(final FileType type,
			final boolean isRuleC, Double multiply,
			final File runnungContextDir,
			final AggregatingRules aggregatedObject, final File imageFile,
			final double[] nodataImage) throws Exception {

		final RenderedImage[][] areas = aggregatedObject.getAreas();
		final RenderedImage[][] gauls = aggregatedObject.getGauls();
		final double[][] nodataAreas = aggregatedObject.getNoDataAreas();
		final double[][] nodataGauls = aggregatedObject.getNoDataGauls();

		final TIFFImageReaderSpi spi = new TIFFImageReaderSpi();
		ImageReader reader = null;
		FileImageInputStream fis = null;
		// return
		Map<String, Object> retMap = null;
		try {
			reader = spi.createReaderInstance();

			fis = new FileImageInputStream(imageFile);
			reader.setInput(fis);

			ImageReadParam rp = new ImageReadParam();

			RenderedOp percentageImage = ImageReadDescriptor.create(fis, 0,
					false, false, false, null, null, rp, reader, null);

			// checks
			final int areaSize = areas.length;
			final int gaulSize = gauls.length;

			if (areaSize != gaulSize) {
				throw new Exception("bad argument length");
			}

			// checks
			final int areaSubSize = areas[0].length;
			final int gaulSubSize = gauls[0].length;

			if (areaSubSize != gaulSubSize) {
				throw new Exception("bad Sub argument length");
			}

			// GAEZ_ID
			final String gaez_id = FilenameUtils.getBaseName(imageFile
					.getName());

			// return
			retMap = new HashMap<String, Object>();
			// DO NOT REMOVE ME
			// ------------USEFUL FOR CLASSES (P4)---------------
			// // GDALINFO 2 CSV
			//
			// final Document GdalInfoDoc ;
			// if ()
			// GdalInfoDoc = GdalInfoExecutor
			// .gdalinfoToDocument(imageFile);
			// final Element GdalInfoDocElement = GdalInfoDoc.getRootElement();
			// // used
			// // into
			// // if it is a discrete file type categories node should never be
			// null!
			// if (GdalInfoDocElement == null) {
			// final String message =
			// "ERROR in RuleB -> here 'GdalInfoDocElement' may NOT be null";
			// if (LOGGER.isErrorEnabled())
			// LOGGER.error(message);
			// throw new Exception(message);
			// }
			// DO NOT REMOVE ME

			/**
			 * i e ADMj
			 */
			for (int level = 0; level < gaulSize; level++) {
				final List<Map<MultiKey, List<Result>>> areaSubResults = new ArrayList<Map<MultiKey, List<Result>>>(
						gaulSubSize);
				final List<Map<MultiKey, List<Result>>> productSubResults = new ArrayList<Map<MultiKey, List<Result>>>(
						gaulSubSize);

				/**
				 * j - index used for administrative units=1,...,M
				 */
				for (int j = 0; j < gaulSubSize; j++) {

					final RenderedImage area = areas[level][j];
					final RenderedImage gaul = gauls[level][j];
					final RenderedImage product;

					if (multiply == null) {
						multiply = 1d;
					}

					double localMultiply = type.equals(FileType.P4) ? multiply / 100
							: multiply;
					
					product = produceFiltered(percentageImage, area,
							nodataImage, new double[] { nodataAreas[level][j] },
							localMultiply);

					// ImageIOUtilities.visualize(area,"area",true);
					ROI roi = null;
					if (isRuleC) {
						RenderedImage maskedRoi = produceMaskedROI(
								percentageImage, nodataImage);
						roi = new ROI(maskedRoi, 0);
						// TODO roi should not exclude negative values!
						
						// ImageIOUtilities.visualize(roi.getAsImage(),"roi",true);
					}
					RenderedImage classifiedArea = classifyArea(area, gaul,
							nodataAreas[level][j], roi);// ImageIOUtilities.visualize(gaul,"gaul",true);
					ClassifiedStats areaStats = (ClassifiedStats) ((RenderedOp) classifiedArea)
							.getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);
					areaSubResults.add(areaStats.results().get(0));// areaStats.results().get(0));

					// RenderedImage classifiedProduct = classifyProduct(type,
					// product[0], gaul, nodataGauls[i][j]);
					RenderedImage classifiedProduct = classifyProduct(type,
							product, gaul, nodataGauls[level][j]);

					ClassifiedStats productStats = (ClassifiedStats) ((RenderedOp) classifiedProduct)
							.getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);
					productSubResults.add(productStats.results().get(0));

					if (LOGGER.isDebugEnabled())
						LOGGER.debug("Classifying #" + j);

				}

				LineType lineType;
				Aggregator aggregatorType;
				File csvOutputFile;
				if (type.equals(FileType.P4)) {
					lineType = LineType.SHORT;
					aggregatorType = new RuleBShortAggregator(areaSubResults);
					csvOutputFile = new File(runnungContextDir, "I" + level + "_"
							+ gaez_id + ".csv");
				} else {
					// FileType.C4;
					// FileType.C
					lineType = LineType.LONG;
					aggregatorType = new RuleBLongAggregator(areaSubResults);
					csvOutputFile = new File(runnungContextDir, "F" + level + "_"
							+ gaez_id + ".csv");
				}

				retMap.put(level + "_csv", csvOutputFile);
				
				RuleBWriter writer = new RuleBWriter(lineType, csvOutputFile,
						gaez_id);
				writer.writeResults(aggregatorType.aggregate(productSubResults,
						null));

			} // for gaul levels
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
			throw e;
		} finally {
			try {
				if (reader != null)
					reader.dispose();
			} catch (Exception e) {
			}

			try {
				if (fis != null)
					fis.close();
			} catch (Exception e) {
			}
		}

		return retMap;
	}

	private static RenderedImage classifyProduct(final FileType type,
			final RenderedImage sourceImage, final RenderedImage classifier,
			final double noData) {

		final Statistic reqStatsArr[];
		if (type.equals(FileType.P4)) {
			reqStatsArr = new Statistic[] { Statistic.SUM, Statistic.MAX,
					Statistic.MIN };
		} else {
			// Statistic.SUM,Statistic.MAX, Statistic.MIN, Statistic.RANGE,
			// Statistic.MEAN,Statistic.VARIANCE
			// Set<Statistic> requestedStats = EnumSet.of(Statistic.SUM,
			// Statistic.MAX, Statistic.MIN, Statistic.MEAN,
			// Statistic.VARIANCE);//Statistic.RANGE,
			// reqStatsArr = requestedStats.toArray(new
			// Statistic[requestedStats.size()]);
			reqStatsArr = new Statistic[] { Statistic.SUM, Statistic.MAX,
					Statistic.MIN, Statistic.MEAN, Statistic.VARIANCE };// Statistic.RANGE,
		}

		final ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
		pb.addSource(sourceImage);
		pb.setParameter("classifiers", new RenderedImage[] { classifier });
		pb.setParameter("stats", reqStatsArr);

		// TODO make it configurable
		pb.setParameter("bands", new Integer[] { 0 });
		pb.setParameter("nodataClassifiers", new Double[] { noData });

		return JAI.create("ClassifiedStats", pb);
	}

	private static RenderedImage classifyArea(final RenderedImage sourceImage,
			final RenderedImage classifier, final double noData, final ROI roi) {

		final Statistic reqStatsArr[] = new Statistic[] { Statistic.SUM };

		final ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
		pb.addSource(sourceImage);
		pb.setParameter("classifiers", new RenderedImage[] { classifier });
		pb.setParameter("stats", reqStatsArr);
		if (roi!=null)
			pb.setParameter("roi", roi);

		// TODO make it configurable
		pb.setParameter("bands", new Integer[] { 0 });
		pb.setParameter("nodataClassifiers", new Double[] { noData });

		return JAI.create("ClassifiedStats", pb);
	}

	private static RenderedImage produceFiltered(RenderedImage percentageImage,
			RenderedImage areaImage, double[] percentageNoData,
			double[] areaNoData, double multiply) {

		RangeLookupTable<Double, Double> tablePercentage = new RangeLookupTable<Double, Double>(
				true);
		for (double noData : percentageNoData) {
			Range<Double> r = Range.create(noData);
			tablePercentage.add(r, Double.NaN);
		}
		RenderedImage maskedPercentage = RangeLookupDescriptor.create(
				percentageImage, tablePercentage, null);

		RangeLookupTable<Double, Double> tableArea = new RangeLookupTable<Double, Double>(
				true);
		for (double noData : areaNoData) {
			Range<Double> r2 = Range.create(noData);
			tableArea.add(r2, Double.NaN);
		}
		RenderedImage maskedArea = RangeLookupDescriptor.create(areaImage,
				tableArea, null);

		RenderedImage product = MultiplyDescriptor.create(maskedPercentage,
				maskedArea, null);

		if (!Double.isNaN(multiply)
				&& Math.abs(multiply - 1) > Double.MIN_VALUE) {
			product = MultiplyConstDescriptor.create(product,
					new double[] { multiply }, null);
		}

		return product;
	}

	// private static RenderedImage[] produceFiltered(RenderedImage
	// percentageImage,
	// RenderedImage areaImage, double[] percentageNoData,
	// double[] areaNoData, double multiply, boolean returnMaskedPercentage) {
	//
	// RangeLookupTable<Double, Double> tablePercentage = new
	// RangeLookupTable<Double, Double>(
	// true);
	// for (double noData : percentageNoData) {
	// Range<Double> r = Range.create(noData);
	// tablePercentage.add(r, Double.NaN);
	// }
	// RenderedImage maskedPercentage =
	// RangeLookupDescriptor.create(percentageImage, tablePercentage, null);
	//
	// RangeLookupTable<Double, Double> tableArea = new RangeLookupTable<Double,
	// Double>(true);
	// for (double noData : areaNoData) {
	// Range<Double> r2 = Range.create(noData);
	// tableArea.add(r2, Double.NaN);
	// }
	// RenderedImage maskedArea =
	// RangeLookupDescriptor.create(areaImage,tableArea, null);
	//
	// RenderedImage product = MultiplyDescriptor.create(maskedPercentage,
	// maskedArea, null);
	//
	// if (!Double.isNaN(multiply)
	// && Math.abs(multiply - 1) > Double.MIN_VALUE) {
	// product = MultiplyConstDescriptor.create(product,
	// new double[] { multiply }, null);
	// }
	//
	// return returnMaskedPercentage ? new RenderedImage[]{product,
	// maskedPercentage} : new RenderedImage[]{product} ;
	// }

	private static RenderedImage produceMaskedROI(
			RenderedImage percentageImage, double[] noData) {

		RangeLookupTable<Double, Double> table = new RangeLookupTable<Double, Double>(
				true);
		for (double nodata : noData) {
			Range<Double> r = Range.create(nodata);
			table.add(r, Double.NaN);
		}
		RenderedImage maskedROI = RangeLookupDescriptor.create(percentageImage,
				table, null);

		return maskedROI;
	}

}
