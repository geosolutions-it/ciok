/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.fao;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSStorageHandler;
import org.geoserver.wps.gs.CollectGeometries;
import org.geoserver.wps.gs.CropCoverage;
import org.geoserver.wps.gs.ScaleCoverage;
import org.geoserver.wps.gs.StoreCoverage;
import org.geoserver.wps.gs.StyleCoverage;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.GeometryCollection;

/**
 * The FAO CropRaster classic process
 * 
 * @author Andrea Aime
 * @author ETj <etj at geo-solutions.it>
 * 
 */
@DescribeProcess(title = "cropRaster", description = "Crop polygons pieces from rasters")
public class CropRaster implements FAOProcess {
    protected final static Logger LOGGER = Logging.getLogger(CropRaster.class);

    protected final static String STYLE_TOKEN_RAW = "#RAW";

    protected final static String STYLE_TOKEN_DEFAULT = "#DEFAULT";

    Catalog catalog;

    WPSStorageHandler storage;

    public CropRaster(Catalog catalog, WPSStorageHandler storage) {
        this.catalog = catalog;
        this.storage = storage;
    }

    public CroppedRasters execute(
            @DescribeParameter(name = "zonelayer", description = "Delimiter zonal layer") String zoneLayer,
            @DescribeParameter(name = "zonecql", description = "CQL filter for zonal layer delimiter", min = 0) String zoneCql,
            @DescribeParameter(name = "rasterlayers", description = "List of the raster layers that will be cropped. "
                    + "List is comma separated. Format [namespace:]layername") String rasterLayers,
            @DescribeParameter(name = "rasterstyles", description = "Comma separated list of styles to apply to the layers. "
                    + "Optional parameter; one entry for every rasterlayer. "
                    + "If the parameter is omitted, rasters will not be styled, and will have the same values of the source ones. "
                    + "If the parameter exists, styles will be applied to every layer. "
                    + "You may specify different kinds of styles:\n"
                    + "* A style starting with http:// will be evalued as an external SLD, retrieved and then applied;\n"
                    + "* The string '#DEFAULT' will apply the default style for a given layer;\n"
                    + "* The string '#RAW' will apply no style for a given layer;\n"
                    + "* other strings will be treated as style names defined in the GeoServer instance.", min = 0) String rasterStyles,
            @DescribeParameter(name = "maxwidth", description = "maximum width in pixel of the output raster. "
                    + "Optional integer parameter. If the output cropped raster has a width bigger than maxwidth, "
                    + "it will be rescaled.", min = 0) Integer maxWidth,
            @DescribeParameter(name = "maxheight", description = "maximum height in pixel of the output raster. "
                    + "Optional integer parameter. Same as maxwidth. If both maxwidth and maxheight exist, "
                    + "the raster will be rescaled to the stricter one; Aspect ratio will be preserved.", min = 0) Integer maxHeigth,
            ProgressListener monitor) throws Exception {
        final float PROGRESS_CONFIG = 5.0f;

        if (monitor == null) {
            monitor = new LoggingMonitor(LOGGER, Level.INFO);
        }

        try {
            // parse the request parameters into GT objects
            monitor.started();
            monitor.setTask(Text.text("Parsing request"));

            final SimpleFeatureCollection areafc = retrieveZones(zoneLayer, zoneCql);
            final List<LayerInfo> coverages = parseRasterLayers(rasterLayers);
            final List<CroppedRaster> cropList = parseRasterStyles(rasterStyles, coverages);

            monitor.progress(PROGRESS_CONFIG);

            // loop over the rasters and crop
            monitor.setTask(Text.text("Cropping rasters..."));
            float progress = 1;

            // build the crop shape
            GeometryCollection cropShape = new CollectGeometries().execute(areafc, null);

            for (CroppedRaster croppedInfo : cropList) {
                monitor.setTask(Text.text("Cropping raster " + croppedInfo.getLayerName() + "("
                        + (int) progress + "/" + cropList.size() + ")"));

                try {
                    // compute the cropped raster
                    GridCoverage2D source = (GridCoverage2D) croppedInfo.getCoverage()
                            .getGridCoverage(null, null);
                    GridCoverage2D result = new CropCoverage().execute(source, cropShape, null);

                    // rescale if needed
                    if (maxWidth != null || maxHeigth != null) {
                        result = downScale(result, maxWidth, maxHeigth);
                    }

                    // apply style if needed
                    if (croppedInfo.getStyle() != null) {
                        result = new StyleCoverage().execute(result, croppedInfo.getStyle());
                    }

                    // store the coverage
                    URL location = new StoreCoverage(storage).execute(result);
                    croppedInfo.setFileUrl(location);

                    // dispose the coverage
                    result.dispose(true);
                } catch (Exception exception) {
                    croppedInfo.setException(exception.getClass().getName() + ": "
                            + exception.getMessage());
                    monitor.exceptionOccurred(exception);
                }
                monitor.progress(PROGRESS_CONFIG + (progress++ / cropList.size())
                        * (100.0f - PROGRESS_CONFIG));
            }

            return new CroppedRasters(cropList);
        } catch (Exception e) {
            monitor.exceptionOccurred(e);
            throw e;
        } finally {
            monitor.complete();
        }
    }

