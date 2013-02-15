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

import it.geosolutions.tools.io.file.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public abstract class RdcReader {
	/**
	 * 
	 * @param rdc
	 * @return
	 */
	public static Map<String, String> readRdc(final File rdc) {
		Map<String, String> ret = new HashMap<String, String>();
		FileReader fr;
		try {
			fr = new FileReader(rdc);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		BufferedReader reader = new BufferedReader(fr);

		String line;
		try {
			line = reader.readLine();
			while (line != null) {
				final String[] values = line.split(" *: *");

				if (values.length == 2) {
					System.out.println(values[0] + " : " + values[1]);
					ret.put(values[0].replaceAll(" ", "_"), values[1]);

				} else
					System.out.println("Error!!!");
				line = reader.readLine();
			}
			return ret;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * @deprecated unused
	 * @throws IOException
	 */
	void testProps() throws IOException {
		Properties p = new Properties();
		FileInputStream is = new FileInputStream(new File("src/faocmb00.rdc"));
		p.load(is);
		Set<Entry<Object, Object>> set = p.entrySet();
		Iterator<Entry<Object, Object>> it = set.iterator();
		while (it.hasNext()) {
			Entry e = it.next();
			System.out.println(e.getKey() + " : " + e.getValue());
			// System.out.println("KEY:\t"+e.getKey());
			// System.out.println("VAL:\t"+e.getValue());
		}
		is.close();
	}

	/**
	 * @deprecated unused
	 * @param header
	 * @param values
	 * @param separator
	 * @return
	 */
	public static String buildRow(final String[] header,
			final Map<String, String> values, final String separator) {
		final StringBuilder row = new StringBuilder();
		for (final String column_key : header) {
			final String value = values.get(column_key);
			if (value != null) {
				row.append(value);
			}
			// else
			// row.append("NULL");
			row.append(separator);
		}
		row.append('\n');
		return row.toString();
	}

	/**
	 * 
	 * @param fileCsv
	 * @param outCsvSum
	 * @return TODO write
	 * @deprecated unused
	 */
	public static boolean readrdc(final File[] fileCsv, final File outCsvSum) {
		FileReader[] fr = new FileReader[fileCsv.length];
		BufferedReader[] br = new BufferedReader[fileCsv.length];
		for (int i = 0; i < fileCsv.length; i++) {
			File csv = fileCsv[i];
			try {
				fr[i] = new FileReader(csv);
				br[i] = new BufferedReader(fr[i]);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				while (i >= 0) {
					IOUtils.closeQuietly(fr[i]);
					IOUtils.closeQuietly(br[i--]);
				}
				return false;
			} finally {

			}
		}

		try {
			// read header and calculate column size
			String line[] = new String[fileCsv.length];
			int colSize = -1;
			for (int i = 0; i < fileCsv.length; i++) {
				line[i] = br[i].readLine();
				if (line != null && colSize == -1) {
					colSize = line[i].split(",").length;
				}
			}
			// read other lines

			while (line[0] != null) {
				Object outRow[] = new Object[colSize];
				for (int i = 0; i < fileCsv.length; i++) {

					line[i] = br[i].readLine();
					final String[] values = line[i].split(",");
					for (int col = 0; col < colSize; col++) {
						String val = values[col];
						try {
							if (outRow[col] != null)
								outRow[col] = Double.parseDouble(val)
										+ ((Double) outRow[col]);
							else
								outRow[col] = Double.parseDouble(val);
						} catch (NumberFormatException e) {
							outRow[col] = val;
						}
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			for (int i = 0; i < fileCsv.length; i++) {
				IOUtils.closeQuietly(fr[i]);
				IOUtils.closeQuietly(br[i]);
			}
		}
		return true;
	}

}
