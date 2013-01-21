/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
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

import it.geosolutions.geobatch.gaez.utils.AggregatingRules;
import it.geosolutions.geobatch.gaez.utils.AreaRationalizer;
import it.geosolutions.geobatch.gaez.utils.GdalInfoExecutor;
import it.geosolutions.geobatch.gaez.utils.rules.ruleA.RuleA;
import it.geosolutions.geobatch.gaez.utils.rules.ruleB.RuleB;
import it.geosolutions.geobatch.gaez.utils.rules.ruleD.RuleD;
import it.geosolutions.tools.io.file.Collector;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.jai.JAI;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author Daniele Romagnoli @geo-solutions.it
 * 
 */
public class AggregatingRulesTest {
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(AggregatingRulesTest.class);

	final private static File[][] gauls = new File[][] {
			{
					new File(
							"src/test/resources/test-data/stats/gauls/gaul0_1.tif"),
					new File(
							"src/test/resources/test-data/stats/gauls/gaul0_2.tif"),
					new File(
							"src/test/resources/test-data/stats/gauls/gaul0_3.tif"),
					new File(
							"src/test/resources/test-data/stats/gauls/gaul0_4.tif") },
			{
					new File(
							"src/test/resources/test-data/stats/gauls/gaul1_1.tif"),
					new File(
							"src/test/resources/test-data/stats/gauls/gaul1_2.tif"),
					new File(
							"src/test/resources/test-data/stats/gauls/gaul1_3.tif"),
					new File(
							"src/test/resources/test-data/stats/gauls/gaul1_4.tif") } };

	final private static File[][] areas = new File[][] {
			{
					new File(
							"src/test/resources/test-data/stats/areas/area0_1.tif"),
					new File(
							"src/test/resources/test-data/stats/areas/area0_2.tif"),
					new File(
							"src/test/resources/test-data/stats/areas/area0_3.tif"),
					new File(
							"src/test/resources/test-data/stats/areas/area0_4.tif"), },
			{
					new File(
							"src/test/resources/test-data/stats/areas/area1_1.tif"),
					new File(
							"src/test/resources/test-data/stats/areas/area1_2.tif"),
					new File(
							"src/test/resources/test-data/stats/areas/area1_3.tif"),
					new File(
							"src/test/resources/test-data/stats/areas/area1_4.tif"), } };

	final double[][] areasNoData = new double[][] { { 0, 0, 0, 0, },
			{ 0, 0, 0, 0, } };

	final double[][] gaulsNodata = new double[][] {
			{ -32768D, -32768D, -32768D, -32768D },
			{ -32768D, -32768D, -32768D, -32768D } };

