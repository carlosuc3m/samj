package ai.nets.samj.communication.model;

import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Cast;

import java.awt.Polygon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.AbstractSamJ;
import ai.nets.samj.EfficientViTSamJ;
import ai.nets.samj.SamEnvManager;
import ai.nets.samj.ui.SAMJLogger;

public class EfficientViTSAML2 implements SAMModel {

	private static final Polygon EMPTY_POLYGON = new Polygon(new int[0], new int[0], 0);

	private EfficientViTSamJ efficientSamJ;
	private SAMJLogger log;
	private Boolean installed = false;
	public static final String FULL_NAME = "EfficientViTSAM-l2";
	public static final String INPUT_IMAGE_AXES = "xyc";
	
	public EfficientViTSAML2() {
		
	}

	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public String getDescription() {
		return "Bla bla Efficient SAM";
	}

	@Override
	public boolean isInstalled() {
		return installed;
	}

	@Override
	public SAMModel instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) {
		try {
			return new EfficientViTSAML2(image,useThisLoggerForIt);
		} catch (IOException | InterruptedException | RuntimeException e) {
			useThisLoggerForIt.error(FULL_NAME + " experienced an error: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}

	public EfficientViTSAML2(final RandomAccessibleInterval<?> image,
	                              final SAMJLogger log)
	throws IOException, RuntimeException, InterruptedException {
		this.log = log;
		AbstractSamJ.DebugTextPrinter filteringLogger = text -> {
			int idx = text.indexOf("contours_x");
			if (idx > 0) this.log.info( text.substring(0,idx) );
			else this.log.info( text );
		};
		efficientSamJ = EfficientViTSamJ.initializeSam("l2",
				SamEnvManager.create(), Cast.unchecked(image),
				filteringLogger, false);
	}

	@Override
	public List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D) {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return efficientSamJ.processPoints(list);
			else return efficientSamJ.processPoints(list, negList);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		log.info(FULL_NAME+": NOT SUPPORTED YET");
		List<Polygon> retList = new ArrayList<>(1);
		retList.add( EMPTY_POLYGON );
		return retList;
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Interval boundingBox2D) {
		try {
			//order to processBox() should be: x0,y0, x1,y1
			final int bbox[] = {
				(int)boundingBox2D.min(0),
				(int)boundingBox2D.min(1),
				(int)boundingBox2D.max(0),
				(int)boundingBox2D.max(1)
			};
			return efficientSamJ.processBox(bbox);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public void notifyUiHasBeenClosed() {
		log.info(FULL_NAME+": OKAY, I'm closing myself...");
		closeProcess();
	}

	@Override
	public void closeProcess() {
		efficientSamJ.close();
	}

	@Override
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}