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

import it.geosolutions.tools.compress.file.Compressor;
import it.geosolutions.tools.io.file.Collector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.management.remote.JMXConnector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class MainGeoBatchJMXClient {

    private final static String CONNECTION_PARAMETERS_KEY = "CONNECTION_PARAMETERS";

    private final static Logger LOGGER = LoggerFactory.getLogger(MainGeoBatchJMXClient.class);

    private final static XStream xstream = new XStream();

    /**
     * USAGE:<br>
     * java it.geosolutions.geobatch.services.jmx.MainJMXClientUtils
     * /PATH/TO/FILE.properties<br>
     * where FILE.properties is the command property file<br>
     * 
     * @param argv a String[0] containing the path of the environment used to
     *            run the action on GeoBatch
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
        final Map<String, String> commonEnv = JMXClientUtils.loadEnv(path);
        try {
            final String connectionPropFileName = commonEnv.get(CONNECTION_PARAMETERS_KEY);
            if (connectionPropFileName != null && !connectionPropFileName.isEmpty()) {
                final Map<String, String> connectionParams = JMXClientUtils.loadEnv(connectionPropFileName);
                initConnection(connectionParams);
            } else {
                // try using the main prop file
                initConnection(commonEnv);
            }

            final ExecutorService es = Executors.newFixedThreadPool(10); // TODO
                                                                         // get
            // thread
            // pool
            // size
            final CompletionService<ConsumerManager> cs = new ExecutorCompletionService<ConsumerManager>(es);
            final List<File> retSuccessFiles = new ArrayList<File>();
            final List<File> retFailFiles = new ArrayList<File>();

            // TODO remove or use in a better way
            int ret = -1;
            for (int size = runTasks(commonEnv, cs); size > 0; --size) {

                // the return/exit status
                try {
                    final Future<ConsumerManager> task = cs.take();
                    final ConsumerManager consumerManager = task.get();
                    final String file = consumerManager.getConfiguration(0).get(ConsumerManager.INPUT_KEY);
                    final ConsumerStatus status = consumerManager.getStatus();
                    final String uuid = consumerManager.getUuid();

                    if (status == ConsumerStatus.COMPLETED) {
                        // success
                        if (LOGGER.isInfoEnabled())
                            LOGGER.info("Action UUID: " + uuid + " EXIT status is: " + status);
                        if (file != null)
                            retSuccessFiles.add(new File(file));

                        if (LOGGER.isInfoEnabled()) {
                            Iterator<JMXProgressListener> it = consumerManager.getListeners().iterator();
                            while (it.hasNext())
                                LOGGER.info("Listener messages: "
                                            + JMXCumulatorListener.printMessages(JMXCumulatorListener.class
                                                .cast(it.next())));
                        }

                        // normal
                        ret = 0;

                    } else if (status == ConsumerStatus.FAILED || status == ConsumerStatus.CANCELED) {
                        // failure
                        if (LOGGER.isInfoEnabled())
                            LOGGER.info("Action UUID: " + uuid + " EXIT status is: " + status);
                        if (file != null)
                            retFailFiles.add(new File(file));
                        if (LOGGER.isErrorEnabled()) {
                            Iterator<JMXProgressListener> it = consumerManager.getListeners().iterator();
                            while (it.hasNext())
                                LOGGER.error("Listener messages: "
                                             + JMXCumulatorListener.printMessages(JMXCumulatorListener.class
                                                 .cast(it.next())));
                        }
                        // some error
                        ret = 1;

                    } else if (status == ConsumerStatus.UNRECOGNIZED) {
                        // failure
                        if (LOGGER.isErrorEnabled())
                            LOGGER.error("Action UUID: " + uuid + " EXIT status is: " + status);
                        if (file != null)
                            retFailFiles.add(new File(file));
                        if (LOGGER.isErrorEnabled()) {
                            Iterator<JMXProgressListener> it = consumerManager.getListeners().iterator();
                            while (it.hasNext())
                                LOGGER.error("Listener messages: "
                                             + JMXCumulatorListener.printMessages(JMXCumulatorListener.class
                                                 .cast(it.next())));
                        }
                        // unrecognized status
                        ret = 2;
                    } else { // status == null
                        // failure
                        if (LOGGER.isErrorEnabled())
                            LOGGER.error("Action UUID: " + uuid
                                         + " Unable to submit new actions to the remote GB");
                        if (file != null)
                            retFailFiles.add(new File(file));
                        if (LOGGER.isErrorEnabled()) {
                            Iterator it = consumerManager.getListeners(JMXCumulatorListener.class).iterator();
                            while (it.hasNext())
                                LOGGER.error("Listener messages: "
                                             + JMXCumulatorListener.printMessages(JMXCumulatorListener.class
                                                 .cast(it.next())));
                        }
                        // unrecognized status
                        ret = 3;
                    }
                } catch (NullPointerException e) {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error(e.getMessage(), e);
                    ret = 5;
                    // } catch (IOException e) {
                    // if (LOGGER.isErrorEnabled())
                    // LOGGER.error(e.getMessage(), e);
                    // ret = 6;
                } catch (Exception e) {
                    if (LOGGER.isErrorEnabled())
                        LOGGER.error(e.getMessage(), e);
                    ret = 7;
                }
            }

            if (argv.length == 3) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(new File(argv[1]));
                    xstream.toXML(retSuccessFiles, fos);
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
                try {
                    fos = new FileOutputStream(new File(argv[2]));
                    xstream.toXML(retFailFiles, fos);
                } catch (Exception e) {
                    LOGGER.error(e.getLocalizedMessage(), e);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }
            System.exit(ret);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled())
                LOGGER.error(e.getLocalizedMessage(), e);
            else
                LOGGER.error(e.getLocalizedMessage());

            throw e;
        } finally {
            dispose();
        }

    }

    public static void applyNamingConvention(Map<String, String> env, File file) {
        String fileName = FilenameUtils.getBaseName(file.getName());

        // name of layer less the extension for geotiffPublish
        String layerName = fileName.toLowerCase();

        // put the layer name to LowerCase for geotiffPublish
        env.put("layerName", layerName.toLowerCase());

        // put the store name to UpperCase for shapeFilePublish
        env.put("storeName", layerName.toUpperCase());

        // put into environment the input
        env.put("INPUT", file.toString());

        // put into GS styles "layerName_sld"
        env.put("defaultStyle", layerName + "_sld");

    }

    /**
     * using the passed environment builds a list of env to work on
     * 
     * @param service
     * @param input
     * @return
     * @throws IOException
     */
    public static int runTasks(Map<String, String> env, CompletionService<ConsumerManager> cs)
        throws IllegalArgumentException, IOException {
        // Get list of input files
        final String input = env.get(ConsumerManager.INPUT_KEY);
        if (input == null)
            throw new IllegalArgumentException("null input string");
        File inFile = new File(input);
        
        List<File> files;
        if (!inFile.exists()) {
        	files=new ArrayList<File>();
        	final String service = env.get(ConsumerManager.SERVICE_ID_KEY);
        	
        	if (service.equals("GeotiffGeoServerService")) {
                // geoserver publish action
        		// check for method
        		if (env.get("dataTransferMethod").equalsIgnoreCase("EXTERNAL")){
        			files.add(inFile);
        		} else {                 
        			throw new IllegalArgumentException("input file doesn't esists nor EXTERNAL method is set");
        		}
            } else if (service.equals("ShapeFileGeoServerService")) {
            	// geoserver publish action
        		// check for method
        		if (env.get("dataTransferMethod").equalsIgnoreCase("EXTERNAL")){
        			files.add(inFile);
        		} else {                 
        			throw new IllegalArgumentException("input file doesn't esists nor EXTERNAL method is set");
        		}
            } else {
            	throw new IllegalArgumentException("Not implemented");            	
            }
            
        } else if (inFile.isDirectory()) {
            // split env by input
            final String service = env.get(ConsumerManager.SERVICE_ID_KEY);
            Collector c = new Collector(null, 1); // TODO set deep

            if (service.equals("GeotiffGeoServerService")) {
                c.setFilter(new WildcardFileFilter("*.tif", IOCase.INSENSITIVE));
                files = c.collect(inFile);

            } else if (service.equals("ShapeFileGeoServerService")) {
                final String dataTransferMethod = env.get("dataTransferMethod"); // DIRECT,
                // URL,
                // EXTERNAL

                if (dataTransferMethod.equalsIgnoreCase("DIRECT")) {
                    c.setFilter(new WildcardFileFilter("*.shp", IOCase.INSENSITIVE));
                    files = c.collect(inFile);
                    for (File file : files) {
                        String shpFileName = FilenameUtils.getBaseName(file.getName());
                        c.setFilter(new WildcardFileFilter(shpFileName + ".*", IOCase.INSENSITIVE));
                        List<File> shpFileColl = c.collect(inFile);
                        Compressor.zip(inFile, shpFileName, shpFileColl.toArray(new File[] {}));
                    }
                    c.setFilter(new WildcardFileFilter("*.zip", IOCase.INSENSITIVE));
                    files = c.collect(inFile);
                } else if (dataTransferMethod.equalsIgnoreCase("EXTERNAL")) {
                    c.setFilter(new WildcardFileFilter("*.shp", IOCase.INSENSITIVE));
                    files = c.collect(inFile);
                } else {
                    throw new IllegalArgumentException("Unsupported dataTransferMethod mode "
                                                       + dataTransferMethod);
                }
            } else {
                files = new ArrayList<File>();
                files.add(inFile);
            }
        } else {
            // inFile is !directory
            // is it an xml file?
            String ext = FilenameUtils.getExtension(inFile.getName());
            if (ext.equalsIgnoreCase("xml")) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(inFile);
                    Object o = xstream.fromXML(fis);
                    if (o instanceof List) {
                        files = (List<File>)o;
                    } else {
                        // writing an example
                        files = new ArrayList<File>();
                        files.add(new File("/file.txt"));
                        files.add(new File("/file_1.txt"));
                        files.add(new File("/file_2.txt"));
                        LOGGER.error("Legal format is:" + xstream.toXML(files));
                        throw new IllegalArgumentException("The input file is not in a legal format: "
                                                           + inFile);
                    }
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            } else {
                files = new ArrayList<File>();
                files.add(inFile);
            }
        }

        int size = 0; // number of submitted tasks
        for (File file : files) {
            // change config using naming convention (from the file name)
            final HashMap<String, String> consumerConfiguration = new HashMap<String, String>(env);

            // modify consumer configuration using naming convention
            applyNamingConvention(consumerConfiguration, file);

            try {
                // submit the job
                cs.submit(new JMXAsynchConsumer(jmxConnector, serviceManager, consumerConfiguration, JMXClientUtils.parsePollingDelay(consumerConfiguration)));

                // work queue size
                ++size;

            } catch (RejectedExecutionException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.error(e.getLocalizedMessage(), e);
                else
                    LOGGER.error(e.getLocalizedMessage());
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.error(e.getLocalizedMessage(), e);
                else
                    LOGGER.error(e.getLocalizedMessage());
            }
        }
        return size;

    }

    // the jmx connector
    private static JMXConnector jmxConnector = null;
    // the ActionManager's proxy
    private static ServiceManager serviceManager = null;

    /**
     * 
     * @param connectionParams connection parameters
     */
    private static void initConnection(Map<String, String> connectionParams) throws Exception {
        if (connectionParams == null) {
            throw new IllegalArgumentException("Unable to run using a null environment map");
        }
        try {
            // get the connector using the configured environment
            jmxConnector = JMXClientUtils.getConnector(connectionParams);
            // create the proxy
            serviceManager = JMXClientUtils.getProxy(connectionParams, jmxConnector);

        } catch (Exception e) {
            if (LOGGER.isErrorEnabled())
                LOGGER.error(e.getLocalizedMessage(), e);
            dispose();
            throw e;
        }
    }

    private static void dispose() throws IOException {
        // TODO dispose all the pending consumers?!?
        if (jmxConnector != null) {
            try {
                // close connector's connection
                jmxConnector.close();
            } catch (IOException e) {
                if (LOGGER.isErrorEnabled())
                    LOGGER.error(e.getMessage(), e);
            }
        }
    }

}