    private GridCoverage2D downScale(GridCoverage2D gc, Integer maxw, Integer maxh)
            throws IOException {
        // fetch the source dimensions
        int width = (int) gc.getGridGeometry().getGridRange2D().getWidth();
        int height = (int) gc.getGridGeometry().getGridRange2D().getHeight();

        // compute the ratios
        float wratio = 0;
        float hratio = 0;
        if (maxw != null) {
            wratio = maxw.floatValue() / width; // wratio < 1 ==> should resample
            if (wratio < 1 && LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "W should be downscaled ({0}<{1})", new Object[] { maxw,
                        width });
        }
        if (maxh != null) {
            hratio = maxh.floatValue() / height;
            LOGGER.log(Level.FINE, "H should be downscaled ({0}<{1})",
                    new Object[] { maxh, height });
        }

        // check if we actually have to resample
        if ((maxw != null && wratio < 1) || (maxh != null && hratio < 1)) {
            if (maxw == null) {
                // maxw not defined: let's infer the required width from hratio
                maxw = Integer.valueOf(Math.round(width * hratio));
                wratio = hratio; // force it
                LOGGER.log(Level.FINE, "Setting maxw to {0}", maxw);
            } else if (maxh == null) {
                // maxh not defined: let's infer the required height from wratio
                maxh = Integer.valueOf(Math.round(height * wratio));
                hratio = wratio; // force it
                LOGGER.log(Level.FINE, "Setting maxh to {0}", maxh);
            } else {
                // both constraints defined: we'll take the stricter one
                if (wratio < hratio) {
                    hratio = wratio;
                    maxh = Integer.valueOf(Math.round(height * wratio));
                    LOGGER.log(Level.FINE, "Forcing maxh to {0}", maxh);
                } else {
                    wratio = hratio;
                    maxw = Integer.valueOf(Math.round(width * hratio));
                    LOGGER.log(Level.FINE, "Forcing maxw to {0}", maxw);
                }
            }

            return new ScaleCoverage().execute(gc, wratio, hratio, 0, 0, null);
        } else {
            return gc;
        }

    }

    /**
     * Retrieve the features of the input area layer, and filter them with the provided cql filter.
     */
    private SimpleFeatureCollection retrieveZones(String areaLayer, String areaCql)
            throws IOException {
        FeatureTypeInfo areaFti = catalog.getFeatureTypeByName(areaLayer);
        if (areaFti == null) {
            throw new WPSException("Unknown zone layer " + areaLayer, "InvalidParameterValue",
                    "zonelayer");
        }

        try {
            SimpleFeatureSource fs = (SimpleFeatureSource) areaFti.getFeatureSource(null, null);
            SimpleFeatureCollection fc;
            if (areaCql != null) {
                Filter cqlFilter = CQL.toFilter(areaCql);
                fc = fs.getFeatures(cqlFilter);
            } else {
                fc = fs.getFeatures();
            }

            return fc;

        } catch (CQLException ex) {
            throw new WPSException("Bad zone CQL '" + areaCql + "': " + ex.getMessage(),
                    "InvalidParameterValue", "zonecql");
        }
    }

