package it.geosolutions.geobatch.services.jmx;

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.util.Assert;

/**
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

	private ActionManager proxy = null;
	private String processUUID = null;
	private JMXConnector jmxc = null;

	@Test
	public void testCallAction() throws Exception {
		
		if (!enabled){
			return;
		}

		Map<String, String> params = new HashMap<String, String>();


		// if (System.getSecurityManager() == null) {
		// System.setSecurityManager(new RMISecurityManager());
		// // TODO check remote bean creation and other tasks
		// }

		try {
			// build the client
			if (!params.containsKey(JMXClientUtils.GB_URL_KEY))
				params.put(JMXClientUtils.GB_URL_KEY,
						JMXClientUtils.GB_URL);
			if (!params.containsKey(JMXClientUtils.GB_PORT_KEY))
				params.put(JMXClientUtils.GB_PORT_KEY,
						JMXClientUtils.GB_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get the proxy object
		if (params == null)
			throw new IllegalStateException(
					"Unable to connect with a null params, please, call init()");

		jmxc = JMXClientUtils.getConnector(params);
		Assert.notNull(jmxc);
		
		if (!params.containsKey(JMXClientUtils.GB_JMXBEAN_KEY))
			params.put(JMXClientUtils.GB_JMXBEAN_KEY,
					JMXClientUtils.GB_JMXBEAN_NAME);

		// // Create a dedicated proxy for the MBean instead of
		// // going directly through the MBean server connection

		proxy = JMXClientUtils.getProxy(params, jmxc);

		Assert.notNull(proxy);

		// form properties env to map env
		params.clear();

		params.put("SERVICE_ID", "CollectorGeneratorService");

		params.put("INPUT", "./");
//
//		params.put("configDir", "./");
		params.put("wildcard", "*.*");
		params.put("deep", "1");
		
		if (!params.containsKey(JMXClientUtils.PROCESS_DELAY_KEY))
			params.put(JMXClientUtils.PROCESS_DELAY_KEY,
					JMXClientUtils.PROCESS_DELAY_DEFAULT.toString());
		
		// if success
		Assert.isTrue(process(params));
	}

	boolean process(Map<String, String> env) throws Exception {

		try {
			// calling remote process
			processUUID = proxy.callAction(env);

			while (true) {
				// waiting for status
				Thread.sleep(Math.round(1000));

				ConsumerStatus status = proxy.getStatus(processUUID);

				// check result
				if (status == ConsumerStatus.COMPLETED) {
					// success
					return true; // no more process execution
				} else if (status == ConsumerStatus.EXECUTING
						|| status == ConsumerStatus.IDLE
						|| status == ConsumerStatus.PAUSED
						|| status == ConsumerStatus.WAITING) {
					// pending
					continue;
				} else if (status == ConsumerStatus.FAILED
						|| status == ConsumerStatus.CANCELED) {
					// failure
					return false; // no more process execution
				} else { // status == ConsumerStatus.UNRECOGNIZED
					// failure
					return false; // no more process execution
				}

			}
		} catch (Exception e) {
			LOGGER.error(e);
			return false;
		} finally {
			if (processUUID != null)
				proxy.disposeAction(processUUID);
		}
	}
}
