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
package it.geosolutions.geobatch.rasterprocessing.zonalstats;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.rasterprocessing.ZonalStatsAction;
import it.geosolutions.geobatch.rasterprocessing.ZonalStatsActionConfiguration;
import it.geosolutions.geobatch.tools.file.Extract;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.media.jai.JAI;

import org.apache.commons.io.FileUtils;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class ZonalStatsActionTest extends Assert {

    @Before
    public void setup() {
        try {
            FileUtils.deleteDirectory(new File("src/test/resources/data/restricted/"));
            FileUtils.deleteDirectory(new File("src/test/resources/data/out/"));
            
            // unzip shapefile
            Extract.extract("src/test/resources/data/restricted.zip");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    @After
    public void after() {
        try {
            FileUtils.deleteDirectory(new File("src/test/resources/data/restricted/"));
            FileUtils.deleteDirectory(new File("src/test/resources/data/out/"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }
    @Test
    public void testSimple() throws ActionException {

//        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512 * 1024 * 1024);
//        new TCTool((SunTileCache) JAI.getDefaultInstance().getTileCache())
//                .setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final ZonalStatsActionConfiguration zsac = new ZonalStatsActionConfiguration("ZonalStatsActionConfiguration", "ZonalStatsActionConfiguration", "ZonalStatsActionConfiguration");
        File outDir = new File("src/test/resources/data/out");
        if (!outDir.exists())
            Assert.assertTrue(outDir.mkdirs());
        zsac.setOutDir(outDir.getAbsolutePath());

        final ZonalStatsAction zsa = new ZonalStatsAction(zsac);
        Queue<FileSystemEvent> queue = new LinkedBlockingQueue<FileSystemEvent>();
        queue.add(new FileSystemEvent(new File("src/test/resources/data/restricted/restricted.shp"),
                FileSystemEventType.FILE_ADDED));
        queue.add(new FileSystemEvent(new File("src/test/resources/data/zones.tiff"),
                FileSystemEventType.FILE_ADDED));

        queue = zsa.execute(queue);

        assertNotNull(queue);
        assertEquals(1, queue.size());

        File shapeFileOut = queue.peek().getSource();

        ShapefileDataStore sh = null;
        try {
            // open up shapefile
            HashMap params = new HashMap();
            params.put(ShapefileDataStoreFactory.URLP.key, shapeFileOut.toURI().toURL());
            params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key, true);
            params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);
            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, true);
            sh = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(params);

            // input
            SimpleFeatureCollection collection = sh.getFeatureSource().getFeatures();
            assertNotNull(collection);
            assertEquals(4,collection.size());
            
            for(SimpleFeatureIterator it=collection.features(); it.hasNext();){
                System.out.println(it.next());
            }
            // checks
            collection.clear();
            
            SimpleFeatureCollection res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=1"));
            SimpleFeature sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 1, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 571, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 1.41336633663366, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"), 0.493048001685133, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 404, 1E-6);
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=2"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 1, 1E-6);
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=3"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 0, 1E-6);
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=4"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 0, 1E-6);
            
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(sh!=null){
                try{
                    sh.dispose();
                } catch (Exception e) {
                    // eat me
                }
            }
        }
    }
    
    @Test
    public void testWithClassificationRaster() throws ActionException {

        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512 * 1024 * 1024);
//        new TCTool((SunTileCache) JAI.getDefaultInstance().getTileCache())
//                .setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final ZonalStatsActionConfiguration zsac = new ZonalStatsActionConfiguration("a", "a", "a");
        File outDir = new File("src/test/resources/data/out");
        if (!outDir.exists())
            Assert.assertTrue(outDir.mkdirs());
        zsac.setOutDir(outDir.getAbsolutePath());

        final ZonalStatsAction zsa = new ZonalStatsAction(zsac);
        Queue<FileSystemEvent> queue = new LinkedBlockingQueue<FileSystemEvent>();
        queue.add(new FileSystemEvent(new File("src/test/resources/data/restricted/restricted.shp"),
                FileSystemEventType.FILE_ADDED));
        queue.add(new FileSystemEvent(new File("src/test/resources/data/zones.tiff"),
                FileSystemEventType.FILE_ADDED));
        queue.add(new FileSystemEvent(new File("src/test/resources/data/zones_class.tif"),
                FileSystemEventType.FILE_ADDED));        

        queue = zsa.execute(queue);

        assertNotNull(queue);
        assertEquals(1, queue.size());

        File shapeFileOut = queue.peek().getSource();

        ShapefileDataStore sh = null;
        try {
            // open up shapefile
            HashMap params = new HashMap();
            params.put(ShapefileDataStoreFactory.URLP.key, shapeFileOut.toURI().toURL());
            params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key, true);
            params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);
            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, true);
            sh = (ShapefileDataStore) new ShapefileDataStoreFactory().createDataStore(params);

            // input
            SimpleFeatureCollection collection = sh.getFeatureSource().getFeatures();
            assertNotNull(collection);
            assertEquals(6,collection.size());
            
         
            // checks
            collection.clear();
            
            SimpleFeatureCollection res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=1 AND classifica=0"));
            SimpleFeature sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"),0, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 1, 1E-6);
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=1 AND classifica=1"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 1, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 316, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 1.99212598425197, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"),0.0887356509416114, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 276, 1E-6);
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=1 AND classifica=2"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 1, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 253, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 1.99212598425197, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"),0.0887356509416114, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 127, 1E-6);
            
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=2"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 2, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"),0, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 1, 1E-6);
            
            
            res = sh.getFeatureSource(sh.getTypeNames()[0]).getFeatures(CQL.toFilter("z_cat=3"));
            sf = (SimpleFeature)res.toArray()[0];
            assertEquals((Double)sf.getAttribute("min"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("max"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("sum"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("avg"), 0, 1E-6);
            assertEquals((Double)sf.getAttribute("stddev"),0, 1E-6);
            assertEquals((Double)sf.getAttribute("count"), 0, 1E-6);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(sh!=null){
                try{
                    sh.dispose();
                } catch (Exception e) {
                    // eat me
                }
            }
        }
    }
}

