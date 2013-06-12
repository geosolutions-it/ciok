package org.geoserver.wms.legendgraphic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

public class LegendGraphicModel {

    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;

    private final GetLegendGraphicRequest request;
    private Style style;

    public LegendGraphicModel(GetLegendGraphicRequest r) {
        super();
        this.request = r;
        // the style we have to build a legend for
        List<Style> gt2Style = request.getStyles();
        if (gt2Style == null || gt2Style.isEmpty()) {
            throw new ServiceException("Bad styles argument: empty or null");
        }
        style = gt2Style.get(0);

        // apply UOM rescaling if we have a scale
        if (request.getScale() > 0) {
            double pixelsPerMeters = RendererUtilities.calculatePixelsPerMeterRatio(
                    request.getScale(), request.getLegendOptions());
            UomRescaleStyleVisitor rescaleVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
            rescaleVisitor.visit(style);
            style = (Style) rescaleVisitor.getCopy();
        }

    }

    public GetLegendGraphicRequest getRequest() {
        return request;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public static Rule[] buildApplicableRules(GetLegendGraphicRequest request, Style style)
            throws ServiceException {

        final List<FeatureType> layers = request.getLayers();
        if (layers == null || layers.isEmpty()) {
            throw new ServiceException("Bad styles argument: empty or null");
        }

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
