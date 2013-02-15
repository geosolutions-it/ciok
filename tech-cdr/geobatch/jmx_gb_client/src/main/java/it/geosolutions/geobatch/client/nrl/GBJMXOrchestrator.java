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
package it.geosolutions.geobatch.client.nrl;

import it.geosolutions.geobatch.services.jmx.ConsumerManager;
import it.geosolutions.geobatch.services.jmx.JMXClientUtils;
import it.geosolutions.geobatch.services.jmx.JMXCumulatorListener;
import it.geosolutions.geobatch.services.jmx.JMXTaskRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class GBJMXOrchestrator {

    private final static Logger LOGGER = LoggerFactory.getLogger(GBJMXOrchestrator.class);

    /**
     * USAGE:<br>
     * java it.geosolutions.geobatch.services.jmx.MainJMXClientUtils /PATH/TO/FILE.properties<br>
     * where FILE.properties is the command property file<br>
     * 
     * @param argv a String[0] containing the path of the environment used to run the action on GeoBatch
     * @throws Exception
     */
    public static void main(String[] argv) throws Exception {
        
        if (argv.length < 1) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unable to run without a property file.");
            }
            System.exit(1);
        }
        final String path = argv[0];
        File envFile = new File(path);
        if (!envFile.isFile()) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unable to run without a property file, check the path: " + path);
            }
            System.exit(1);
        }

        // building the environment
        final Map<String, String> commonEnv = JMXClientUtils.loadEnv(argv[0]);

        JMXTaskRunner<ConsumerManager> runner = new ConsumerRunner(commonEnv);
        
        final List<ConsumerManager> retSuccess = new ArrayList<ConsumerManager>();
        final List<ConsumerManager> retFail = new ArrayList<ConsumerManager>();

        // run tasks remotely
        runner.run(retSuccess, retFail);

        if (argv.length == 3) {
            final XStream xstream = new XStream();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(argv[1]));
                xstream.toXML(retSuccess, fos);
            } catch (Exception e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            } finally {
                IOUtils.closeQuietly(fos);
            }
            try {
                fos = new FileOutputStream(new File(argv[2]));
                xstream.toXML(retFail, fos);
            } catch (Exception e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        } else {
            for (ConsumerManager c:retSuccess){
                LOGGER.info("Succesfully completed consumer: "+c.getUuid()+" with status "+c.getStatus());
                for (JMXCumulatorListener l:c.getListeners(JMXCumulatorListener.class)){
                    LOGGER.info("Messages: ");
                    int i=0;
                    for (String message:l.getMessages())
                        LOGGER.info("Message_"+i+": "+message);
                }
            }
            for (ConsumerManager c:retFail){
                LOGGER.info("Failure for consumer: "+c.getUuid()+" with status "+c.getStatus());
                for (JMXCumulatorListener l:c.getListeners(JMXCumulatorListener.class)){
                    LOGGER.info("Messages: ");
                    int i=0;
                    for (String message:l.getMessages())
                        LOGGER.info("Message_"+i+": "+message);
                }
            }
        }
        System.exit(0);
    }

}
