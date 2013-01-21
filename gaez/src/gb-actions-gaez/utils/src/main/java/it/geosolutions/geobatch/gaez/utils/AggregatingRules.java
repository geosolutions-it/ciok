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

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.apache.commons.io.IOUtils;
import org.geotools.resources.image.ImageUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.jai.operator.ImageReadDescriptor;

/**
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @author Daniele Romagnoli @geo-solutions.it
 * 
 */
public class AggregatingRules {

	/**
	 * Default logger
	 */
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(AggregatingRules.class);

	private RenderedOp areas[][];
	private FileImageInputStream fisAreas[][];
	private ImageReader irAreas[][];
	private double noDataAreas[][];

	private RenderedOp gauls[][];
	private FileImageInputStream fisGauls[][];
	private ImageReader irGauls[][];
	private double noDataGauls[][];

	private int size;
	private int subSize;

	// is this object initialized
	private boolean initialized = false;

	/**
	 * 
	 * @param gauls
	 * @param areas
	 * @throws Exception
	 */
	public AggregatingRules(final File gauls[][], final File areas[][])
			throws Exception {

		initialized = (open(gauls, areas) && calculateNoData());

	}

	public AggregatingRules(final File gauls[][], final double gaulsNoData[][],
			final File areas[][], final double areasNoData[][])
			throws Exception {
		initialized = open(gauls, areas);
		noDataAreas = areasNoData;
		noDataGauls = gaulsNoData;

	}

	/**
	 * 
	 * @param gauls
	 * @param areas
	 * @throws Exception
	 */
	private boolean calculateNoData() throws Exception {

		try {
			noDataAreas = new double[size][subSize];
			noDataGauls = new double[size][subSize];

			/**
			 * i e ADMj
			 */
			for (int i = 0; i < size; i++) {
				/**
				 * j - index used for administrative units=1,...,M
				 */
				for (int j = 0; j < subSize; j++) {
					// to get noData
					noDataAreas[i][j] = getNoData(this.areas[i][j])
							.doubleValue();
					// to get noData
					noDataGauls[i][j] = getNoData(this.gauls[i][j])
							.doubleValue();
				}
			}

		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);

			// dispose this object
			dispose(true);

			throw e;
		}

