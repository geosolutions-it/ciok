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

import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import it.geosolutions.tools.freemarker.filter.FreeMarkerFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class GdalInfoExecutor {
	final static Logger LOGGER = LoggerFactory.getLogger(GdalInfoExecutor.class);

	protected static Document parser(String xmlString) {

		StringReader sr=null;
		try {
			sr=new StringReader(xmlString);
			final InputSource source = new InputSource(sr);
			final SAXBuilder builder = new SAXBuilder();
			final Document doc = builder.build(source);
			return doc;

		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		} catch (JDOMException e) {

			LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			 IOUtils.closeQuietly(sr);
		}
		return null;
	}

	public static org.w3c.dom.Document convertToDOM(org.jdom.Document jdomDoc) {
		DOMOutputter outputter = new DOMOutputter();
		try {
			return outputter.output(jdomDoc);
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * run the Gdalinfo un the rst file and build the csv in output using the
	 * passed freemarker template.
	 * 
	 * @param rst
	 *            the image to check
	 * @param template
	 *            the freemarker template
	 * @param csvOut
	 *            the output file
	 * @return the output file (if success) or null if fails.
	 * @throws Exception 
	 */
	public static File gdalinfoToCSV(final File rst, final File template,
			final File csvOut) throws Exception {

		final Document doc = gdalinfoToDocument(rst);
		return process(convertToDOM(doc), template, csvOut);
	}

	/**
	 * 
	 * @param rst
	 * @return
	 * @throws Exception 
	 */
	public static Document gdalinfoToDocument(final File rst) throws Exception {

		final String[] cmd = buildCommand(null, "gdalinfo",
				new String[] { rst.getAbsolutePath() });
		if (cmd == null) {
			return null;
		}
		final String res = exec(cmd);
		final String xml;

		LOGGER.info("GDALINFO: " + res);
		// System.out.print("GDALINFO: "+res);
		if (res != null) {
			int index=res.indexOf("<");
			if (index<0){
				// no "<" found
				return null;
			}
			xml = res.substring(index);
			if (xml.length() == 0) {
				LOGGER.error("Unable to get xml output from this rst file: "
						+ rst.getAbsolutePath());
				return null;
			}
		} else
			return null;

		final Document doc = parser(xml);
		return doc;
	}

	public final static String NODATA_KEY = "nodata";
	public final static String MAX_KEY = "max";
	public final static String MIN_KEY = "min";

	/**
	 * return a map including noData, Max and Min recalculating stats using the
	 * new nodata
	 * 
	 * @param image
	 * @return
	 */
	public static Map<String, Double> getMaxMinNoData(final File image) {
		final Map<String, Double> map = new HashMap<String, Double>();
		File vrt = null;
		try {

			// read the noData using a tiff reader
			Number noDataNumber = AggregatingRules.getNoData(image);
			// same thing but calling external process (this works also on
			// idrisi)
			if (noDataNumber == null) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error("Unable to read no data value");
				return null;
			}
			Double noData = noDataNumber.doubleValue();
			map.put(NODATA_KEY, noData);
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("NoData: " + noData);

			// the VRT file to generate
			vrt = new File(image.getParent(), FilenameUtils.getBaseName(image
					.getName()) + ".vrt");

			// build the gdal_translate command to generate VRT
			String command[] = buildCommand(
					null,
					"gdal_translate",
					new String[] { "-OF", "VRT", "-a_nodata",
							noData.toString(), image.getAbsolutePath(),
							vrt.getAbsolutePath() });
			if (command == null) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error("Unable to generate translate command");
				return null;
			}
			if (LOGGER.isDebugEnabled()) {
				for (int i = 0; i < command.length; i++) {
					LOGGER.debug("command-i: " + command[i]);
				}
			}

			// run gdal_translate to create a VRT file to apply the noData to
			// the image
			String ret = exec(command);
			if (ret == null) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error("Unable to run the translate command");
				return null;
			}

			// run gdalinfo on the VRT file recalculating min and max using the
			// retrieved noData
			command = buildCommand(null, "gdalinfo", new String[] { "-stats",
					"-mm", vrt.getAbsolutePath() });
			if (command == null) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error("Unable to generate gdalinfo command");
				return null;
			}
			ret = exec(command);

			if (ret != null) {
				// also match the nodata
				// " *Min=[0-9\\.]+ *Max=[0-9\\.]+ +Computed +Min/Max=([0-9\\.]+),([0-9\\.]+)| *NoData Value=([0-9\\.]+)"
				final Pattern p = Pattern
						.compile(
								" *.+ +Computed +Min/Max=([0-9\\.]+),([0-9\\.]+)",
								Pattern.MULTILINE);
				final Matcher m = p.matcher(ret);
				String max = null, min = null;
				while (m.find()) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("match: " + m.group());
					}

					if (min == null){
						min = m.group(1);
						map.put(MIN_KEY, Double.parseDouble(min));
					}
					if (max == null){
						max = m.group(2);
						map.put(MAX_KEY, Double.parseDouble(max));
					}
				}
				if (max == null || min==null){
					if (LOGGER.isWarnEnabled())
						LOGGER.warn("Unable to read Max or Min using parsing:\n"+ret);
				}
			}
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
			return null;
		} finally {
			// if (vrt!=null)
			// vrt.deleteOnExit();
		}
		return map;
	}

	/**
	 * Execute an external process to Retrieve the value of the pixel 0,0 (as
	 * Double) reading the passed image file
	 * 
	 * @param file
	 * @return Double
	 * @throws Exception
	 */
	public static Double gdallocationinfo(final String file) throws Exception {
		if (file != null) {
			if (!new File(file).exists()) {
				throw new Exception("Passed file doesn't exist");
			}
		} else {
			throw new NullPointerException("Passed file doesn't exist");
		}

		final String s = exec(new String[] { "gdallocationinfo", file, "0", "0" });

		final String[] res = s.split("Value: ");
		if (res.length != 2) {
			throw new Exception("Unable to parse gdallocationinfo result");
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("NoData for file " + file + " is: " + res[1]);
		}
		return Double.parseDouble(res[1]);
	}

	public static File process(final org.w3c.dom.Document element,
			final File template, final File csvOut) {
		FileReader fr = null;
		FreeMarkerFilter f = null;
		FileWriter fw = null;
		try {
			fr = new FileReader(template);

			f = new FreeMarkerFilter(template.getParent(), fr);

			fw = new FileWriter(csvOut);

			final TemplateModel model = f.wrapRoot(element);

			if (model == null)
				LOGGER.error("Unable to wrap object " + element);
			else
				LOGGER.info("Object succesfully wrapped: " + element);

			f.process(model, fw);

			return csvOut;
		} catch (NullPointerException e) {
//			System.out.println(e.getLocalizedMessage());
			LOGGER.error(e.getLocalizedMessage(), e);
		} catch (TemplateModelException e) {
//			System.out.println(e.getLocalizedMessage());
			// TODO Auto-generated catch block
			LOGGER.error(e.getLocalizedMessage(), e);
		} catch (TemplateException e) {
//			System.out.println(e.getLocalizedMessage());
			// TODO Auto-generated catch block
			LOGGER.error(e.getLocalizedMessage(), e);
		} catch (FileNotFoundException e) {
//			System.out.println(e.getLocalizedMessage());
			// TODO Auto-generated catch block
			LOGGER.error(e.getLocalizedMessage(), e);
		} catch (IOException e) {
//			System.out.println(e.getLocalizedMessage());
			// TODO Auto-generated catch block
			LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			try {
				if (fr != null) {
					fr.close();
				}
				if (fw != null) {
					fw.flush();
					fw.close();
				}
			} catch (IOException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
		return null;

	}

	/**
	 * @param path
	 *            the optional path to look for command exec (optional == null)
	 * @param exec
	 *            the command to run
	 * @param opts
	 *            options to pass to command exec (optional == null)
	 * @return to command
	 */
	public static String[] buildCommand(final String path, final String exec,
			final String opts[]) {
		final String[] command;
		final int size;

		if (exec == null) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error("Unable to find the command to execute using: PATH:"
						+ path + " exec:" + exec);
			return null;
		}

		if (opts != null) {
			// gdalinfo opts[0] ... opts[n-1] file
			size = opts.length + 1;
		} else {
			// gdalinfo file
			size = 1;
		}
		command = new String[size];
		if (path != null) {
			command[0] = path + exec;
			if (!new File(command[0]).exists()) {
				if (LOGGER.isWarnEnabled())
					LOGGER.warn("Unable to find the command to execute using: PATH:"
							+ path
							+ " exec:"
							+ exec
							+ "\n Trying to run command from the env:PATH");
				command[0] = exec;
			}
		} else {
			command[0] = exec;
		}
		int i = 1;
		for (int j = 0; i < size; i++, j++) {
			command[i] = opts[j];
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Appending option: " + opts[j]);
		}

		if (LOGGER.isInfoEnabled())
			LOGGER.info("COMMAND: " + command.toString());

		return command;
	}

	/**
	 * execute gdalinfo and return the output string (if success) or null if
	 * error (see log)
	 * 
	 * @param command
	 * @return
	 * @throws Exception 
	 */
	public static String exec(final String[] command) throws Exception {
		final ProcessBuilder pb = new ProcessBuilder(command);
		BufferedReader input = null;
		BufferedReader output = null;
		try {
			final Process proc = pb.start();

			// read error messages
			String errorsLine;
			input = new BufferedReader(new InputStreamReader(
					proc.getErrorStream()));
			final StringBuffer errors = new StringBuffer();
			try {
				while ((errorsLine = input.readLine()) != null){
					errors.append(errorsLine);
					if (LOGGER.isErrorEnabled())
						LOGGER.error(errorsLine);
				}
			} catch (IOException e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(),e);
				throw e;
			} finally {
				try {
					if (input != null)
						input.close();
				} catch (Throwable t) {
				}
			}

			// output
			output = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			final StringBuffer stdout = new StringBuffer();
			String outputLine;
			try {

				while ((outputLine = output.readLine()) != null)
					stdout.append(outputLine);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error(e.getLocalizedMessage(), e);
			} finally {
				try {
					if (output != null)
						output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					LOGGER.error(e.getLocalizedMessage(), e);
				}
			}

			final int result = proc.waitFor();

			if (checkStatus(result))
				return stdout.toString();
			else {
				LOGGER.error(errors.toString());
				return null;
			}

		} catch (Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
			throw e;
		} finally {

		}
	}

	/**
	 * if status is good returns true
	 * 
	 * @param ret
	 * @return
	 */
	public static boolean checkStatus(final int ret) {
		switch (ret) {
		case 0:
			return true; // OK
		default:
			return false;
		}

	}

}
