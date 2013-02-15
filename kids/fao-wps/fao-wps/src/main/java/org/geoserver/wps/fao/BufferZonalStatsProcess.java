/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.PointBuffers;
import org.geoserver.wps.gs.RasterZonalStatistics;
import org.geoserver.wps.gs.VectorZonalStatistics;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * A process computing zonal statistics over raster and vector data
 * 
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(title = "Buffer Zonal Stats Process", description = "Points in buffer areas zonal Stats")
public class BufferZonalStatsProcess implements FAOProcess {

    Catalog catalog;

    public BufferZonalStatsProcess(Catalog catalog) {
        this.catalog = catalog;
    }
    
    @DescribeResult(name="result", description="The computed stats")
    public BufferZonalStatistics execute(
            @DescribeParameter(name = "datalayers", description = "List of the layers containing the data to be computed. "
                    + "List is comma separated. Format [namespace:]layername") String dataLayers,
            @DescribeParameter(name = "datafields", description = "List of the name of the fields "
                    + "in the (vector) layers containing the data. One fieldName declaration for every vector layer is required. "
                    + "Raster may have empty field names or provide a band number.", min = 0) String dataFields,
            @DescribeParameter(name = "datacqlfilters", description = "CQL filtering the data features. "
                    + "Only used for vector layers. "
                    + "Optional; if existing, a (possibly empty) entry for every layer is required. "
                    + "CQL filters are separated by the # character.", min = 0) String dataCqlFilters,
            @DescribeParameter(name = "areacenter", description = "LonLat coords of the center of the circular buffers.") String areaCenter,
            @DescribeParameter(name = "arearadii", description = "Comma separated radii in Km of the circular buffers.") String arearadii,
            ProgressListener listener) throws Exception {

        // parse the data layer list
        List<ResourceInfo> dataResources = new ArrayList<ResourceInfo>();
        for (String dataLayerName : dataLayers.split("\\s*,\\s*")) {
            ResourceInfo ri = catalog.getResourceByName(dataLayerName, ResourceInfo.class);
            if (ri == null) {
                throw new WPSException("Could not locate layer '" + dataLayerName + "'",
                        "InvalidParameterValue", "Data layers list");
            } else if (!(ri instanceof FeatureTypeInfo) && !(ri instanceof CoverageInfo)) {
                throw new WPSException("Layer '" + dataLayerName
                        + "' cannot be used, should be either a vector or a raster layer",
                        "InvalidParameterValue", "Data layers list");
            }
            dataResources.add(ri);
        }

        // the data fields
        List<String> fields = null;
        if(dataFields != null) {
            fields = Arrays.asList(dataFields.split("\\s*,\\s*"));
            if (fields.size() != dataResources.size()) {
                throw new WPSException("Data layers and fields length mismatch" + " ("
                        + dataResources.size() + "," + fields.size() + ")");
            }
        } 

        // the cql filters list
        List<Filter> cqlFilters = new ArrayList<Filter>();
        if (dataCqlFilters != null) {
            for (String cql : dataCqlFilters.split("\\s*#\\s*")) {
                if (cql.equals("")) {
                    cqlFilters.add(Filter.INCLUDE);
                } else {
                    cqlFilters.add(ECQL.toFilter(cql));
                }
            }
        } else {
            for (int i = 0; i < dataResources.size(); i++) {
                cqlFilters.add(Filter.INCLUDE);
            }
        }
        if (cqlFilters.size() != dataResources.size()) {
            throw new WPSException("Data layers and cql filters length mismatch" + " ("
                    + dataResources.size() + "," + cqlFilters.size() + ")");
        }
        
        // parse the center
        Point center;
        String centerOrdinates[] = areaCenter.split("\\s*,\\s*");
        if (centerOrdinates.length != 2) {
            throw new WPSException("Bad area center [" + centerOrdinates + "]. Format is lon,lat", "InvalidParameterValue", "areacenter");
        }
        try {
            double lon = Double.parseDouble(centerOrdinates[0].trim());
            double lat = Double.parseDouble(centerOrdinates[1].trim());
            center = new GeometryFactory().createPoint(new Coordinate(lon, lat));
        } catch (NumberFormatException ex) {
            throw new WPSException("Bad area center [" + centerOrdinates + "]. Unparsable coords.", "InvalidParameterValue", "areacenter");
        }

        // parse the diameters
        double[] distances;
        try {
            String[] strDistances = arearadii.split("\\s*,\\s*");
            distances = new double[strDistances.length];
            for (int i = 0; i < strDistances.length; i++) {
                distances[i] = Double.parseDouble(strDistances[i]) * 1000;
            }
        } catch (NumberFormatException ex) {
            throw new WPSException("Bad area diameter [" + arearadii + "].", "InvalidParameterValue", "areacenter");
        }
        if (distances.length == 0) {
            throw new WPSException("At least one area radius is required", "InvalidParameterValue", "areacenter");
        }
        
        // build the zones feature collection
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        SimpleFeatureCollection areaFeatureCollection = new PointBuffers().execute(center, wgs84, distances, 21, null);
        
        // build up the result list
        List<BufferZonalStatistic> stats = new ArrayList<BufferZonalStatistic>();
        for(int i = 0; i < dataResources.size(); i++) {
            SimpleFeatureCollection result;
            String field = fields != null ? fields.get(i) : null;
            ResourceInfo info = dataResources.get(i);
            if(dataResources.get(i) instanceof FeatureTypeInfo) {
                // get the feature collection
                FeatureTypeInfo ftInfo = (FeatureTypeInfo) dataResources.get(i);
                SimpleFeatureCollection dataCollection = (SimpleFeatureCollection) ftInfo.getFeatureSource(null, null).getFeatures(cqlFilters.get(i));
                
                // get and check the field
                if(field == null) {
                    throw new WPSException("Field for layer " + info.getPrefixedName() + " is missing", 
                            "InvalidParameterValue", "Data layers field names");
                }
                if(ftInfo.getFeatureType().getDescriptor(field) == null) {
                    throw new WPSException("Field " + field + " not available in layer " + info.getPrefixedName(), 
                            "InvalidParameterValue", "Data layers field names");
                }
                
                // run the statistics
                result = new VectorZonalStatistics().execute(dataCollection, field, areaFeatureCollection);
            } else {
                // get the feature collection
                CoverageInfo covInfo = (CoverageInfo) dataResources.get(i);
                GridCoverage2D coverage = (GridCoverage2D) covInfo.getGridCoverage(null, null);
                
                // get and check the field, should be a band if available
                int band = 0;
                if(field != null && field.length() > 0) {
                    try {
                        band = Integer.parseInt(field);
                    } catch (NumberFormatException e) {
                        throw new WPSException("Invalid band number specified in field list: " + field, 
                                "InvalidParameterValue", "Data layers list");
                    }
                }
                
                // run the statistics
                result = new RasterZonalStatistics().execute(coverage, band, areaFeatureCollection, null);
            }
            
            // turn the statistics into ZoneStatistic objects
            SimpleFeatureIterator fi = null;
            try {
                fi = result.features();
                while(fi.hasNext()) {
                    SimpleFeature f = fi.next();
                    
                    // basic info
                    BufferZonalStatistic stat = new BufferZonalStatistic();
                    stat.buffercenter = areaCenter;
                    stat.bufferradius = (Double) f.getAttribute("z_radius") / 1000.0;
                    stat.datalayer = info.getPrefixedName();
                    if(info instanceof FeatureTypeInfo) {
                        // field is returned only if the layer is a vector one
                        stat.datafield = field;
                    }
                    
                    // statistics
                    Statistics s = new Statistics(f);
                    stat.stats = s;
                    
                    stats.add(stat);
                }
            } finally {
                if(fi != null) {
                    fi.close();
                }
            }
        }
        
        

        return new BufferZonalStatistics(stats);
    }

    protected String asString(Object attribute) { 
        if(attribute == null) {
            return null;
        } else {
            return attribute.toString();
        }
    }
}
