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
package it.geosolutions.geobatch.gaez.utils;

import java.io.IOException;

import org.junit.Test;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class RdcReaderTest {
	
	/**
	 * @param args
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {
		final String[] header = { "code_1", "code_2", "code_3" };
		/*
		File[] fileCsv = new File[] {
				new File(
						"src/main/resources/test-data/I0_cnt1_lr_soi_hwsd_domi.csv"),
				new File(
						"src/main/resources/test-data/I0_cnt2_lr_soi_hwsd_domi.csv"),
				new File(
						"src/main/resources/test-data/I0_cnt3_lr_soi_hwsd_domi.csv"),
				new File(
						"src/main/resources/test-data/I0_cnt4_lr_soi_hwsd_domi.csv") };
		final File outCsvSum = new File(
				"src/main/resources/test-data/outCsv.csv");
		CSVReader.writeCsv(fileCsv, outCsvSum);
		*/
		// readrdc(fileCsv, outCsvSum);
		// System.out.print(getCats(new File(
		// "/media/bigdisk/data/ciok/idrisi/et0_1960.rdc")));
		// System.out.print(getCats(new File(
		// "/media/bigdisk/data/ciok/GAEZ/faocmb00.rdc")));

		// final String line=buildRow(header, readrdc(new
		// File("/media/bigdisk/data/ciok/idrisi/et0_1960.rdc")), ",");
		// writeCSW(header, new String[]{line});
	}

}
