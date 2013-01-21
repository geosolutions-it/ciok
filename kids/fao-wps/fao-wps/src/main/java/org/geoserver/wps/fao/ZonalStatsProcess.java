/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import jaitools.numeric.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.RangeLookupProcess;
import org.geoserver.wps.gs.RasterZonalStatistics;
import org.geoserver.wps.gs.VectorZonalStatistics;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * A process computing zonal statistics over raster and vector data
 * 
 * @author Andrea Aime - GeoSolutions
 */
@DescribeProcess(title = "Zonal Stats Process", description = "Points in polygons zonal Stats")
public class ZonalStatsProcess implements FAOProcess {

    Catalog catalog;

    public ZonalStatsProcess(Catalog catalog) {
        this.catalog = catalog;
    }

    @DescribeResult(name = "result", description = "The computed stats")
    public ZonalStatistics execute(
            @DescribeParameter(name = "datalayers", description = "List of the layers containing the data to be computed. "
                    + "List is comma separated. Format [namespace:]layername") String dataLayers,
            @DescribeParameter(name = "datafields", description = "List of the name of the fields "
                    + "in the (vector) layers containing the data. One fieldName declaration for every vector layer is required. "
                    + "Raster layers shall have empty field names or provide a band number.", min = 0) String dataFields,
            @DescribeParameter(name = "datacqlfilters", description = "CQL filtering the data features. "
                    + "Only used for vector layers. "
                    + "Optional; if existing, a (possibly empty) entry for every layer is required. "
                    + "CQL filters are separated by the # character.", min = 0) String dataCqlFilters,
            @DescribeParameter(name = "arealayer", description = "Area layer delimiter") String areaLayer,
            @DescribeParameter(name = "areacqlfilter", description = "CQL filter for area layer delimiter", min = 0) String areaCqlFilter,
            @DescribeParameter(name = "rasterclassification", description = "Ranges to classify data features. "
                    + "Used by raster and point layers. Optional; if existing applies to all data layers. " +
                    		"Entries are in the form [BAND @] LAYERNAME RANGE...", min = 0) String rasterClassificationSpec,
            ProgressListener listener) throws Exception {

        // parse the data layer list
        List<ResourceInfo> dataResources = new ArrayList<ResourceInfo>();
        for (String dataLayerName : dataLayers.split("\\s*,\\s*")) {
            ResourceInfo ri = catalog.getResourceByName(dataLayerName, ResourceInfo.class);
            if (ri == null) {
                throw new WPSException("Could not locate layer '" + dataLayerName + "'",
                        "InvalidParameterValue", "datalayers");
            } else if (!(ri instanceof FeatureTypeInfo) && !(ri instanceof CoverageInfo)) {
                throw new WPSException("Layer '" + dataLayerName
                        + "' cannot be used, should be either a vector or a raster layer",
                        "InvalidParameterValue", "datalayers");
            }
            dataResources.add(ri);
        }

        // the data fields
        List<String> fields = null;
        if (dataFields != null) {
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

        // grab the zones feature collection
        FeatureTypeInfo areaInfo = catalog.getFeatureTypeByName(areaLayer);
        if (areaInfo == null) {
            throw new WPSException("Could not locate zone layer '" + areaLayer + "'",
                    "InvalidParameterValue", "arealayer");
        }
        Filter areaFilter = Filter.INCLUDE;
        if (areaCqlFilter != null) {
            areaFilter = ECQL.toFilter(areaCqlFilter);
        }
        SimpleFeatureCollection areaFeatureCollection = (SimpleFeatureCollection) areaInfo
                .getFeatureSource(null, null).getFeatures(areaFilter);

        // parse the raster classification
        RasterClassification rasterClassification = parseRasterClassifications(rasterClassificationSpec);
        GridCoverage2D classificationGrid = null;
        if(rasterClassification != null) {
            GridCoverage2D raw = (GridCoverage2D) rasterClassification.getCoverageInfo().getGridCoverage(null, null);
            // darn generics...
            List<Range> ranges = new ArrayList<Range>(rasterClassification.getRanges());
            classificationGrid = new RangeLookupProcess().execute(raw, 0, ranges, null);
        }

        // build up the result list
        List<ZonalStatistic> stats = new ArrayList<ZonalStatistic>();
        for (int i = 0; i < dataResources.size(); i++) {
            SimpleFeatureCollection result;
            String field = fields != null ? fields.get(i) : null;
            ResourceInfo info = dataResources.get(i);
            if (info instanceof FeatureTypeInfo) {
                // get the feature collection
                FeatureTypeInfo ftInfo = (FeatureTypeInfo) info;
                SimpleFeatureCollection dataCollection = (SimpleFeatureCollection) ftInfo
                        .getFeatureSource(null, null).getFeatures(cqlFilters.get(i));

                // get and check the field
                if (field == null) {
                    throw new WPSException("Field for layer " + info.getPrefixedName()
                            + " is missing", "InvalidParameterValue", "datafields");
                }
                if (ftInfo.getFeatureType().getDescriptor(field) == null) {
                    throw new WPSException("Field " + field + " not available in layer "
                            + info.getPrefixedName(), "InvalidParameterValue",
                            "datafields");
                }
                
                // we don't support raster classification on vector data
                if(classificationGrid != null) {
                    throw new WPSException("Raster classification not supported against point data", "InvalidParameterValue",
                            "datalayers");
                }

                // run the statistics
                result = new VectorZonalStatistics().execute(dataCollection, field,
                        areaFeatureCollection);
            } else {
                // get the feature collection
                CoverageInfo covInfo = (CoverageInfo) dataResources.get(i);
                GridCoverage2D coverage = (GridCoverage2D) covInfo.getGridCoverage(null, null);

                // get and check the field, should be a band if available
                int band = 0;
                if (field != null && field.length() > 0) {
                    try {
                        band = Integer.parseInt(field);
                    } catch (NumberFormatException e) {
                        throw new WPSException("Invalid band number specified in field list: "
                                + field, "InvalidParameterValue", "datalayers");
                    }
                }
                
                // run the statistics
                result = new RasterZonalStatistics().execute(coverage, band, areaFeatureCollection,
                        classificationGrid);
            }

            // turn the statistics into ZoneStatistic objects
            SimpleFeatureIterator fi = null;
            try {
                fi = result.features();
                ZonalStatistic stat = null;
                String lastFeatureId = null;
                while (fi.hasNext()) {
                    SimpleFeature f = fi.next();
                    String featureId = f.getID();
                    
                    // is the feature id same as before? if so we just have to accumulate
                    // another Statistics object into the zonal stats. Otherwise, build it
                    if(lastFeatureId == null || !lastFeatureId.equals(featureId)) {
                        // basic info
                        stat = new ZonalStatistic();
                        stat.areaLayer = areaLayer;
                        stat.datalayer = info.getPrefixedName();
                        if (info instanceof FeatureTypeInfo) {
                            // field is returned only if the layer is a vector one
                            stat.datafield = field;
                        }

                        // all of the other area attributes
                        LinkedHashMap<String, String> feature = new LinkedHashMap<String, String>();
                        for (AttributeDescriptor att : ((SimpleFeatureType) areaInfo.getFeatureType())
                                .getAttributeDescriptors()) {
                            if (!(att instanceof GeometryDescriptor)) {
                                feature.put(att.getLocalName(), asString(f.getAttribute("z_"
                                        + att.getLocalName())));
                            }
                        }
                        stat.feature = feature;
                        
                        // add to the results
                        stats.add(stat);
                        lastFeatureId = featureId;
                    }
                    
                    // statistics
                    Statistics s = new Statistics(f);
                    if(s.count == null) {
                        s.count = 0l;
                    }
                    if(s.std != null && Double.isNaN(s.std)) {
                        s.std = 0d;
                    }
                    stat.stats.add(s);
                    
                    // if classified add the classification attribute
                    if(classificationGrid != null) {
                        int clazz = ((Number) f.getAttribute("classification")).intValue();
                        if(clazz > 0) {
                            Range range = rasterClassification.getRanges().get(clazz - 1);
                            s.rangeAttribute = RangeUtils.getString(range);
                        }
                    }
                }
            } finally {
                if (fi != null) {
                    fi.close();
                }
            }
        }

        return new ZonalStatistics(stats);
    }

    protected String asString(Object attribute) {
        if (attribute == null) {
            return null;
        } else {
            return attribute.toString();
        }
    }

    /**
     * Fetches, parses and fills in the Classification inputs.
     * 
     * @param input
     *            The process input
     * 
     * @throws WPSInvalidParameterValueException
     *             when problems with the passed input param occur
     */
    protected RasterClassification parseRasterClassifications(String rasterClassificationSpec) {
        if(rasterClassificationSpec == null) {
            return null;
        }
        
        RasterClassification rasterClassification = null;
        try {
            rasterClassification = RasterClassification.parse(rasterClassificationSpec.trim());
        } catch (IllegalArgumentException e) {
            throw new WPSException(e.getMessage(), "InvalidParameterValue", "rasterclassification");
        }

        // Check layer exists
        CoverageInfo coverageInfo = catalog.getCoverageByName(rasterClassification.getLayername());
        if (coverageInfo == null) {
            throw new WPSException("Can't find coverage [" + rasterClassification.getLayername()
                    + "]", "InvalidParameterValue", "rasterclassification");
        }
        rasterClassification.setCoverageInfo(coverageInfo);
        return rasterClassification;
    }

}
