/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
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
package it.geosolutions.geobatch.rasterprocessing.classifiedstats;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsAction;
import it.geosolutions.geobatch.rasterprocessing.ClassifiedStatsConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ETj (etj at geo-solutions.it)
 */
public class ClassifiedStatsActionTest extends Assert {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClassifiedStatsActionTest.class);

    @Test
    public void test2ClassStats() throws ActionException, IOException {
        
//      JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512*1024*1024);
//      new TCTool((SunTileCache)JAI.getDefaultInstance().getTileCache());
        File data = loadFile("data/data.tif");
        File class1 = loadFile("data/classmask1.tif");
        File class2 = loadFile("data/class5stripes.tif");

        assertNotNull(data);
        assertNotNull(class1);
        assertNotNull(class2);

        File inputFile = File.createTempFile("clstats_in", ".xml");
        FileUtils.forceDeleteOnExit(inputFile);
        File outputFile = File.createTempFile("clstats_out", ".xml");
        FileUtils.forceDeleteOnExit(outputFile);

        // create the input file
        Element reqRoot = new Element("classifiedStats")
                .addContent(new Element("data").setText(data.getAbsolutePath()))
                .addContent(new Element("classification").setText(class1.getAbsolutePath()))
                .addContent(new Element("classification").setText(class2.getAbsolutePath()));
        Element output = new Element("output")
                .addContent(new Element("file").setText(outputFile.getAbsolutePath()))
                .addContent(new Element("gaez_id").setText("GAEZ_CODE_HERE_2"))
                .addContent(new Element("categories")
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("0"))
                            .addContent(new Element("description").setText("code zero")))
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("1"))
                            .addContent(new Element("description").setText("code,one")))
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("2"))
                            .addContent(new Element("description").setText("code \"two\"")))
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("3"))
                            .addContent(new Element("description").setText("code, \"three\"")))
                        );
        reqRoot.addContent(output);
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        FileUtils.writeStringToFile(inputFile, outputter.outputString(reqRoot));

        ClassifiedStatsConfiguration actionCfg = new ClassifiedStatsConfiguration("csc", "csc", "csc");
//        actionCfg.setPercentage(true);
        ClassifiedStatsAction action = new ClassifiedStatsAction(actionCfg);
        
        Queue<FileSystemEvent> queue = new LinkedBlockingQueue<FileSystemEvent>();
        queue.add(new FileSystemEvent(inputFile, FileSystemEventType.FILE_ADDED));
        queue = action.execute(queue);

        LOGGER.info("Output file is " + outputFile);
        LOGGER.info(IOUtils.toString(new FileReader(outputFile)));

    }

    @Test
    public void test1ClassStats() throws ActionException, IOException {
        
//        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512*1024*1024);
//        new TCTool((SunTileCache)JAI.getDefaultInstance().getTileCache());
        
//        File data = loadFile("data/data.tif");
//        File class1 = loadFile("data/classmask1.tif");
        File data = loadFile("data/area_lv2_LZW.tif");
        File class1 = loadFile("data/gaul_lv2_LZW.tif");

        assertNotNull(data);
        assertNotNull(class1);

        File inputFile = File.createTempFile("clstats_in", ".xml");
        FileUtils.forceDeleteOnExit(inputFile);
        File outputFile = File.createTempFile("clstats_out", ".xml");
        FileUtils.forceDeleteOnExit(outputFile);

        // create the input file
        Element reqRoot = new Element("classifiedStats")
                .addContent(new Element("data").setText(data.getAbsolutePath()))
                .addContent(new Element("classification").setText(class1.getAbsolutePath()));
        Element output = new Element("output")
                .addContent(new Element("file").setText(outputFile.getAbsolutePath()))
                .addContent(new Element("gaez_id").setText("GAEZ_CODE_HERE"));
        reqRoot.addContent(output);
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        FileUtils.writeStringToFile(inputFile, outputter.outputString(reqRoot));

        ClassifiedStatsConfiguration actionCfg = new ClassifiedStatsConfiguration("csc", "csc", "csc");
//        actionCfg.setComputationMode(ComputationMode.DEFERRED);
        ClassifiedStatsAction action = new ClassifiedStatsAction(actionCfg);

        Queue<FileSystemEvent> queue = new LinkedBlockingQueue<FileSystemEvent>();
        queue.add(new FileSystemEvent(inputFile, FileSystemEventType.FILE_ADDED));
        queue = action.execute(queue);

        LOGGER.info("Output file is " + outputFile);
        LOGGER.info(IOUtils.toString(new FileReader(outputFile)));

    }

    @Test
    public void testDataMultiply() throws ActionException, IOException {

    LOGGER.error(" =============== testDataMultiply() ");

//      JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512*1024*1024);
//      new TCTool((SunTileCache)JAI.getDefaultInstance().getTileCache());
        File data = loadFile("data/data.tif");
        File class1 = loadFile("data/classmask1.tif");
        File class2 = loadFile("data/class5stripes.tif");

        assertNotNull(data);
        assertNotNull(class1);
        assertNotNull(class2);

        File inputFile = File.createTempFile("clstats_in", ".xml");
        FileUtils.forceDeleteOnExit(inputFile);
        File outputFile = File.createTempFile("clstats_out", ".xml");
        FileUtils.forceDeleteOnExit(outputFile);

        // create the input file
        Element reqRoot = new Element("classifiedStats")
                .addContent(new Element("data").setText(data.getAbsolutePath()))
                .addContent(new Element("classification").setText(class2.getAbsolutePath()))
                .addContent(new Element("data_multiplier").setText(class1.getAbsolutePath()));

        Element output = new Element("output")
                .addContent(new Element("file").setText(outputFile.getAbsolutePath()))
                .addContent(new Element("gaez_id").setText("GAEZ_CODE_HERE_2"))
                .addContent(new Element("categories")
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("0"))
                            .addContent(new Element("description").setText("code zero")))
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("1"))
                            .addContent(new Element("description").setText("code,one")))
                        .addContent(new Element("category")
                            .addContent(new Element("code").setText("file_title"))
                            .addContent(new Element("description").setText("TITLE,INSTEAD,OF,DESC")))
                        );
        reqRoot.addContent(output);
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        FileUtils.writeStringToFile(inputFile, outputter.outputString(reqRoot));

        ClassifiedStatsConfiguration actionCfg = new ClassifiedStatsConfiguration("csc", "csc", "csc");
//        actionCfg.setPercentage(true);
        ClassifiedStatsAction action = new ClassifiedStatsAction(actionCfg);

        Queue<FileSystemEvent> queue = new LinkedBlockingQueue<FileSystemEvent>();
        queue.add(new FileSystemEvent(inputFile, FileSystemEventType.FILE_ADDED));
        queue = action.execute(queue);

        LOGGER.info("Output file is " + outputFile);
        LOGGER.info(IOUtils.toString(new FileReader(outputFile)));

    }


    protected File loadFile(String name) {
        try {
            URL url = this.getClass().getClassLoader().getResource(name);
            if(url == null)
                throw new IllegalArgumentException("Cant get file '"+name+"'");
            File file = new File(url.toURI());
            return file;
        } catch (URISyntaxException e) {
            LOGGER.error("Can't load file " + name + ": " + e.getMessage(), e);
            return null;
        }
    }

}

