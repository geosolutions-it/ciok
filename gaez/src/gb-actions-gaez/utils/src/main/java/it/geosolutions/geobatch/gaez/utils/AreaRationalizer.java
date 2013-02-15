package it.geosolutions.geobatch.gaez.utils;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.DivideDescriptor;

import org.geotools.image.jai.Registry;
import org.geotools.resources.image.ImageUtilities;
import org.jaitools.media.jai.rangelookup.RangeLookupDescriptor;
import org.jaitools.media.jai.rangelookup.RangeLookupRIF;
import org.jaitools.media.jai.rangelookup.RangeLookupTable;
import org.jaitools.numeric.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.jai.operator.ImageReadDescriptor;
import com.sun.media.jai.operator.ImageWriteDescriptor;

public class AreaRationalizer {

	static {
        try{
            Registry.registerRIF(JAI.getDefaultInstance(), new RangeLookupDescriptor(), new RangeLookupRIF(), Registry.JAI_TOOLS_PRODUCT);
        } catch (Throwable e) {
            // swallow exception in case the op has already been registered.
        }
    }
	
	/**
	 * Default logger
	 */
	protected final static Logger LOGGER = LoggerFactory
			.getLogger(AreaRationalizer.class);

	final ImageReader ir[];
	final FileImageInputStream fis[];
	final RenderedOp riAreas[];
	final RenderedOp riZeroed[];

	RenderedOp riAreaSum;
	ImageReader sir;
	FileImageInputStream sfis;

	ImageOutputStream fos[];
	File areaRatioImages[];

	final int size;
	boolean initted;

	public AreaRationalizer(final File areaRatioImages[],
			final File areaImages[], final double noData[]) throws Exception {
		// TODO check on the size
		size = areaRatioImages.length;

		this.areaRatioImages = areaRatioImages;
		fos = new ImageOutputStream[size];

		ir = new ImageReader[size];
		fis = new FileImageInputStream[size];
		riZeroed = new RenderedOp[size];
		riAreas = new RenderedOp[size];

		try {
			init(areaImages, noData);
		} catch (Exception e) {
			dispose(true);
			throw e;
		}

	}

	public boolean init(final File areaImages[], final double noData[])
			throws Exception {

		TIFFImageReaderSpi spi = new TIFFImageReaderSpi();

		for (int i = 0; i < size; i++) {
			ir[i] = null;
			fis[i] = null;
			riZeroed[i] = null;
			riAreas[i] = null;
			try {
				fis[i] = new FileImageInputStream(areaImages[i]);
				ir[i] = spi.createReaderInstance();
				final ImageReadParam rp = ir[i].getDefaultReadParam();
				riAreas[i] = ImageReadDescriptor.create(fis[i], 0, false,
						false, false, null, null, rp, ir[i], null);// .readAsRenderedImage(0,
																	// new
																	// ImageReadParam());

				RangeLookupTable<Double, Double> rlt = new RangeLookupTable<Double, Double>(
						true);
				rlt.add(new Range<Double>(noData[i]), 0d);// 1d);//Double.NaN);
				riZeroed[i] = RangeLookupDescriptor.create(riAreas[i], rlt,
						null);

				// ImageIOUtilities.visualize(riAreaSum, "", true);
			} catch (IOException e) {
				initted = false;
				throw e;
			}
		}

		riAreaSum = null;
		for (int i = 0; i < size; i++) {
			if (riAreaSum == null)
				riAreaSum = riZeroed[i];
			else
				riAreaSum = AddDescriptor.create(riAreaSum, riZeroed[i], null);
		}

		// RangeLookupTable<Double, Integer> rlt = new RangeLookupTable<Double,
		// Integer>(true);
		// rlt.add(Range.create(0D), 1);
		// riAreaSum = RangeLookupDescriptor.create(riAreaSum, rlt, null);
		RangeLookupTable<Double, Double> rlt = new RangeLookupTable<Double, Double>(
				true);
		rlt.add(Range.create(0D), Double.NaN);
		riAreaSum = RangeLookupDescriptor.create(riAreaSum, rlt, null);

		for (int i = 0; i < size; i++) {
			try {

				riAreas[i] = DivideDescriptor.create(riZeroed[i], riAreaSum,
						null);
				// riAreas[i]=DivideDescriptor.create(riAreas[i], riAreaSum,
				// null);

				// ImageIOUtilities.visualize(riAreas[i], "", false);
			} catch (Exception e) {
				initted = false;
				throw e;
			}
		}

		initted = true;
		return Boolean.TRUE;

	}

