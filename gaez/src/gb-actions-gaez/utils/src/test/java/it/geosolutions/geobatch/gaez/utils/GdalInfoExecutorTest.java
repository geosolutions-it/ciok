/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://code.google.com/p/geobatch/
 *  Copyright (C) 2007-2011 GeoSolutions S.A.S.
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
package it.geosolutions.geobatch.gaez.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class GdalInfoExecutorTest {
	final static Logger LOGGER = LoggerFactory
			.getLogger(GdalInfoExecutor.class);
	
	private boolean isData=false;
	private File dataImage;
	private File dataTemplate;
	private File csvOut;

	/**
	 * relative
	 */
	@Before
	public void testData(){
		dataImage=new File("src/test/resources/test-data/idrisi/et0_1960.rst");
		dataTemplate=new File("src/main/resources/transform.template");
		try {
			csvOut= File.createTempFile("transform_out",".template");
			
			if (dataImage.exists() && dataTemplate.exists())
				isData=true;
			else
				isData=false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			isData=false;
		}
	}
	
	@After
	public void removeData(){
		if (csvOut!=null && csvOut.exists())
			csvOut.delete();
	}
	
	/**
	 * TODO JUNIT tests
	 * 
	 * @param args
	 * @throws IOException
	 */
	@Test
	public void Test() throws IOException {

		if (!isData)
			return;
		
		final File rst = dataImage;

		final File template = dataTemplate;

		final String xml = "<GDALRasterAttributeTable><FieldDefn index=\"0\"><Name>Value</Name><Type>0</Type><Usage>0</Usage></FieldDefn><FieldDefn index=\"1\"><Name>Value_1</Name><Type>0</Type><Usage>5</Usage></FieldDefn><FieldDefn index=\"2\"><Name>Red</Name><Type>0</Type><Usage>6</Usage></FieldDefn><FieldDefn index=\"3\"><Name>Green</Name><Type>0</Type><Usage>7</Usage></FieldDefn><FieldDefn index=\"4\"><Name>Blue</Name><Type>0</Type><Usage>8</Usage></FieldDefn><FieldDefn index=\"5\"><Name>Alpha</Name><Type>0</Type><Usage>9</Usage></FieldDefn><FieldDefn index=\"6\"><Name>Class_name</Name><Type>2</Type><Usage>2</Usage></FieldDefn><Row index=\"0\"><F>1</F><F>1</F><F>231</F><F>0</F><F>23</F><F>255</F><F>&gt;75% Cultivated land</F></Row><Row index=\"1\"><F>2</F><F>2</F><F>208</F><F>0</F><F>46</F><F>255</F><F>&gt;75% Forest land</F></Row><Row index=\"2\"><F>3</F><F>3</F><F>185</F><F>0</F><F>69</F><F>255</F><F>&gt;75% Grass and woodland</F></Row><Row index=\"3\"><F>4</F><F>4</F><F>162</F><F>0</F><F>92</F><F>255</F><F>&gt;75% Barren land</F></Row><Row index=\"4\"><F>5</F><F>5</F><F>139</F><F>0</F><F>115</F><F>255</F><F>50-75% Cultivated land</F></Row><Row index=\"5\"><F>6</F><F>6</F><F>115</F><F>0</F><F>139</F><F>255</F><F>50-75% Forest land</F></Row><Row index=\"6\"><F>7</F><F>7</F><F>92</F><F>0</F><F>162</F><F>255</F><F>50-75% Grass and woodland</F></Row><Row index=\"7\"><F>8</F><F>8</F><F>69</F><F>0</F><F>185</F><F>255</F><F>50-75% Barren land</F></Row><Row index=\"8\"><F>9</F><F>9</F><F>46</F><F>0</F><F>208</F><F>255</F><F>&gt;50% Built-up land</F></Row><Row index=\"9\"><F>10</F><F>10</F><F>23</F><F>0</F><F>231</F><F>255</F><F>Land cover associations</F></Row><Row index=\"10\"><F>11</F><F>11</F><F>0</F><F>0</F><F>255</F><F>255</F><F>Water</F></Row></GDALRasterAttributeTable>";

		Document doc = GdalInfoExecutor.parser(xml);

		final Element element = doc.getRootElement();
		System.out.println(GdalInfoExecutor.process(GdalInfoExecutor.convertToDOM(element.getDocument()),
				template, csvOut));

		// File out=gdalinfoToCSV(rst, template, csvOut);
		// if (out!=null)
		// System.out.println("OK");
		// else
		// System.out.println("NOT OK");

	}

	@Test
	public void TestGdallocationinfo(){
		if (!isData)
			return;
		try {
			Double d=GdalInfoExecutor.gdallocationinfo(dataImage.getAbsolutePath());
			System.out.println("Double:"+ d);
			Assert.assertEquals(Double.valueOf(-9), d);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void TestGdalinfo(){
		if (!isData)
			return;
		try {
			
			Map<String,Double> map=GdalInfoExecutor.getMaxMinNoData(new File("src/test/resources/test-data/stats/ruleA/res01_mcl_h3a22050.tif"));//res01_mc2_c2a22020.tif"));
			System.out.println("Double:"+ map);
//			Assert.assertEquals(Double.valueOf(-9), d);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
