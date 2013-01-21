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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to provide read/write/update Database functions to the GAEZ
 * flow
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class DataStoreHandlerTest {

	/**
	 * Status code of the Idrisi Raster file.<br>
	 * Default value is ‘RDY’.<br>
	 * This field must be updated by the process.<br>
	 * Allowed values are:<br>
	 * RDY - ready to be processed<br>
	 * LCK - locked by other process (in parallelized computing)<br>
	 * OK - file successfully processed<br>
	 * KO - file processed with errors<br>
	 */
	private final static String STATUS_KEY = "status_code";

	/**
	 * Default logger
	 */
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(DataStoreHandlerTest.class);

	/**
	 * TODO JUNIT tests
	 * 
	 * @param args
	 * @throws IOException
	 */
	@Test
	public void test() throws IOException {

		FileInputStream fis = null;
		try {

			jdbcUpdateTest("src/main/resources/jdbc.properties");

		} catch (FileNotFoundException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
			Assert.fail(e.getLocalizedMessage());
		} catch (Throwable e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
			Assert.fail(e.getLocalizedMessage());
		} finally {
			if (fis != null)
				IOUtils.closeQuietly(fis);
		}
	}

	public void jdbcUpdateTest(final String propPath) throws IOException {

		final File dataStorePropFile = new File(propPath);
		final Properties dataStoreProp = new Properties();

		FileInputStream fis = null;
		ThreadPoolExecutor ex = null;
		try {
			fis = new FileInputStream(dataStorePropFile);
			dataStoreProp.load(fis);

			FutureTask<List<Map<String, Object>>> a = new FutureTask<List<Map<String, Object>>>(
					new QueryTest(dataStoreProp));
			FutureTask<List<Map<String, Object>>> b = new FutureTask<List<Map<String, Object>>>(
					new QueryTest(dataStoreProp));
			FutureTask<List<Map<String, Object>>> c = new FutureTask<List<Map<String, Object>>>(
					new QueryTest(dataStoreProp));
			FutureTask<List<Map<String, Object>>> d = new FutureTask<List<Map<String, Object>>>(
					new QueryTest(dataStoreProp));
			FutureTask<List<Map<String, Object>>> e = new FutureTask<List<Map<String, Object>>>(
					new QueryTest(dataStoreProp));

			ex = new ThreadPoolExecutor(10, 10, 10000L, TimeUnit.SECONDS,
					new ArrayBlockingQueue<Runnable>(10));

			if (LOGGER.isInfoEnabled())
				LOGGER.info("Running A-E");

			ex.execute(a);
			ex.execute(b);
			ex.execute(c);
			ex.execute(d);
			ex.execute(e);

			final List<Map<String, Object>> aList = a.get();
			final List<Map<String, Object>> bList = b.get();
			final List<Map<String, Object>> cList = c.get();
			final List<Map<String, Object>> dList = d.get();
			final List<Map<String, Object>> eList = e.get();

			Iterator<Map<String, Object>> aIt = aList.iterator();
			while (aIt.hasNext()) {
				// junit.framework.Assert.assertFalse();
				Map<String, Object> map = aIt.next();
				if (bList.contains(map)) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error("NOT OK");
					Assert.fail("the map from the list a is also present into b list");
				}
				if (cList.contains(map)) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error("NOT OK");
					Assert.fail("the map from the list a is also present into c list");
				}
				if (dList.contains(map)) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error("NOT OK");
					Assert.fail("the map from the list a is also present into d list");
				}
				if (eList.contains(map)) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error("NOT OK");
					Assert.fail("the map from the list a is also present into d list");
				}
			}

		} catch (FileNotFoundException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} catch (Throwable e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			if (fis != null)
				IOUtils.closeQuietly(fis);
			if (ex != null)
				ex.shutdown();
		}
		return;
	}

	private class QueryTest implements Callable<List<Map<String, Object>>> {

		Properties dataStoreProp;

		public QueryTest(Properties dataStoreProp) {
			this.dataStoreProp = dataStoreProp;

		}

		public List<Map<String, Object>> call() {
			if (LOGGER.isInfoEnabled())
				LOGGER.info("RUNNING:" + Thread.currentThread().getId());

			List<Map<String, Object>> listOfmap3;
			try {
				listOfmap3 = DataStoreHandler.select4UpdatePrepStat(
						dataStoreProp, 1, STATUS_KEY + "='RDY' AND IGNORE=1",
						STATUS_KEY + "='LCK'");
				if (LOGGER.isInfoEnabled())
					LOGGER.info("Returned list size is " + listOfmap3.size());

				if (listOfmap3!=null){
					Iterator<Map<String, Object>>it=listOfmap3.iterator();
					while (it.hasNext()){
						Map<String, Object> map=it.next();
						map.put(DataStoreHandlerTest.STATUS_KEY, "RDY");
					}
				}
				
				DataStoreHandler.jdbcUpdateStatus(dataStoreProp, listOfmap3);
				
				if (LOGGER.isInfoEnabled())
					LOGGER.info("Returning from thread :"
							+ Thread.currentThread().getId());

				return listOfmap3;

			} catch (Throwable e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			}
			return null;
		}
	}

}
