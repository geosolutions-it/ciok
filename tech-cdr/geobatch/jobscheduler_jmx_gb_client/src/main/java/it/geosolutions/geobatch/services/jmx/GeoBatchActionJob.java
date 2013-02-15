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

import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;

import sos.spooler.Job_impl;
import sos.spooler.Variable_set;

/**
 * JobScheduler job implementation to call GeoBatch Action via JMX
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class GeoBatchActionJob extends Job_impl {

    public final static String CONFIG_FILE_KEY = "CONFIG_FILE";
    public final static String CONFIG_FILE = "jmx.properties";

    private Long pollingDelay = null;

    private Map<String,String> environment = null;

    private JMXConnector jmxc = null;
    
    private ActionManager proxy = null;

    /**
     * @return the environment
     */
    public final Map<String,String> getEnvironment() {
        return environment;
    }

    @Override
    public void spooler_close() throws Exception {
        spooler_log.info("ON CLOSE ");
        if (proxy != null) {
            
            if (jmxc == null)
            	spooler_log.error("Unable to close connection, please, connect before");
            else
            	jmxc.close();
        }
    }

    @Override
    public boolean spooler_init() throws Exception {
        spooler_log.info("ON INIT");
        spooler_job.set_state_text("STARTED");
//        if (System.getSecurityManager() == null) {
//            System.setSecurityManager(new RMISecurityManager());
//            // TODO check remote bean creation and other tasks
//        }

        final Variable_set varSet = spooler_task.params();

//         Process delay secs
        final String delay = varSet.value(JMXClientUtils.PROCESS_DELAY_KEY);
        if (delay != null) {
            try {
                pollingDelay = new Long(delay);
            } catch (NumberFormatException nfe) {
                pollingDelay = JMXClientUtils.PROCESS_DELAY_DEFAULT;
                spooler_log.error("Failed to parse double value: " + delay + ". Message:"
                                  + nfe.getLocalizedMessage());
            }
        } else {
            spooler_log.warn("No delay is set for process, using default (secs):" + JMXClientUtils.PROCESS_DELAY_DEFAULT);
            pollingDelay = JMXClientUtils.PROCESS_DELAY_DEFAULT;
        }
        spooler_log.info("Polling delay is set to: " + pollingDelay);
        
        
        // spooler_task.set_delay_spooler_process(pollingDelay);
        // spooler_task.set_repeat(pollingDelay);

        // Client defaults
        final String configFilePath = varSet.value(CONFIG_FILE_KEY);
        try {
            // build the client
            environment=JMXClientUtils.loadEnv(configFilePath);
            return true;
        } catch (Exception e) {
            spooler_log.debug(e.getLocalizedMessage());
        }

        // failure
        return false;
    }

    @Override
    public void spooler_on_error() throws Exception {
        spooler_log.error("ERROR OCCURRED");
        spooler_task.set_exit_code(1);
    }

    @Override
    public void spooler_on_success() throws Exception {
        spooler_log.info("SUCCESS OCCURRED");
    }

    @Override
    public void spooler_exit() throws Exception {
        spooler_log.info("ON EXIT");
    }

    @Override
    public boolean spooler_open() throws Exception {
        spooler_log.info("ON OPEN");

        try {

            spooler_log.info("Getting proxy....");

            jmxc=JMXClientUtils.getConnector(getEnvironment());
            
            if (jmxc == null) {
            	spooler_log.error("Failed to connect");
                // failure
                return false;
            }
            spooler_log.info("Connection established");
            
            // get the proxy object
            proxy = JMXClientUtils.getProxy(getEnvironment(),jmxc);
            
            // if success
            if (proxy == null) {
            	spooler_log.error("Failed to get a proxy");
                // failure
                return false;
            }
            spooler_log.info("Proxy successfully obtained");
            return true;
        } catch (Exception e) {
            spooler_log.error(e.getLocalizedMessage());
            throw e;
        }
    }

    @Override
    public boolean spooler_process() throws Exception {
    	
        boolean waiting = false;
        String processUUID = null;
        String serviceID = null;
        
        serviceID=spooler_task.params().value(JMXServiceManager.SERVICE_ID_KEY);
        if (serviceID==null){
        	spooler_log.error("Unable to get the service id");
        	return false;
        }
        
        spooler_log.info("ON PROCESS: "+serviceID);
        // start remote job/action using JMX client
        
        // creating map params
        final Map<String, String> env = new HashMap<String, String>();
        final String keyList = spooler_task.params().names();
        final String[] keys = keyList.split(";");
        for (String key : keys) {
            spooler_log.info("adding key:" + key + " value:" + spooler_task.params().value(key));
            env.put(key, spooler_task.params().value(key));
        }

        // calling remote process
        spooler_log.info("calling action");
        spooler_job.set_state_text("PROCESSING using: "+serviceID);
        try {
	        processUUID = proxy.callAction(env);
                if (processUUID == null) {
                    // failure
                    spooler_log.info("Too may executions, please dispose some consumer.");
                    // spooler_task.set_result("EXIT status is: "+statusInt);
                    // spooler_task.set_error("Some problem into action execution, please reference to the GeoBatch log");
                    spooler_job.set_state_text("FAIL");
                    spooler_task.set_exit_code(1);
                    return false; // need more process execution
                }
	        // final String uuid = (String)mbsc.invoke(mbeanName, methodName,
	        // new Object[] {params}, signature);
	
	        // process is started
	        waiting = true;
	        
	        while (waiting){
		        // waiting for status
		        Thread.sleep(this.pollingDelay);
		
		        spooler_log.info("Getting status from GeoBatch for UUID: " + processUUID+" ServiceID: "+serviceID);
		
		        // int ret = (Integer)mbsc.invoke(mbeanName, "getStatus", new Object[]
		        // {uuid}, new String[]{String.class.getName()});
		
		        ConsumerStatus status = proxy.getStatus(processUUID);
		
		        // check result
		        if (status == ConsumerStatus.EXECUTING || status == ConsumerStatus.IDLE
		            || status == ConsumerStatus.PAUSED || status == ConsumerStatus.WAITING) {
		            
		            // pending
		            spooler_log.info("WAITING while status is: " + status);
		            spooler_job.set_state_text(status.toString());
		            
		        } else if (status == ConsumerStatus.COMPLETED) {
		            // success
		            spooler_log.info("EXIT status is: " + status);
		            // spooler_task.set_result("EXIT status is: "+statusInt);
		            spooler_job.set_state_text(status.toString());
		            spooler_task.set_exit_code(0);
		            waiting=false; // no more process execution
		        } else if (status == ConsumerStatus.FAILED || status == ConsumerStatus.CANCELED) {
		            // failure
		            spooler_log.info("EXIT status is: " + status);
		            // spooler_task.set_result("EXIT status is: "+statusInt);
		            // spooler_task.set_error("Some problem into action execution, please reference to the GeoBatch log");
		            spooler_job.set_state_text(status.toString());
		            spooler_task.set_exit_code(1);
		            waiting=false; // no more process execution
		        } else { // status == ConsumerStatus.UNRECOGNIZED
		            // failure
		            spooler_log.info("EXIT status is: " + status);
		            spooler_job.set_state_text(status.toString());
		            spooler_task.set_exit_code(1);
		            waiting=false; // no more process execution
		        }
	        }
        } finally {
	        // dispose remote consumer
                spooler_log.info("DISPOSE CLIENT: " + serviceID);
                if (proxy != null && processUUID != null) {
                    try {
                        // dispose remote GeoBatch consumer instance
                        proxy.disposeAction(processUUID);
                    } catch (Exception e) {
                        spooler_log.error(e.getLocalizedMessage());
                    }
                }
        }
        return false; // no more process execution
    }

}
