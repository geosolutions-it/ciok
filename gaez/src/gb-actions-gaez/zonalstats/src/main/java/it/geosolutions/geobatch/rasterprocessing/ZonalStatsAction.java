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

package it.geosolutions.geobatch.rasterprocessing;

import it.geosolutions.filesystemmonitor.monitor.FileSystemEvent;
import it.geosolutions.filesystemmonitor.monitor.FileSystemEventType;
import it.geosolutions.geobatch.flow.event.action.ActionException;
import it.geosolutions.geobatch.flow.event.action.BaseAction;
import it.geosolutions.geobatch.rasterprocessing.zonalstats.RasterZonalStatistics;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.resources.image.ImageUtilities;

/**
 * Simple action to work out the zonal stats from JAI Tools.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 */
public class ZonalStatsAction extends BaseAction<FileSystemEvent> {
	
	private final ZonalStatsActionConfiguration conf;

	public ZonalStatsAction(ZonalStatsActionConfiguration actionConfiguration) {
		super(actionConfiguration);
		conf=actionConfiguration;
	}

	public Queue<FileSystemEvent> execute(Queue<FileSystemEvent> ev)
			throws ActionException {
		// making all sort of assumptions on what we receive
		// 1 Shapefile
		// 2 GeoTiff dati
		File shapefile = ev.poll().getSource();
		final String shapeFileName=shapefile.getName();
		final File data = ev.poll().getSource();
		File classification=null;
		if(!ev.isEmpty())
		    classification = ev.poll().getSource();
		
		Queue<FileSystemEvent> ret=null;
		GeoTiffReader readerData = null, classificationData=null;
		GridCoverage2D dataCoverage=null, classificationCoverage=null;
		ShapefileDataStore sh = null;
		try {
			// open up geotiff
			readerData = new GeoTiffReader(data);
			dataCoverage = readerData.read(null);
			
			if(classification!=null){
		                // open up classification
			        classificationData = new GeoTiffReader(classification);
			        classificationCoverage = classificationData.read(null); 

			}

			// open up shapefile
			final HashMap params = new HashMap();
			params.put(ShapefileDataStoreFactory.URLP.key, shapefile.toURI()
					.toURL());
			params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key, true);
			params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);
			params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, true);
			sh = (ShapefileDataStore) new ShapefileDataStoreFactory()
					.createDataStore(params);

			// input
			SimpleFeatureCollection collection = sh.getFeatureSource()
					.getFeatures();

			// output
			SimpleFeatureCollection output = new RasterZonalStatistics()
					.execute(dataCoverage, 0, collection, classificationCoverage);

			// write down as shapefile for the moment
			File outFile=new File(conf.getOutDir(),shapeFileName);
			
			ret=new LinkedList<FileSystemEvent>();
			
			writeResults(output, outFile);
			ret.add(new FileSystemEvent(outFile, FileSystemEventType.FILE_ADDED));
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		    
		    // perform clean up
		    if(readerData!=null){
		        try{
		            readerData.dispose();
		        } catch (Exception e) {
                            //eat me
                        }
		    }
		    
                    if (dataCoverage != null) {
                        try {                            
                            ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(dataCoverage.getRenderedImage()));
                            
                            //paranoiac check
                            dataCoverage.dispose(true);
                        } catch (Exception e) {
                            // eat me
                        }
                    }
                    
                    // perform clean up
                    if(classificationData!=null){
                        try{
                            classificationData.dispose();
                        } catch (Exception e) {
                            //eat me
                        }
                    }
                    
                    if (dataCoverage != null) {
                        try {                            
                            ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(dataCoverage.getRenderedImage()));
                            
                            //paranoiac check
                            dataCoverage.dispose(true);
                        } catch (Exception e) {
                            // eat me
                        }
                    }
                    if (classificationCoverage != null) {
                        try {                            
                            ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(classificationCoverage.getRenderedImage()));
                            
                            //paranoiac check
                            dataCoverage.dispose(true);
                        } catch (Exception e) {
                            // eat me
                        }
                    }
                    if (sh != null) {
                        try {                            
                                              
                            sh.dispose();
                        } catch (Exception e) {
                            // eat me
                        }
                    }
                                        
		}
		
		return ret;
	}

	private void writeResults(SimpleFeatureCollection collection, final File out)
			throws IOException {
		/*
		 * Get an output file name and create the new shapefile
		 */


		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", out.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
				.createNewDataStore(params);
		newDataStore.createSchema(collection.getSchema());

		/*
		 * Write the features to the shapefile
		 */
		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore
				.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(collection);
				transaction.commit();

			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();

			} finally {
				transaction.close();
			}
		} else {
			System.out
					.println(typeName + " does not support read/write access");
		}

	}
}