		return true;

	}

	public boolean open(final File gauls[][], final File areas[][])
			throws Exception {
		final int areaSize = areas.length;
		final int gaulSize = gauls.length;

		if (areaSize != gaulSize || areaSize == 0) {
			throw new Exception("bad argument length");
		} else {
			size = areaSize;
		}

		// checks
		final int areaSubSize = areas[0].length;
		final int gaulSubSize = gauls[0].length;

		if (areaSubSize != gaulSubSize || areaSubSize == 0) {
			throw new Exception("bad Sub argument length");
		} else {
			subSize = areaSubSize;
		}

		try {
			fisAreas = new FileImageInputStream[size][subSize];
			irAreas = new TIFFImageReader[size][subSize];
			this.areas = new RenderedOp[size][subSize];

			fisGauls = new FileImageInputStream[size][subSize];
			irGauls = new TIFFImageReader[size][subSize];
			this.gauls = new RenderedOp[size][subSize];

			final TIFFImageReaderSpi spi = new TIFFImageReaderSpi();
			/**
			 * i e ADMj
			 */
			for (int i = 0; i < size; i++) {
				/**
				 * j - index used for administrative units=1,...,M
				 */
				for (int j = 0; j < subSize; j++) {

					irAreas[i][j] = spi.createReaderInstance();
					fisAreas[i][j] = new FileImageInputStream(areas[i][j]);
					// spi.canDecodeInput(input)
					irAreas[i][j].setInput(fisAreas[i][j]);

					irGauls[i][j] = spi.createReaderInstance();
					// spi.canDecodeInput(input)
					fisGauls[i][j] = new FileImageInputStream(gauls[i][j]);
					irGauls[i][j].setInput(fisGauls[i][j]);

					ImageReadParam rp = new ImageReadParam();
					// rp.setSourceRegion(new Rectangle(0, 0, 1, 1));

					// ImageLayout il=new ImageLayout();
					// RenderingHints r=new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
					// il);
					// il.setTileWidth(1).setTileHeight(1);

					this.areas[i][j] = ImageReadDescriptor.create(
							fisAreas[i][j], 0, false, false, false, null, null,
							rp, irAreas[i][j], null);

					this.gauls[i][j] = ImageReadDescriptor.create(
							fisGauls[i][j], 0, false, false, false, null, null,
							rp, irGauls[i][j], null);

				}
			}

		} catch (IOException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);

			// dispose this object
			dispose(true);

			throw e;
		}

		return true;

	}

	private void cleanCache(final RenderedOp op) {
		if (op != null) {
			try {
				ImageUtilities.disposePlanarImageChain(PlanarImage
						.wrapRenderedImage(op));
			} catch (Exception e) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug(e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * disposer
	 */
	public void dispose(final boolean cacheClean) {
		/**
		 * i e ADMj
		 */
		for (int i = 0; i < size; i++) {
			/**
			 * j - index used for administrative units=1,...,M
			 */
			for (int j = 0; j < subSize; j++) {
				try {
					if (this.areas[i][j] != null) {
						if (cacheClean)
							cleanCache(this.areas[i][j]);
						this.areas[i][j].dispose();
					}
				} catch (Exception innerE) {
					// eat
				}
				try {
					if (irAreas[i][j] != null)
						irAreas[i][j].dispose();
				} catch (Exception innerE) {
					// eat
				}
				try {
					if (fisAreas[i][j] != null)
						fisAreas[i][j].close();
				} catch (Exception innerE) {
					// eat
				}

				try {
					if (this.gauls[i][j] != null) {
						if (cacheClean)
							cleanCache(this.gauls[i][j]);
						this.gauls[i][j].dispose();
					}
				} catch (Exception innerE) {
					// eat
				}
				try {
					if (irGauls[i][j] != null)
						irGauls[i][j].dispose();
				} catch (Exception innerE) {
					// eat
				}
				try {
					if (fisGauls[i][j] != null)
						fisGauls[i][j].close();
				} catch (Exception innerE) {
					// eat
				}
			} // end for
		} // end for

		initialized = false;
	}

	/**
	 * @return the size
	 */
	public final int getSize() {
		return size;
	}

	/**
	 * @return the subSize
	 */
	public final int getSubSize() {
		return subSize;
	}

	/**
	 * @return the areas
	 */
	public final RenderedOp[][] getAreas() {
		return areas;
	}

	/**
	 * @return the gauls
	 */
	public final RenderedOp[][] getGauls() {
		return gauls;
	}

	/**
	 * @return the initialized
	 */
	public final boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return the noDataAreas
	 */
	public final double[][] getNoDataAreas() {
		return noDataAreas;
	}

	/**
	 * @return the noDataGauls
	 */
	public final double[][] getNoDataGauls() {
		return noDataGauls;
	}

	// public static Map<String, Object> ruleA(final FileType type,final File
	// gdalinfoTemplate,
	// final File runnungContextDir, final File imageFile,
	// final File[][] areas, final File[][] gauls,
	// final Double nodataImage, final Double[][] nodataAreas, final Double[][]
	// nodataGauls) throws Exception {
	// return RuleA.ruleA(type, gdalinfoTemplate, runnungContextDir, imageFile,
	// areas, gauls, nodataImage, nodataAreas, nodataGauls);
	// }

	// public static Map<String, Object> ruleB(
	// final FileType type, final double multiply,
	// final File runnungContextDir, final File imageFile,
	// final RenderedImage[][] areas, final RenderedImage[][] gauls,
	// final double nodataImage, final double[][] nodataAreas, final double[][]
	// nodataGauls)
	// throws Exception {
	// return RuleB.ruleB(type, multiply, runnungContextDir, imageFile, areas,
	// gauls, nodataImage, nodataAreas, nodataGauls);
	// }

	/**
	 * <td>noDataRanges</td><td>Collection&lt;Range></td> <td>Ranges of values
	 * to treat specifically as NODATA</td><td>null (no NODATA values defined)</td>
	 * </tr>
	 * <tr>
	 * <td>noDataClassifiers</td>
	 * <td>Double[]</td>
	 * <td>NoData specific for image classifiers. The order of the noData
	 * elements of the array shall respect the order of the elements within the
	 * image classifiers array. NoData are specified as Double although they
	 * refer to classifiers images which are of integer types. Using a Double
	 * allows to specifiy NaN in case some specific noData entries aren't
	 * unavailable for some classifier images. As an instance [-9999,
	 * Double.NaN, -32768, 0, Double.NaN] in case there isn't any noData for
	 * classifierImage 1 and 4 (starting from index 0).</td>
	 * <td>null (no NODATA values defined)</td>
	 * 
	 * @param image
	 * @return
	 * @throws IOException
	 */
	public static Number getNoData(final File image) throws IOException {

		ImageReader reader = new TIFFImageReaderSpi().createReaderInstance();
		FileImageInputStream fis = null;
		RenderedOp o = null;
		try {
			fis = new FileImageInputStream(image);
			reader.setInput(fis);

			ImageReadParam rp = new ImageReadParam();
			rp.setSourceRegion(new Rectangle(0, 0, 1, 1));

			// ImageLayout il=new ImageLayout();
			// RenderingHints r=new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);
			// il.setTileWidth(1).setTileHeight(1);

			o = ImageReadDescriptor.create(fis, 0, false, false, false, null,
					null, rp, reader, null);

			return getNoData(o);

		} catch (IOException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} finally {

			if (o != null) {
				try {
					o.dispose();
				} catch (Exception e) {
				}
			}
			if (reader != null) {
				try {
					reader.dispose();
				} catch (Exception e) {
				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}

		}
		return null;
	}

	/**
	 * return the value of the pixel 0,0 as Number
	 * 
	 * @param o
	 * @return
	 */
	private static Number getNoData(RenderedOp o) {
		final Raster raster = o.getTile(0, 0);
		// Double[] pixel = new Double[0];
		// Object[] pixel = new Object[0];
		final SampleModel sm = raster.getSampleModel();
		final int dt = sm.getDataType();

		switch (dt) {
		case DataBuffer.TYPE_BYTE: {
			byte[] pixel = new byte[1];
			raster.getDataElements(0, 0, pixel);
			return pixel[0];
		}
		case DataBuffer.TYPE_USHORT: {
			short[] pixel = new short[1];
			raster.getDataElements(0, 0, pixel);
			return pixel[0];
		}
		case DataBuffer.TYPE_INT: {
			int[] pixel = new int[1];
			raster.getDataElements(0, 0, pixel);
			return pixel[0];
		}
		case DataBuffer.TYPE_SHORT: {
			short[] pixel = new short[1];
			raster.getDataElements(0, 0, pixel);
			return pixel[0];
		}
		case DataBuffer.TYPE_FLOAT: {
			float[] pixel = new float[1];
			raster.getDataElements(0, 0, pixel);
			return pixel[0];
		}
		case DataBuffer.TYPE_DOUBLE: {
			double[] pixel = new double[1];
			raster.getDataElements(0, 0, pixel);
			return pixel[0];
		}
		}
		return null;
	}

	/**
	 * 
	 * @param map
	 * @param summaryOutputFile
	 * @param i
	 * @return
	 * @throws IOException
	 * @deprecated never used
	 */
	public static Map<String, Object> summarySum(final Map<String, Object> map,
			final File summaryOutputFile, final int i) throws IOException {
		// GET
		FileInputStream fis = null;
		final Properties summaryOutputProp = new Properties();
		try {
			fis = new FileInputStream(summaryOutputFile);
			summaryOutputProp.load(fis);
			// ---------------------------------------------------------------------------------------------
			Object min = map.get("min_value_" + i);
			Object max = map.get("max_value_" + i);

			// TODO map.put("no_data_value",summaryOutputProp.get(""));

			if (min == null) {
				map.put("min_value_" + i, Double.parseDouble(summaryOutputProp
						.getProperty("min")));
			} else {
				map.put("max_value_" + i,
						(Double) min
								+ Double.parseDouble(summaryOutputProp
										.getProperty("min")));
			}
			if (max == null) {
				map.put("max_value_" + i, Double.parseDouble(summaryOutputProp
						.getProperty("max")));
			} else {
				map.put("max_value_" + i,
						(Double) max
								+ Double.parseDouble(summaryOutputProp
										.getProperty("max")));
			}

			// ---------------------------------------------------------------------------------------------
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (fis != null)
				IOUtils.closeQuietly(fis);
		}

		return map;
	}

}
