/*
 */

package org.geoserver.wps.fao;

import jaitools.numeric.Range;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;

/**
 *
 * @author ETj <etj at geo-solutions.it>
 */
public class RasterClassification {

    private static final int DEFAULT_BAND = 0;

    final private String layername;
    final private int band;
    final private List<Range<Double>> ranges;

    private CoverageInfo coverageInfo;

    /**
     * @param line String in the form <TT>(band"@")? layername range+</TT>
     *
     */

    public static RasterClassification parse(String line)
        throws IllegalArgumentException {

        int atidx = line.indexOf('@');
        int p1idx = line.indexOf('(');
        int p2idx = line.indexOf('[');

        // take the first occurence between existing "(" and "["
        int rangeidx = Math.min(p1idx, p2idx);
        if(rangeidx == -1)
            rangeidx = Math.max(p1idx, p2idx);
        if(rangeidx == -1)
            throw new IllegalArgumentException("No range defined");

        String layerName = line.substring(atidx+1, rangeidx);
        layerName = layerName.trim();
        if(layerName.length() == 0)
            throw new IllegalArgumentException("No layer name defined");

        int band = DEFAULT_BAND;
        if(atidx != -1) {
            String sband = line.substring(0,atidx).trim();
            try {
                band = Integer.valueOf(sband);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Bad band definition ("+sband+")");
            }
        }

        String ranges = line.substring(rangeidx);
        List<Range<Double>> rangeList = RangeUtils.parseRanges(ranges);

        return new RasterClassification(layerName, band, rangeList);
    }

    public RasterClassification(String layername, int band, List<Range<Double>> rangeList) {
        this.layername = layername;
        this.band = band;
        this.ranges = Collections.unmodifiableList(rangeList);
    }

    public String getLayername() {
        return layername;
    }
    
    public int getBand() {
        return band;
    }

    public List<Range<Double>> getRanges() {
        return ranges;
    }

    public CoverageInfo getCoverageInfo() {
        return coverageInfo;
    }

    public void setCoverageInfo(CoverageInfo coverageInfo) {
        this.coverageInfo = coverageInfo;
    }

}