    /**
     * Fetches, parses the raster layer names and checks the layers exist in geoserver catalog.
     * 
     * @param input
     *            The process input
     * 
     * @throws WPSException
     *             when problems with the passed input param occur
     */
    protected List<LayerInfo> parseRasterLayers(String datalayers) {
        String datalayersarr[] = datalayers.split("\\s*,\\s*");
        List<LayerInfo> ret = new ArrayList<LayerInfo>(datalayersarr.length);

        // scan the layer list
        for (String layerName : datalayersarr) {
            layerName = layerName.trim();

            LayerInfo layer = catalog.getLayerByName(layerName);
            if (layer == null) {
                throw new WPSException("Unknown layer " + layerName, "InvalidParameterValue",
                        "rasterlayers");
            }

            ResourceInfo resourceInfo = layer.getResource();
            if (!(resourceInfo instanceof CoverageInfo)) {
                throw new WPSException("Not a coverage: " + layerName, "InvalidParameterValue",
                        "rasterlayers");
            }
            ret.add(layer);
        }

        return ret;
    }

    /**
     * Grab the various styles and associate them with the raster layers
     * 
     * @param rasterStyles
     * @param layers
     * @return
     */
    protected List<CroppedRaster> parseRasterStyles(String rasterStyles, List<LayerInfo> layers) {
        List<CroppedRaster> ret = new ArrayList<CroppedRaster>(layers.size());

        if (rasterStyles != null) {
            // parse the style list
            final String[] stylesarr = rasterStyles.split("\\s*,\\s*");
            if (stylesarr.length != layers.size()) {
                throw new WPSException("Styles number mismatch (layers:" + layers.size()
                        + " styles:" + stylesarr.length + ")", "InvalidParameterValue",
                        "rasterstyles");
            }

            for (int i = 0; i < layers.size(); i++) {
                LayerInfo layerInfo = layers.get(i);
                String reqStyle = stylesarr[i];

                CroppedRaster croppedInfo = new CroppedRaster();
                ret.add(croppedInfo);
                croppedInfo.setLayerName(layerInfo.getName());
                croppedInfo.setCoverage((CoverageInfo) layerInfo.getResource());
                croppedInfo.setRequestedStyle(reqStyle);

                try {
                    if (STYLE_TOKEN_RAW.equals(reqStyle)) {
                        // no styling
                        croppedInfo.setStyle(null);
                    } else if (STYLE_TOKEN_DEFAULT.equals(reqStyle)) {
                        // default style
                        croppedInfo.setStyle(layerInfo.getDefaultStyle().getStyle());
                    } else if (reqStyle.length() == 0) {
                        // bad entry
                        throw new WPSException("Empty style name at position " + i,
                                "InvalidParameterValue", "rasterstyles");
                    } else if (reqStyle.startsWith("http:")) {
                        croppedInfo.setStyle(parseRemoteStyle(reqStyle));
                    } else {
                        StyleInfo styleInfo = catalog.getStyleByName(reqStyle);
                        if (styleInfo == null) {
                            throw new WPSException("Unknown style " + reqStyle,
                                    "InvalidParameterValue", "rasterstyles");
                        }
                        croppedInfo.setStyle(styleInfo.getStyle());
                    }
                } catch (IOException ex) {
                    throw new WPSException("Error setting style '" + reqStyle + "' to "
                            + layerInfo.getName(), ex);
                }
            }
        } else {
            // rasterstyles param not exists: no styling at all
            for (int i = 0; i < layers.size(); i++) {
                LayerInfo layerInfo = layers.get(i);

                CroppedRaster croppedInfo = new CroppedRaster();
                croppedInfo.setLayerName(layerInfo.getName());
                croppedInfo.setCoverage((CoverageInfo) layerInfo.getResource());
                ret.add(croppedInfo);
            }
        }

        return ret;
    }

    Style parseRemoteStyle(String url) throws IOException {
        final SLDParser stylereader = new SLDParser(CommonFactoryFinder.getStyleFactory(null),
                new URL(url));
        final StyledLayerDescriptor sld = stylereader.parseSLD();
        final StyledLayer sl = sld.getStyledLayers()[0];
        if (sl instanceof NamedLayer) {
            final NamedLayer nl = (NamedLayer) sl;
            return nl.getStyles()[0];
        } else {
            final UserLayer nl = (UserLayer) sl;
            return nl.getUserStyles()[0];
        }
    }

}