	private void cleanCache(final RenderedOp op) {
		if (op != null) {
			try {
				ImageUtilities.disposePlanarImageChain(PlanarImage
						.wrapRenderedImage(op));
			} catch (Exception e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
			}
		}
	}

	public void dispose(final boolean cacheClean) {
		for (int i = 0; i < size; i++) {
			try {
				if (riZeroed != null && riZeroed[i] != null) {
					if (cacheClean)
						cleanCache(riZeroed[i]);
					riZeroed[i].dispose();
				}
			} catch (Exception innerE) {
				// eat
			}
			try {
				if (ir != null && ir[i] != null)
					ir[i].dispose();
			} catch (Exception e) {
			}
			try {
				if (fis != null && fis[i] != null)
					fis[i].close();
			} catch (Exception e) {
			}

			try {
				if (fos != null && fos[i] != null)
					fos[i].close();
			} catch (Exception e) {
			}

		}
		initted = false;
	}

	public static void produce(final File areasRationalized[][],
			final File areas[][], final double areasNoData[][])
			throws Exception {
		int size = areasRationalized.length;
		for (int i = 0; i < size; i++) {
			if (!areasRationalized[i][0].exists()) {
				AreaRationalizer ar = null;
				try {
					ar = new AreaRationalizer(areasRationalized[i], areas[i],
							areasNoData[i]);
					ar.write();
				} catch (Exception e) {
					if (LOGGER.isErrorEnabled())
						LOGGER.error(e.getLocalizedMessage(), e);
					throw e;
				} finally {
					if (ar != null)
						ar.dispose(true);
				}
			}
		}
	}

	public void write() throws Exception {
		if (!initted)
			throw new IllegalStateException(
					"Unable to write object using a not disposed object");

		TIFFImageWriterSpi spi = new TIFFImageWriterSpi();
		for (int i = 0; i < size; i++) {
			ImageWriter writer = null;
			try {

				fos[i] = new FileImageOutputStream(areaRatioImages[i]);

				writer = spi.createWriterInstance();
				writer.setOutput(fos[i]);
				ImageWriteParam par = writer.getDefaultWriteParam();
				par.setTilingMode(ImageWriteParam.MODE_EXPLICIT);

				// par.setTiling(this.riAreas[i].getTileHeight(),
				// this.riAreas[i].getTileHeight(), 0, 0);
				par.setTiling(256, 256, 0, 0);

				IIOImage image = new IIOImage(this.riAreas[i], null, null);
				writer.write(null, image, par);

			} catch (Exception e) {
				if (LOGGER.isErrorEnabled())
					LOGGER.error(e.getLocalizedMessage(), e);
				throw e;
			} finally {
				try {
					if (writer != null)
						writer.dispose();
				} catch (Exception e) {
				}
				try {
					if (fos != null && fos[i] != null)
						fos[i].close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * 
	 * @param image
	 * @param file
	 * @deprecated untested
	 */
	public static void write(RenderedImage image, String file) {
		final File productFile = new File(file);
		FileImageOutputStream fos = null;
		try {
			fos = new FileImageOutputStream(productFile);
		} catch (FileNotFoundException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		} catch (IOException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		}
		TIFFImageWriterSpi spiw = new TIFFImageWriterSpi();
		ImageWriter writer = null;
		try {
			writer = spiw.createWriterInstance();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.setOutput(fos);
		final RenderedOp op = ImageWriteDescriptor.create(image, fos, null,
				false, false, false, false, null, null, null, null, null, null,
				null, writer, null);
		op.getRendering();
		try {
			writer.dispose();
		} catch (Exception e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		}
		try {
			fos.close();
		} catch (IOException e) {
			if (LOGGER.isErrorEnabled())
				LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

}
