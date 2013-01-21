package it.geosolutions.geobatch.gaez.utils.rules.ruleD;

import it.geosolutions.geobatch.gaez.utils.AreaRationalizer;

import java.io.File;

import javax.media.jai.JAI;

import org.junit.Before;
import org.junit.Test;

public class AreaRationalizerTest {
	@Before
	public void before(){
		JAI.getDefaultInstance().getTileCache().setMemoryThreshold(0.75f);
		JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024*1024*512);
	}
	
	@Test
	public void Test() throws Exception {

//		JAI.getDefaultInstance().getTileCache().setMemoryThreshold(0.75f);
		JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024*1024*756);
		
		File[][] areas = new File[][] {
				{
						new File(
								"src/test/resources/test-data/stats/areas/area0_1.tif"),
						new File(
								"src/test/resources/test-data/stats/areas/area0_2.tif"),
						new File(
								"src/test/resources/test-data/stats/areas/area0_3.tif"),
						new File(
								"src/test/resources/test-data/stats/areas/area0_4.tif"), },
				{
						new File(
								"src/test/resources/test-data/stats/areas/area1_1.tif"),
						new File(
								"src/test/resources/test-data/stats/areas/area1_2.tif"),
						new File(
								"src/test/resources/test-data/stats/areas/area1_3.tif"),
						new File(
								"src/test/resources/test-data/stats/areas/area1_4.tif"), } };

		File[][] areasRationalized = new File[][] {
				{
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area0_1_ratio.tif"),
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area0_2_ratio.tif"),
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area0_3_ratio.tif"),
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area0_4_ratio.tif"), },
				{
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area1_1_ratio.tif"),
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area1_2_ratio.tif"),
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area1_3_ratio.tif"),
						new File(
								"src/test/resources/test-data/stats/areas_ratio/area1_4_ratio.tif"), } };

		double[][] areasNoData = new double[][] { { 0, 0, 0, 0, },
				{ 0, 0, 0, 0, } };
		
		AreaRationalizer.produce(areasRationalized, areas, areasNoData);

	}

}
