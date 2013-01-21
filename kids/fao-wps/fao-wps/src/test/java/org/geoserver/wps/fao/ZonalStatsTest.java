package org.geoserver.wps.fao;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.RasterZonalStatsTest;
import org.geoserver.wps.gs.VectorZonalStatsTest;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class ZonalStatsTest extends WPSTestSupport {
    
    static final double EPS = 1e-6;

    public static QName BUGSITES = new QName(MockData.SF_URI, "bugsites", MockData.SF_PREFIX);

    public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addPropertiesType(BUGSITES, VectorZonalStatsTest.class
                .getResource("bugsites.properties"), Collections.singletonMap(
                MockData.KEY_SRS_NUMBER, "EPSG:26713"));
        dataDirectory.addPropertiesType(RESTRICTED, VectorZonalStatsTest.class
                .getResource("restricted.properties"), Collections.singletonMap(
                MockData.KEY_SRS_NUMBER, "EPSG:26713"));
        dataDirectory.addCoverage(RasterZonalStatsTest.DEM, RasterZonalStatsTest.class.getResource("sfdem.tiff"), MockData.TIFF, null);
    }

    public void testSingleVector() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
                "  <ows:Identifier>fao:ZonalStats</ows:Identifier>\n" + 
                "  <wps:DataInputs>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>datalayers</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>bugsites</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>datafields</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>cat</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>arealayer</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>restricted</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "  </wps:DataInputs>\n" + 
                "  <wps:ResponseForm>\n" + 
                "    <wps:RawDataOutput mimeType=\"text/xml\">\n" + 
                "      <ows:Identifier>result</ows:Identifier>\n" + 
                "    </wps:RawDataOutput>\n" + 
                "  </wps:ResponseForm>\n" + 
                "</wps:Execute>";
        
        Document doc = postAsDOM(root(), xml);
        // print(doc);
        
        assertXpathEvaluatesTo("1", "count(/ZonalStatistics)", doc);
        assertXpathEvaluatesTo("4", "count(/ZonalStatistics/ZonalStatistic)", doc);
        assertXpathEvaluatesTo("0", "/ZonalStatistics/ZonalStatistic[feature/cat=1]/stats/count", doc);
        assertXpathEvaluatesTo("1", "/ZonalStatistics/ZonalStatistic[feature/cat=4]/stats/count", doc);
        assertXpathEvaluatesTo("84", "/ZonalStatistics/ZonalStatistic[feature/cat=4]/stats/max", doc);
        assertXpathEvaluatesTo("0", "/ZonalStatistics/ZonalStatistic[feature/cat=4]/stats/range", doc);
        assertXpathEvaluatesTo("23", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/count", doc);
        assertXpathEvaluatesTo("57.869565", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/mean", doc);
        assertXpathEvaluatesTo("15.120687", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/std", doc);
    }
    
    public void testSingleRaster() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
                "  <ows:Identifier>fao:ZonalStats</ows:Identifier>\n" + 
                "  <wps:DataInputs>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>datalayers</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>sfdem</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>arealayer</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>restricted</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "  </wps:DataInputs>\n" + 
                "  <wps:ResponseForm>\n" + 
                "    <wps:RawDataOutput mimeType=\"text/xml\">\n" + 
                "      <ows:Identifier>result</ows:Identifier>\n" + 
                "    </wps:RawDataOutput>\n" + 
                "  </wps:ResponseForm>\n" + 
                "</wps:Execute>";
        
        Document doc = postAsDOM(root(), xml);
        // print(doc);

        // just the basic tests
        assertXpathEvaluatesTo("1", "count(/ZonalStatistics)", doc);
        assertXpathEvaluatesTo("4", "count(/ZonalStatistics/ZonalStatistic)", doc);
        assertXpathEvaluatesTo("0", "count(/ZonalStatistics/ZonalStatistic/datafield)", doc);
        // check some counts
        assertXpathEvaluatesTo("424", "/ZonalStatistics/ZonalStatistic[feature/cat=1]/stats/count", doc);
        assertXpathEvaluatesTo("218", "/ZonalStatistics/ZonalStatistic[feature/cat=2]/stats/count", doc);
        assertXpathEvaluatesTo("18629", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/count", doc);
        assertXpathEvaluatesTo("1697", "/ZonalStatistics/ZonalStatistic[feature/cat=4]/stats/count", doc);
        // check the stats of a single one
        assertXpathEvaluatesTo("1281", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/min", doc);
        assertXpathEvaluatesTo("1695", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/max", doc);
        assertXpathEvaluatesTo("414", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/range", doc);
        assertXpathEvaluatesTo("27431470", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/sum", doc);
        assertXpathEvaluatesTo("1472.514359", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/mean", doc);
        assertXpathEvaluatesTo("93.61446", "/ZonalStatistics/ZonalStatistic[feature/cat=3]/stats/std", doc);
    }
    
    public void testClassifiedRaster() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute service=\"WPS\" version=\"1.0.0\"\n"
                + "    xmlns:wps=\"http://www.opengis.net/wps/1.0.0\"\n"
                + "    xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n"
                + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 ../wpsExecute_request.xsd\">\n"
                + "\n"
                + "    <ows:Identifier>fao:ZonalStats</ows:Identifier>\n"
                + "\n"
                + "    <wps:DataInputs>\n"
                + "        <wps:Input>\n"
                + "            <ows:Identifier>arealayer</ows:Identifier>\n"
                + "            <wps:Data>\n"
                + "                <wps:LiteralData>restricted</wps:LiteralData>\n"
                + "            </wps:Data>\n"
                + "        </wps:Input>\n"
                + "\n"
                + "        <wps:Input>\n"
                + "            <ows:Identifier>areacqlfilter</ows:Identifier>\n"
                + "            <wps:Data>\n"
                + "                <wps:LiteralData>cat&lt;3</wps:LiteralData>\n"
                + "            </wps:Data>\n"
                + "        </wps:Input>\n"
                + "\n"
                + "        <wps:Input>\n"
                + "            <ows:Identifier>datalayers</ows:Identifier>\n"
                + "            <wps:Data>\n"
                + "                <wps:LiteralData>sfdem</wps:LiteralData>\n"
                + "            </wps:Data>\n"
                + "        </wps:Input>\n"
                + "\n"
                + "        <wps:Input>\n"
                + "            <ows:Identifier>datafields</ows:Identifier>\n"
                + "            <wps:Data>\n"
                + "                <wps:LiteralData></wps:LiteralData>\n"
                + "            </wps:Data>\n"
                + "        </wps:Input>\n"
                + "\n"
                + "        <wps:Input>\n"
                + "            <ows:Identifier>rasterclassification</ows:Identifier>\n"
                + "            <wps:Data>\n"
                + "                <wps:LiteralData>sfdem(;1150)[1150;1190)</wps:LiteralData>\n"
                + "            </wps:Data>\n"
                + "        </wps:Input>\n"
                + "\n"
                + "    </wps:DataInputs>\n"
                + "\n"
                + "    <wps:ResponseForm>\n"
                + "        <wps:RawDataOutput> "
                + "            <ows:Identifier>result</ows:Identifier>\n"
                + "        </wps:RawDataOutput>\n" 
                + "    </wps:ResponseForm>\n" 
                + "</wps:Execute>";
        
        Document dom = postAsDOM(root(), xml); // postAsDOM(root(), xml);
        //print(dom);

        Document expected = dom(getClass().getResourceAsStream("rasterclass.xml"));
        XMLUnit.setIgnoreWhitespace(true);
        Diff difference = new Diff(expected, dom);
        // System.out.println(difference.toString());
        assertTrue(difference.similar());
        assertTrue(difference.identical());
    }
    
    public void testFieldMismatch() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
            "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
            "  <ows:Identifier>fao:ZonalStats</ows:Identifier>\n" + 
            "  <wps:DataInputs>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>datalayers</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>bugsites</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>datafields</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>cat1,cat2</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>arealayer</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>restricted</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "  </wps:DataInputs>\n" + 
            "  <wps:ResponseForm>\n" + 
            "    <wps:RawDataOutput mimeType=\"text/xml\">\n" + 
            "      <ows:Identifier>result</ows:Identifier>\n" + 
            "    </wps:RawDataOutput>\n" + 
            "  </wps:ResponseForm>\n" + 
            "</wps:Execute>";
        
        Document doc = postAsDOM(root(), xml);
        //print(doc);
        
        assertXpathEvaluatesTo("NoApplicableCode", "/ows:ExceptionReport/ows:Exception/@exceptionCode", doc);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String exception = xpath.evaluate("/ows:ExceptionReport/ows:Exception/ows:ExceptionText", doc);
        assertTrue(exception.contains("length mismatch (1,2)"));
    }
    
    public void testMissingParameter() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
        "  <ows:Identifier>fao:ZonalStats</ows:Identifier>\n" + 
        "  <wps:DataInputs>\n" + 
        "    <wps:Input>\n" + 
        "      <ows:Identifier>datafields</ows:Identifier>\n" + 
        "      <wps:Data>\n" + 
        "        <wps:LiteralData>cat</wps:LiteralData>\n" + 
        "      </wps:Data>\n" + 
        "    </wps:Input>\n" + 
        "    <wps:Input>\n" + 
        "      <ows:Identifier>arealayer</ows:Identifier>\n" + 
        "      <wps:Data>\n" + 
        "        <wps:LiteralData>restricted</wps:LiteralData>\n" + 
        "      </wps:Data>\n" + 
        "    </wps:Input>\n" +
        "  </wps:DataInputs>\n" + 
        "  <wps:ResponseForm>\n" + 
        "    <wps:RawDataOutput mimeType=\"text/xml\">\n" + 
        "      <ows:Identifier>result</ows:Identifier>\n" + 
        "    </wps:RawDataOutput>\n" + 
        "  </wps:ResponseForm>\n" + 
        "</wps:Execute>";
        
        Document doc = postAsDOM(root(), xml);
        // print(doc);
        
        assertXpathEvaluatesTo("NoApplicableCode", "/ows:ExceptionReport/ows:Exception/@exceptionCode", doc);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String exception = xpath.evaluate("/ows:ExceptionReport/ows:Exception/ows:ExceptionText", doc);
        assertTrue(exception.contains("datalayers"));
    }
    
    public void testEmptyArea() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
            "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
            "  <ows:Identifier>fao:ZonalStats</ows:Identifier>\n" + 
            "  <wps:DataInputs>\n" +
            "    <wps:Input>\n" + 
            "      <ows:Identifier>datalayers</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>bugsites</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>datafields</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>cat</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>arealayer</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>restricted</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" +
            "    <wps:Input>\n" + 
            "      <ows:Identifier>areacqlfilter</ows:Identifier>\n" + 
            "      <wps:Data>\n" + 
            "        <wps:LiteralData>cat&gt;1000</wps:LiteralData>\n" + 
            "      </wps:Data>\n" + 
            "    </wps:Input>\n" + 
            "  </wps:DataInputs>\n" + 
            "  <wps:ResponseForm>\n" + 
            "    <wps:RawDataOutput mimeType=\"text/xml\">\n" + 
            "      <ows:Identifier>result</ows:Identifier>\n" + 
            "    </wps:RawDataOutput>\n" + 
            "  </wps:ResponseForm>\n" + 
            "</wps:Execute>";
        
        Document doc = postAsDOM(root(), xml);
        // print(doc);
        
        assertXpathEvaluatesTo("1", "count(/ZonalStatistics)", doc);
        assertXpathEvaluatesTo("0", "count(/ZonalStatistics/ZonalStatistic)", doc);
    }
    
    
}
