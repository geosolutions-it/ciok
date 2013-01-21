/*
 */

package org.geoserver.wps.fao;

import jaitools.numeric.Range;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ETj <etj at geo-solutions.it>
 */
public class RangeUtils {

    private final static String RE_OPEN  = "(\\(|\\[)"; // char for opening a range
    private final static String RE_CLOSE = "(\\)|\\])"; // char for closing range
    private final static String RE_NUM   = "(\\-?\\d+(?:\\.\\d*)?)?"; // a nullable general number

    private final static String RANGE_REGEX = RE_OPEN + RE_NUM + ";" + RE_NUM + RE_CLOSE ; // + "\\z";
    private final static Pattern RANGE_PATTERN = Pattern.compile(RANGE_REGEX);

    private final static String RANGELIST_REGEX = "(" + RE_OPEN + RE_NUM + ";" + RE_NUM + RE_CLOSE + ")+" ; // "\\z";
    private final static Pattern RANGELIST_PATTERN = Pattern.compile(RANGELIST_REGEX);

    /**
     * Return the parsed Range.
     *
     * @param sRange
     * @return
     */
    public static Range<Double> parseRange(String sRange) {
        Matcher m = RANGE_PATTERN.matcher(sRange);
        
        if( ! m.matches())
            throw new IllegalArgumentException("Bad range definition '"+sRange+"'");

        return loopableParseRange(m, sRange);
    }

    /**
     * Return the parsed Range.
     *
     * @param sRange
     * @return
     */
    protected static Range<Double> loopableParseRange(Matcher m, String sRange) {
        Double min = null;
        Double max = null;

        if(m.groupCount() != 4) {
            throw new IllegalStateException("Range returned wrong group count ("+sRange+") : " + m.groupCount());
        }

        if(m.group(2) != null) {
            min = new Double(m.group(2));
        }
        if(m.group(3) != null) {
            max = new Double(m.group(3));
        }

        boolean inclmin;
        if(m.group(1).equals("("))
            inclmin = false;
        else if(m.group(1).equals("["))
            inclmin = true;
        else
            throw new IllegalArgumentException("Bad min delimiter ("+sRange+")");

        boolean inclmax;
        if(m.group(4).equals(")"))
            inclmax = false;
        else if(m.group(4).equals("]"))
            inclmax = true;
        else
            throw new IllegalArgumentException("Bad max delimiter ("+sRange+")");

        if(min != null && max != null && min>max)
            throw new IllegalArgumentException("Bad min/max relation ("+sRange+")");

        return new Range<Double>(min, inclmin, max, inclmax);
    }

    public static List<Range<Double>> parseRanges(String sRangeList) {

        // check that the whole input string is a list of ranges
        Matcher m = RANGELIST_PATTERN.matcher(sRangeList);
        if(! m.matches()) 
            throw new IllegalArgumentException("Bad range definition '"+sRangeList+"'");

        // fetch every single range
        m = RANGE_PATTERN.matcher(sRangeList);
        
        List<Range<Double>> ret = new ArrayList<Range<Double>>();
        while(m.find()) {
            Range<Double> range = loopableParseRange(m, sRangeList);
            ret.add(range);
        }

        return ret;
    }

    public static String getString(Range range) {
        String ret = range.toString();
        if(ret.startsWith("Range"))
            return ret.substring(5);
        else
            return ret;
    }

}
