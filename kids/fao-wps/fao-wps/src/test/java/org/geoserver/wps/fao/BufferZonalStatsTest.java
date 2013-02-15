package org.geoserver.wps.fao;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.Collections;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.RasterZonalStatsTest;
import org.geoserver.wps.gs.VectorZonalStatsTest;
import org.w3c.dom.Document;

public class BufferZonalStatsTest extends WPSTestSupport {
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

    public void testSingleVectorLayer() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    		"<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
    		"  <ows:Identifier>fao:BufferZonalStats</ows:Identifier>\n" + 
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
    		"      <ows:Identifier>areacenter</ows:Identifier>\n" + 
    		"      <wps:Data>\n" + 
    		"        <wps:LiteralData>-103.7,44.4</wps:LiteralData>\n" + 
    		"      </wps:Data>\n" + 
    		"    </wps:Input>\n" + 
    		"    <wps:Input>\n" + 
    		"      <ows:Identifier>arearadii</ows:Identifier>\n" + 
    		"      <wps:Data>\n" + 
    		"        <wps:LiteralData>1,10,30</wps:LiteralData>\n" + 
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
        
        assertXpathEvaluatesTo("1", "count(/BufferZonalStatistics)", doc);
        assertXpathEvaluatesTo("3", "count(/BufferZonalStatistics/BufferZonalStatistic)", doc);
        assertXpathEvaluatesTo("1", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=1]/stats/count", doc);
        assertXpathEvaluatesTo("87", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=1]/stats/max", doc);
        assertXpathEvaluatesTo("45", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=10]/stats/count", doc);
        assertXpathEvaluatesTo("90", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=10]/stats/max", doc);
        assertXpathEvaluatesTo("90", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=30]/stats/count", doc);
        assertXpathEvaluatesTo("90", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=30]/stats/max", doc);
    }
    
    public void testSingleRasterLayer() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
                "  <ows:Identifier>fao:BufferZonalStats</ows:Identifier>\n" + 
                "  <wps:DataInputs>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>datalayers</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>sfdem</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>areacenter</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>-103.7,44.425</wps:LiteralData>\n" + 
                "      </wps:Data>\n" + 
                "    </wps:Input>\n" + 
                "    <wps:Input>\n" + 
                "      <ows:Identifier>arearadii</ows:Identifier>\n" + 
                "      <wps:Data>\n" + 
                "        <wps:LiteralData>5,10,150</wps:LiteralData>\n" + 
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
        print(doc);
        
        assertXpathEvaluatesTo("1", "count(/BufferZonalStatistics)", doc);
        assertXpathEvaluatesTo("3", "count(/BufferZonalStatistics/BufferZonalStatistic)", doc);
        // these should be set once we have stable outputs for raster statistics
//        assertXpathEvaluatesTo("1", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=1]/stats/count", doc);
//        assertXpathEvaluatesTo("87", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=1]/stats/max", doc);
//        assertXpathEvaluatesTo("45", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=10]/stats/count", doc);
//        assertXpathEvaluatesTo("90", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=10]/stats/max", doc);
//        assertXpathEvaluatesTo("90", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=30]/stats/count", doc);
//        assertXpathEvaluatesTo("90", "/BufferZonalStatistics/BufferZonalStatistic[bufferradius=30]/stats/max", doc);
    }
    
}