	@Before
	public void before() {
		// JAI.getDefaultInstance().getTileCache().setMemoryThreshold(0.75f);

		JAI.getDefaultInstance().getTileCache()
				.setMemoryCapacity(1024 * 1024 * 756);
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testA() throws IOException {

		// final File gdalinfoTemplate = new
		// File("/home/carlo/work/project/ciok/trunk/gaez/GEOBATCH_DATA_DIR/gaez_flow/config/transform.template");
		final File gdalinfoTemplate = new File(
				"src/test/resources/test-data/stats/ruleA/legend.template");
		// final File imageFile = new
		// File("/media/bigshare/temp/gaez/TIFF/main_ac/res01_mc2_c2a22020.tif");
		final File imageFileDir = new File(
				"src/test/resources/test-data/stats/ruleA/");

		final Collector c = new Collector(new WildcardFileFilter("*.tif"));
		final List<File> imageFileList = c.collect(imageFileDir);

		final File runnungContextDir = new File(
				"src/test/resources/test-data/stats/ruleA/out/");
		if (runnungContextDir.exists())
			FileUtils.deleteDirectory(runnungContextDir);
		runnungContextDir.mkdirs();

		final Iterator<File> it = imageFileList.iterator();
		while (it.hasNext()) {
			final File imageFile = it.next();

			try {
				// TODO check if MAX_val || MIN_VAL == null
				Map<String, Double> maxMinMap = GdalInfoExecutor
						.getMaxMinNoData(imageFile);
				if (maxMinMap == null) {
					throw new NullPointerException(
							"::GAEZ:: problem calculating min_value, max_value or noData");
				}
				Double noData = maxMinMap.get(GdalInfoExecutor.NODATA_KEY);
				Assert.assertNotNull(noData);

				Map<String, Object> mapOut = RuleA.ruleA(FileType.F,
						gdalinfoTemplate, runnungContextDir, imageFile, areas,
						gauls, noData, areasNoData, gaulsNodata);

				File legend = (File) mapOut.get(RuleA.LEGEND_KEY);
				Assert.assertNotNull(legend);
				if (LOGGER.isInfoEnabled())
					LOGGER.info("FILE legend:" + legend.getAbsolutePath());
				Assert.assertTrue(legend.exists());

				File statsLev0 = (File) mapOut.get("L0_csv");
				Assert.assertNotNull(statsLev0);
				if (LOGGER.isInfoEnabled())
					LOGGER.info("FILE stats_lev0:"
							+ statsLev0.getAbsolutePath());
				Assert.assertTrue(statsLev0.exists());

				File statsLev1 = (File) mapOut.get("L1_csv");
				Assert.assertNotNull(statsLev1);
				if (LOGGER.isInfoEnabled())
					LOGGER.info("FILE stats_lev1:"
							+ statsLev1.getAbsolutePath());
				Assert.assertTrue(statsLev1.exists());

			} catch (Exception e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 * 
	 * @note: UPDATE gaezprj.data set status_code='TOK' where file_name_tif=
	 *        'res01_hist_lt2_1979.tif' or
	 *        file_name_tif='res01_hist_lt3_1984.tif' or
	 *        file_name_tif='res01_hist_prc_1978.tif' or
	 *        file_name_tif='res01_hist_prc_1979.tif' or
	 *        file_name_tif='res01_hist_prc_1990.tif';
	 * 
	 * 
	 *        SELECT *from gaezprj.data where
	 *        file_name_tif='res01_hist_lt2_1979.tif' or
	 *        file_name_tif='res01_hist_lt3_1984.tif' or
	 *        file_name_tif='res01_hist_prc_1978.tif' or
	 *        file_name_tif='res01_hist_prc_1979.tif' or
	 *        file_name_tif='res01_hist_prc_1990.tif';
	 */
	@Test
	@Ignore
	public void testB_C() throws Exception {

		final AggregatingRules aggregator = new AggregatingRules(gauls, areas);

		final File runnungContextDir = new File(
				"src/test/resources/test-data/stats/ruleB/b_c/out/");
		if (runnungContextDir.exists())
			FileUtils.deleteDirectory(runnungContextDir);
		runnungContextDir.mkdirs();

		final File imageFileDir = new File(
				"src/test/resources/test-data/stats/ruleB/b_c/");

		final Collector c = new Collector(new WildcardFileFilter("*.tif"));
		final List<File> imageFileList = c.collect(imageFileDir);
		try {
			final Iterator<File> it = imageFileList.iterator();
			while (it.hasNext()) {
				final File imageFile = it.next();
				try {
					Map<String, Double> map = GdalInfoExecutor
							.getMaxMinNoData(imageFile);
					double noData = map.get(GdalInfoExecutor.NODATA_KEY);
					double multipy = 1;

					RuleB.ruleBC(FileType.C, false, multipy, runnungContextDir,
							aggregator, imageFile, new double[] { noData });

				} catch (Exception e) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error(e.getLocalizedMessage(), e);
				} finally {
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			aggregator.dispose(true);
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 * 
	 * @note: UPDATE gaezprj.data set status_code='TOK', status_msg='' where
	 *        file_name_tif= 'res02_eha22020i_oats125b_wde.tif' or
	 *        file_name_tif= 'res02_eha22020i_ricd125b_wde.tif'or file_name_tif=
	 *        'res02_eha22020i_spot125b_wde.tif'or file_name_tif=
	 *        'res02_eha22020i_srye015b_wde.tif' or file_name_tif=
	 *        'res02_eha22020i_srye015b_wde.tif' or
	 *        file_name_tif='res02_eha22020i_sugb150b_wde.tif'
	 * 
	 * 
	 *        SELECT * from gaezprj.data where file_name_tif=
	 *        'res02_eha22020i_oats125b_wde.tif' or file_name_tif=
	 *        'res02_eha22020i_ricd125b_wde.tif'or file_name_tif=
	 *        'res02_eha22020i_spot125b_wde.tif'or file_name_tif=
	 *        'res02_eha22020i_srye015b_wde.tif' or file_name_tif=
	 *        'res02_eha22020i_srye015b_wde.tif' or
	 *        file_name_tif='res02_eha22020i_sugb150b_wde.tif'
	 * 
	 */
	@Test
	@Ignore
	public void testC_c() throws Exception {
		JAI.getDefaultInstance().getTileCache()
				.setMemoryCapacity(768 * 1024 * 1024);

		final File runnungContextDir = new File(
				"src/test/resources/test-data/stats/ruleC/c_c/out/");
		if (runnungContextDir.exists())
			FileUtils.deleteDirectory(runnungContextDir);
		runnungContextDir.mkdirs();

		final File imageFileDir = new File(
				"src/test/resources/test-data/stats/ruleC/c_c/");

		final Collector c = new Collector(new WildcardFileFilter("*.tif"));
		final List<File> imageFileList = c.collect(imageFileDir);
		AggregatingRules aggregator = null;
		try {

			aggregator = new AggregatingRules(gauls, areas);

			final Iterator<File> it = imageFileList.iterator();
			while (it.hasNext()) {
				final File imageFile = it.next();
				try {
					Map<String, Double> map = GdalInfoExecutor
							.getMaxMinNoData(imageFile);
					double noData = map.get(GdalInfoExecutor.NODATA_KEY);
					double multipy = 1;
					// RuleB.ruleB(FileType.C, multipy,
					// runnungContextDir,imageFile, aggregator.getAreas(),
					// aggregator.getGauls(), noData,
					// aggregator.getNoDataAreas(),
					// aggregator.getNoDataGauls());

					RuleB.ruleBC(FileType.C, true, multipy, runnungContextDir,
							aggregator, imageFile, new double[] { noData, 0 });

				} catch (Exception e) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error(e.getLocalizedMessage(), e);
				} finally {
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			aggregator.dispose(true);
		}
	}

	@Test
	@Ignore
	public void TestB_c() throws Exception {
		JAI.getDefaultInstance().getTileCache()
				.setMemoryCapacity(768 * 1024 * 1024);

		final File runnungContextDir = new File(
				"src/test/resources/test-data/stats/ruleB/b_c/out/");
		if (runnungContextDir.exists())
			FileUtils.deleteDirectory(runnungContextDir);
		runnungContextDir.mkdirs();

		final File imageFileDir = new File(
				"src/test/resources/test-data/stats/ruleB/b_c/");

		final Collector c = new Collector(new WildcardFileFilter("*.tif"));
		final List<File> imageFileList = c.collect(imageFileDir);
		AggregatingRules aggregator = null;
		try {

			aggregator = new AggregatingRules(gauls, areas);

			final Iterator<File> it = imageFileList.iterator();
			while (it.hasNext()) {
				final File imageFile = it.next();
				try {
					final Map<String, Double> map = GdalInfoExecutor
							.getMaxMinNoData(imageFile);
					double noData = map.get(GdalInfoExecutor.NODATA_KEY);
					double multipy = 1;

					RuleB.ruleBC(FileType.C, false, multipy, runnungContextDir,
							aggregator, imageFile, new double[] { noData });

				} catch (Exception e) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error(e.getLocalizedMessage(), e);
				} finally {
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			aggregator.dispose(true);
		}
	}

	@Test
	public void TestB_p4() throws Exception {
		JAI.getDefaultInstance().getTileCache()
				.setMemoryCapacity(768 * 1024 * 1024);

		final File runnungContextDir = new File(
				"src/test/resources/test-data/stats/ruleB/b_p4/out/");
		if (runnungContextDir.exists())
			FileUtils.deleteDirectory(runnungContextDir);
		runnungContextDir.mkdirs();

		final File imageFileDir = new File(
				"src/test/resources/test-data/stats/ruleB/b_p4/");

		final Collector c = new Collector(new WildcardFileFilter("*.tif"));
		final List<File> imageFileList = c.collect(imageFileDir);
		AggregatingRules aggregator = null;
		try {

			aggregator = new AggregatingRules(gauls, areas);

			final Iterator<File> it = imageFileList.iterator();
			while (it.hasNext()) {
				final File imageFile = it.next();
				try {
					final Map<String, Double> map = GdalInfoExecutor
							.getMaxMinNoData(imageFile);
					double noData = map.get(GdalInfoExecutor.NODATA_KEY);
					double multipy = 1;

					RuleB.ruleBC(FileType.C, false, multipy, runnungContextDir,
							aggregator, imageFile, new double[] { noData });

				} catch (Exception e) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error(e.getLocalizedMessage(), e);
				} finally {
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			aggregator.dispose(true);
		}
	}

	final private static File[][] areasRationalized = new File[][] {
			{
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area0_1_ratio.tif"),
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area0_2_ratio.tif"),
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area0_3_ratio.tif"),
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area0_4_ratio.tif"), },
			{
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area1_1_ratio.tif"),
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area1_2_ratio.tif"),
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area1_3_ratio.tif"),
					new File(
							"src/test/resources/test-data/stats/areas_ratio/area1_4_ratio.tif"), } };

	/**
	 * 
	 * @Test
	 * @Ignore
	 * 
	 * @note: UPDATE gaezprj.data set status_code='TOK', status_msg='' where
	 *        file_name_tif= 'act2000_i_oce_2000_prd.tif' or file_name_tif=
	 *        'act2000_i_rt2_2000_prd.tif'or file_name_tif=
	 *        'act2000_i_sfl_2000_prd.tif'or file_name_tif=
	 *        'act2000_i_sfl_2000_yld.tif' or file_name_tif=
	 *        'act2000_r_oce_2000_prd.tif' or
	 *        file_name_tif='act2000_r_rt1_2000_prd.tif'or file_name_tif=
	 *        'act2000_t_cc1_2000_prd.tif'or file_name_tif=
	 *        'act2000_t_olv_2000_prd.tif' or file_name_tif=
	 *        'act2000_t_sfl_2000_prd.tif' or file_name_tif=
	 *        'act2000_t_veg_2000_prd.tif';
	 * 
	 *        SELECT * from gaezprj.data where file_name_tif=
	 *        'act2000_i_oce_2000_prd.tif' or file_name_tif=
	 *        'act2000_i_rt2_2000_prd.tif'or file_name_tif=
	 *        'act2000_i_sfl_2000_prd.tif'or file_name_tif=
	 *        'act2000_i_sfl_2000_yld.tif' or file_name_tif=
	 *        'act2000_r_oce_2000_prd.tif' or
	 *        file_name_tif='act2000_r_rt1_2000_prd.tif'or file_name_tif=
	 *        'act2000_t_cc1_2000_prd.tif'or file_name_tif=
	 *        'act2000_t_olv_2000_prd.tif' or file_name_tif=
	 *        'act2000_t_sfl_2000_prd.tif' or file_name_tif=
	 *        'act2000_t_veg_2000_prd.tif';
	 */
	public void TestD_c() throws Exception {
		new File("src/test/resources/test-data/stats/areas_ratio/").mkdirs();

		final File runnungContextDir = new File(
				"src/test/resources/test-data/stats/ruleD/c/out/");
		if (runnungContextDir.exists())
			FileUtils.deleteDirectory(runnungContextDir);
		runnungContextDir.mkdirs();

		final File imageFileDir = new File(
				"src/test/resources/test-data/stats/ruleD/c/");

		final Collector c = new Collector(new WildcardFileFilter("*.tif"));
		final List<File> imageFileList = c.collect(imageFileDir);
		try {
			final Iterator<File> it = imageFileList.iterator();
			while (it.hasNext()) {
				final File imageFile = it.next();
				AggregatingRules aggregator = null;
				try {

					if (!areasRationalized[0][0].exists()) {
						AreaRationalizer.produce(areasRationalized, areas,
								areasNoData);
					}

					aggregator = new AggregatingRules(gauls, areasRationalized);

					Map<String, Double> map = GdalInfoExecutor
							.getMaxMinNoData(imageFile);
					double noData = map.get(GdalInfoExecutor.NODATA_KEY);
					final double multiply = 1;

					RuleD.ruleD(aggregator, multiply, imageFile, noData,
							runnungContextDir);

				} catch (Exception e) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error(e.getLocalizedMessage(), e);
				} finally {
					if (aggregator != null)
						aggregator.dispose(true);
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {

		}
	}

}
