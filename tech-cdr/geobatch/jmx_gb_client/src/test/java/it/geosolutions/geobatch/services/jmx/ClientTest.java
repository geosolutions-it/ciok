/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.codehaus.org/
 *  Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.services.jmx;

import java.io.File;
import java.util.Map;

import javax.management.remote.JMXConnector;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * 
 * existgb="true" mvn clean install -o
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class ClientTest {

	private final static Logger LOGGER = Logger.getLogger(ClientTest.class);

	private static boolean enabled = false;

	static {

		// These tests will destroy data, so let's make sure we do want to run
		// them
		enabled = getenv("existgb", "false").equalsIgnoreCase("true");
		if (!enabled)
			LOGGER.warn("Tests are disabled. Please read the documentation to enable them.");
	}

	private static String getenv(String envName, String envDefault) {
		String env = System.getenv(envName);
		String ret = System.getProperty(envName, env);
		LOGGER.debug("env var " + envName + " is " + ret);
		return ret != null ? ret : envDefault;
	}

	@Test
	public void testLoadEnv() throws Exception {
		Assert.assertNotNull(JMXClientUtils.loadEnv(new ClassPathResource(
				"jmx.properties").getFile().getAbsolutePath()));
	}

	@Test
	public void testConnector() throws Exception {
		if (!enabled) {
			return;
		}

		File envFile = new ClassPathResource("jmx.properties").getFile();
		Map env = JMXClientUtils.loadEnv(envFile.getAbsolutePath());
		Assert.assertNotNull(env);
		if (enabled) {
			JMXConnector c = JMXClientUtils.getConnector(env);
			Assert.assertNotNull(c);

			ServiceManager proxy = JMXClientUtils.getProxy(env,c);
			Assert.assertNotNull(proxy);

			// calling remote process
			ConsumerManager consumer=null;
			try {
				
				consumer=new JMXConsumerManager(env, proxy);
				
				consumer.run(JMXAsynchConsumer.getEvent(env));
				
				while (true) {
					ConsumerStatus status = consumer.getStatus();
					Thread.sleep(1000);
					// checking
					// waiting or running or completed
					if (status == ConsumerStatus.EXECUTING
							|| status == ConsumerStatus.IDLE
							|| status == ConsumerStatus.PAUSED
							|| status == ConsumerStatus.WAITING
							|| status == ConsumerStatus.COMPLETED) {
						// no more process execution
						Assert.assertTrue(true);
						break;
					}
					// failed
					if (status == ConsumerStatus.FAILED
							|| status == ConsumerStatus.CANCELED) {
						// no more process execution
						Assert.assertFalse(true);
						break;
					}
				}
			} catch (Exception e) {
				LOGGER.error(e);
			} finally {
				if (consumer != null) {
					consumer.dispose();
				}
			}
		}

	}

}
