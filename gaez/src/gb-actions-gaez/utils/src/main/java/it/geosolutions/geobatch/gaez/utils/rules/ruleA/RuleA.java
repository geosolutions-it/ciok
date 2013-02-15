package it.geosolutions.geobatch.gaez.utils.rules.ruleA;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.gaez.utils.GdalInfoExecutor;
import it.geosolutions.geobatch.gaez.utils.rules.FileType;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsAction;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsAction.ClassificationStatsParams;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RuleA {

	/**
	 * Default logger
	 */
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(RuleA.class);


	public static final String LEGEND_KEY="legend_csv";
	
	/**
	 * A:
	 * accepted types: F (discrete), C (continuous)
	 * 
	 * @param gdalinfoTemplate
	 * @param runnungContextDir
	 * @param imageFile
	 * @param areas
	 * @param gauls
	 * @return
	 * @throws Exception
	 */
	
	public static Map<String, Object> ruleA(final FileType type, final File gdalinfoTemplate,
			final File runnungContextDir, final File imageFile,
			final File[][] areas, final File[][] gauls,
			final Double nodataImage, final double[][] nodataAreas, final double[][] nodataGauls) throws Exception {
		
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
		final String gaez_id = FilenameUtils.getBaseName(imageFile.getName());

		// return
		final Map<String, Object> retMap = new HashMap<String, Object>();

		/**
		 * creating element of categories needed to the output <categories>
		 * <class><value>VVV</value><description>DDD</description></class> ...
		 * </categories>
		 */
		Element categories =null;
		if (type.isDiscrete() || type==FileType.P4){
			// GDALINFO 2 CSV
			final Document GdalInfoDoc = GdalInfoExecutor
					.gdalinfoToDocument(imageFile);
			if (GdalInfoDoc==null)
				throw new NullPointerException("Unable to get Gdalinfo Document from: "+imageFile);
			final Element GdalInfoDocElement = GdalInfoDoc.getRootElement(); // used
																				// into
																				// the
																				// class
																				// stats...
	
			// if it is a discrete file type categories node should never be null!
			if (GdalInfoDocElement == null) {
				final String message = "ERROR in RuleA -> here 'GdalInfoDocElement' may NOT be null";
				if (LOGGER.isErrorEnabled())
					LOGGER.error(message);
				throw new Exception(message);
				// map.put("status_msg",message);
				// map.put("status_code","KO"); // throw e;
				// continue;
			}
			/**
			 * creating element of categories needed to the output <categories>
			 * <class><value>VVV</value><description>DDD</description></class> ...
			 * </categories>
			 */
			categories = new Element("categories");
			for (Element row : (List<Element>) GdalInfoDocElement
					.getChildren("Row")) {
				final List<Element> fValues = row.getChildren("F");
				String code = fValues.get(0).getText();
				String desc = fValues.get(fValues.size() - 1).getText();
				Element category = new Element("category");
				category.addContent(new Element("code").setText(code));
				category.addContent(new Element("description").setText(desc));
				categories.addContent(category);
			}
		
			// get the legend using GdalInfo
			final File csvLegendOut = new File(runnungContextDir, gaez_id
					+ "_legend.csv");
			final File gdalInfoResFile = GdalInfoExecutor.process(
					GdalInfoExecutor.convertToDOM(GdalInfoDoc), gdalinfoTemplate,
					csvLegendOut);
			if (gdalInfoResFile == null) {
				final String message = "ERROR in RuleA -> Unable to get gdalinfoToCSV output";
				if (LOGGER.isErrorEnabled())
					LOGGER.error(message);
				throw new Exception(message);
				// map.put("status_msg","Unable to get gdalinfoToCSV output");
				// map.put("status_code","KO"); // throw e;
			}
		
			// add legend to the return
			retMap.put(LEGEND_KEY, csvLegendOut);

		}
		// CLASSIFIED STATS ACTION
		final ClassifiedStatsConfiguration actionCfg = new ClassifiedStatsConfiguration(
				"csc", "csc", "csc");
		final ClassifiedStatsAction action = new ClassifiedStatsAction(
				actionCfg);

		final File csvOutputSumFile[] = new File[gaulSize];
		/**
		 * i e ADMj
		 */
		for (int level = 0; level < gaulSize; level++) {

			final File inputFile[] = new File[gaulSubSize];
			final File cswOutputFile[] = new File[gaulSubSize];
			final File summaryOutputFile[] = new File[gaulSubSize];

			/**
			 * j - index used for administrative units=1,...,M
			 */
			for (int j = 0; j < gaulSubSize; j++) {

				final File areaRasterFile = areas[level][j];
				final File gaulFile = gauls[level][j];

				inputFile[j] = File.createTempFile("clstats_in", ".xml",
						runnungContextDir);

				final Element reqRoot;
				reqRoot = new Element("classifiedStats")
						.addContent(
								new Element(ClassificationStatsParams.DATA).setAttribute(ClassificationStatsParams.ATTRIB_NODATA, nodataImage.toString())
										.setText(areaRasterFile
										.getAbsolutePath()))
						.addContent(
								new Element(ClassificationStatsParams.CLASSIFICATION).setAttribute(ClassificationStatsParams.ATTRIB_NODATA, Double.toHexString(nodataGauls[level][j]))
										.setText(gaulFile
										.getAbsolutePath()))
						.addContent(
								new Element(ClassificationStatsParams.CLASSIFICATION).setAttribute(ClassificationStatsParams.ATTRIB_NODATA, Double.toHexString(nodataAreas[level][j]))
										.setText(imageFile
										.getAbsolutePath()));

				// notice that 'i' is the gaul level
				cswOutputFile[j] = new File(runnungContextDir, "I" + level + "_"
						+ j + "_" + gaez_id + ".csv");

				summaryOutputFile[j] = File.createTempFile("clSummaryStats_in",
						".properties", runnungContextDir);

				final Element output = new Element(ClassificationStatsParams.OUTPUTPARAMS)
						.addContent(
								new Element("file").setText(cswOutputFile[j]
										.getAbsolutePath()))
						.addContent(new Element("gaez_id").setText(gaez_id));

				if (type.isDiscrete() || type==FileType.P4){
					if (categories==null)
						throw new IllegalArgumentException("Categories may not be null here...");
					/**
					 * adding categories node
					 */
					if (!categories.isRootElement())
						categories.detach();
					output.addContent(categories);
				}

				reqRoot.addContent(output);

				final XMLOutputter outputter = new XMLOutputter(
						Format.getCompactFormat());
				FileUtils.writeStringToFile(inputFile[j],
						outputter.outputString(reqRoot));

				// CLASSIFIED STATS ACTION
				Queue<FileSystemEvent> queue = new LinkedList<FileSystemEvent>();
				queue.add(new FileSystemEvent(inputFile[j],
						FileSystemEventType.FILE_ADDED));
				queue = action.execute(queue);

//summarySum(retMap, summaryOutputFile[j], i);

			} // for Sub gaul levels

			csvOutputSumFile[level] = new File(runnungContextDir, "I" + level + "_"
					+ gaez_id + ".csv");

			if (CSVUtils.mergeCsv(cswOutputFile, csvOutputSumFile[level]) != Boolean.TRUE) {
				final String message = "ERROR in RuleA -> failed to calculate stats sum";
				if (LOGGER.isErrorEnabled())
					LOGGER.error(message);
				throw new Exception(message);
			}

			retMap.put(level+"_csv", csvOutputSumFile[level]);

		} // for gaul levels

		return retMap;
	}

}
