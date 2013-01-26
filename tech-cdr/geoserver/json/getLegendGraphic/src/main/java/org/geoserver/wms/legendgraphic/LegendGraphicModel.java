package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class LegendGraphicModel {

    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;

    /**
     * Singleton shape painter to serve all legend requests. We can use a single shape painter instance as long as it remains thread safe.
     */
    private static final StyledShapePainter shapePainter = new StyledShapePainter();

    /**
     * used to create sample point shapes with LiteShape (not lines nor polygons)
     */
    private static final GeometryFactory geomFac = new GeometryFactory();

    /**
     * set to <code>true</code> when <code>abort()</code> gets called, indicates that the rendering of the legend graphic should stop gracefully as
     * soon as possible
     */
    private boolean renderingStopRequested;

    /**
     * Just a holder to avoid creating many polygon shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleRect;

    /**
     * Just a holder to avoid creating many line shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleLine;

    /**
     * Just a holder to avoid creating many point shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 samplePoint;

    private final GetLegendGraphicRequest request;

    public LegendGraphicModel(GetLegendGraphicRequest r) {
        super();
        this.request = r;

    }

    public GetLegendGraphicRequest getRequest() {
        return request;
    }

    public static Rule[] buildApplicableRules(GetLegendGraphicRequest request)
            throws ServiceException {

        // the style we have to build a legend for
        List<Style> gt2Style = request.getStyles();
        if (gt2Style == null || gt2Style.isEmpty()) {
            throw new ServiceException("Bad styles argument: empty or null");
        }
        Style style = gt2Style.get(0);

        // // width and height, we might have to rescale those in case of DPI usage
        // int w = request.getWidth();
        // int h = request.getHeight();
        //
        // // apply dpi rescale
        // double dpi = RendererUtilities.getDpi(request.getLegendOptions());
        // double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        // if(dpi != standardDpi) {
        // double scaleFactor = dpi / standardDpi;
        // w = (int) Math.round(w * scaleFactor);
        // h = (int) Math.round(h * scaleFactor);
        // RescaleStyleVisitor dpiVisitor = new RescaleStyleVisitor(scaleFactor);
        // dpiVisitor.visit(gt2Style);
        // gt2Style = (Style) dpiVisitor.getCopy();
        // }

        // apply UOM rescaling if we have a scale
        if (request.getScale() > 0) {
            double pixelsPerMeters = RendererUtilities.calculatePixelsPerMeterRatio(
                    request.getScale(), request.getLegendOptions());
            UomRescaleStyleVisitor rescaleVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
            rescaleVisitor.visit(style);
            style = (Style) rescaleVisitor.getCopy();
        }

        final List<FeatureType> layers = request.getLayers();
        if (layers == null || layers.isEmpty()) {
            throw new ServiceException("Bad styles argument: empty or null");
        }
//        FeatureType layer = layers.get(0);

//        boolean strict = request.isStrict();
//        final boolean buildRasterLegend = (!strict && layer == null && LegendUtils
//                .checkRasterSymbolizer(style)) || LegendUtils.checkGridLayer(layer);
        // if (buildRasterLegend) {
        // final RasterLayerLegendHelper rasterLegendHelper = new RasterLayerLegendHelper(request);
        // final BufferedImage image = rasterLegendHelper.getLegend();
        // return image;
        // }

        // final SimpleFeature sampleFeature;
//        final Feature sampleFeature;
//        if (layer == null) {
//            sampleFeature = createSampleFeature();
//        } else {
//            sampleFeature = createSampleFeature(layer);
//        }
        final FeatureTypeStyle[] ftStyles = style.featureTypeStyles().toArray(
                new FeatureTypeStyle[0]);
        final double scaleDenominator = request.getScale();

        final List<String> ruleNames = request.getRules();
        if (ruleNames != null && !ruleNames.isEmpty()) {
            Iterator<String> it = ruleNames.iterator();
            final List<Rule> applicableRules = new ArrayList<Rule>();
            while (it.hasNext()) {
                String ruleName = it.next();

                Rule rule = LegendUtils.getRule(ftStyles, ruleName);
                if (rule == null) {
                    throw new ServiceException("Specified style does not contains a rule named "
                            + ruleName);
                }
                applicableRules.add(rule);

            }
            return applicableRules.toArray(new Rule[] {});
        }
        // final NumberRange<Double> scaleRange = NumberRange.create(scaleDenominator,
        // scaleDenominator);
        return LegendUtils.getApplicableRules(ftStyles, scaleDenominator);

    }

    private static SimpleFeature createSampleFeature() {
        SimpleFeatureType type;
        try {
            type = DataUtilities.createType("Sample", "the_geom:Geometry");
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return SimpleFeatureBuilder.template((SimpleFeatureType) type, null);
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the rendering of the legend graphic.
     * 
     * @param schema the schema for which to create a sample Feature instance
     * 
     * @return
     * 
     * @throws ServiceException
     */
    private static Feature createSampleFeature(FeatureType schema) throws ServiceException {
        Feature sampleFeature;
        try {
            if (schema instanceof SimpleFeatureType) {
                sampleFeature = SimpleFeatureBuilder.template((SimpleFeatureType) schema, null);
            } else {
                sampleFeature = DataUtilities.templateFeature(schema);
            }
        } catch (IllegalAttributeException e) {
            throw new ServiceException(e);
        }
        return sampleFeature;
    }

}
