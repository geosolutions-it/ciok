package it.geosolutions.geobatch.gaez.utils.rules.ruleA;

import it.geosolutions.geobatch.gaez.utils.rules.ruleA.CSVUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class CSVUtilsTest {
	
	/**
	 * @param args
	 * @throws IOException
	 * @Deprecated
	 */
	@Test
	public void test() throws IOException {
		File[] fileCsv = new File[] {
				new File(
						"src/test/resources/test-data/stats/ruleA/mergeCSV/I0_cnt1_lr_soi_hwsd_domi.csv"),
				new File(
						"src/test/resources/test-data/stats/ruleA/mergeCSV/I0_cnt2_lr_soi_hwsd_domi.csv"),
				new File(
						"src/test/resources/test-data/stats/ruleA/mergeCSV/I0_cnt3_lr_soi_hwsd_domi.csv"),
				new File(
						"src/test/resources/test-data/stats/ruleA/mergeCSV/I0_cnt4_lr_soi_hwsd_domi.csv") };
		final File outCsvSum = new File(
				"src/test/resources/test-data/stats/ruleA/mergeCSV/outCsv.csv");
		
		CSVUtils.mergeCsv(fileCsv, outCsvSum);
		
		Assert.assertTrue(outCsvSum.exists());
	}

}

