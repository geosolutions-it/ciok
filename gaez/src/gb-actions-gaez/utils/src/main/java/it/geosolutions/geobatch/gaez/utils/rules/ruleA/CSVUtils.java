package it.geosolutions.geobatch.gaez.utils.rules.ruleA;

import it.geosolutions.tools.io.file.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CSVUtils {

	/**
		 * 
		 * @param fileCsv
		 * @param outCsvSum
		 * @return TODO write
		 */
		public static boolean mergeCsv(final File[] fileCsv, final File outCsvSum) {
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
				String header = null;
				int colSize = -1;
				for (int i = 0; i < fileCsv.length; i++) {
					line[i] = br[i].readLine();
					if (header == null && colSize == -1 && line != null) {
						header = line[i];
						colSize = header.split(",").length;
					} else if (!header.equals(line[i])) { // check for equals header
						return false;
					}
				}
				// read other lines
				final Map<String, Object[]> rows = new HashMap<String, Object[]>();
				
				final int keySize;
				final String regex;
				
				if (header.equals("GAEZ_ID,GAUL_CODE,CATEGORY_CODE,CATEGORY_DESCRIPTION,COUNT,AREA")){
	//				GAEZ_ID,GAUL_CODE,CATEGORY_CODE,CATEGORY_DESCRIPTION,COUNT,AREA
					keySize=3;
					regex="([^ ,]*,[+-.0-9]*),(.*)";
				} else {
	//				GAEZ_ID,GAUL_CODE,COUNT,MIN,MAX,RANGE,MEAN,STD,SUM
					keySize=2;
					regex="([^ ,]*,[+-.0-9]+),([0-9]*,.*)";
				}
				
				final int valuesSize = colSize - keySize;
				final Pattern p = Pattern.compile(regex);
	
				for (int i = 0; i < fileCsv.length; i++) {
					while ((line[i] = br[i].readLine()) != null) {
						final Matcher m = p.matcher(line[i]);
	
						if (!m.matches()){
							throw new IllegalArgumentException(
									"line: "+line[i]+" do not matches.");
						}
						
						// final String
						// readLine[]=line[i].split("[^ ,]*,[+-.0-9]*,[+-.0-9]*,");
						if (m.groupCount() != 2)// readLine.length
							throw new IllegalArgumentException(
									"Group count is bad: " + m.groupCount()
											+ " line: " + line[i]);
	
						final String key = m.group(1);
						final String vals = m.group(2);

						Object outRow[];
						if ((outRow = rows.get(key)) == null) {
							outRow = new Object[valuesSize];
	
							String[] values = vals.split(",(?=( *.))(?!(.*\"))");
							
							if (valuesSize!=values.length){
								throw new Exception("Error parsing line: "+values);
							}
	
							for (int col = 0; col < valuesSize; col++) {
								String val = values[col];
								try {
									outRow[col] = Double.parseDouble(val);
								} catch (NumberFormatException e) {
									outRow[col] = val;
								}
							}
	
							rows.put(key, outRow);
						} else {
							// outRow
							String[] values = vals.split(",(?=( *.))(?!(.*\"))");
	
							if (valuesSize!=values.length){
								throw new Exception("Error parsing line: "+values);
							}
							
							for (int col = 0; col < valuesSize; col++) {
								String val = values[col];
								try {
									if (outRow[col] != null
											&& outRow[col] instanceof Number)
										outRow[col] = Double.parseDouble(val)
												+ ((Double) outRow[col]);
									else
										outRow[col] = Double.parseDouble(val);
								} catch (NumberFormatException e) {
									outRow[col] = val;
								}
							}
	
							rows.put(key, outRow);
						}
					}// while (rows)
				}// for files
	
				CSVUtils.writeCSW(outCsvSum, header, rows);
	
			} catch (Throwable e) {
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

	/**
	 * @param header
	 * @param lines
	 */
	public static void writeCSW(final File outCsv, final String header,
			final Map<String, Object[]> lines) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outCsv);
			// final StringBuilder head_row = new StringBuilder();
			// for (final String column_key : header) {
			// head_row.append(column_key);
			// head_row.append(",");
			// }
			// writer.append(head_row.toString());
			writer.append(header);
			writer.append('\n');
			writer.flush();
			Set<String> keys = lines.keySet();
			final Set<Map.Entry<String, Object[]>> set = lines.entrySet();
			final Iterator<Map.Entry<String, Object[]>> it = set.iterator();
			while (it.hasNext()) {
				Map.Entry<String, Object[]> row = it.next();
				writer.append(row.getKey());
				writer.append(",");
				final Object[] vals = row.getValue();
				int size = vals.length;
				for (int i = 0; i < size - 1; i++) {
					writer.append(vals[i].toString());
					writer.append(",");
				}
				writer.append(vals[size - 1].toString());
				writer.append('\n');
				writer.flush();
			}
			// for (final String row : lines) {
			// writer.append(row);
			// writer.flush();
			// }
	
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	
	}
	
	
	private class Row implements Comparable<Row> {

		public int gaulCode;
		public int categoryCode;

		public Row(int g, int c) {
			gaulCode = g;
			categoryCode = c;
		}

		public int compareTo(Row o) {
			if (gaulCode == o.gaulCode && categoryCode == o.categoryCode) {
				return 0;
			} else if (gaulCode > o.gaulCode && categoryCode > o.categoryCode) {
				return 1;
			} else {
				return -1;
			}

		}

	}

}

