package ai.nets.samj.communication;

import ai.nets.samj.AbstractSamJ;
import ai.nets.samj.EfficientSamJ;
import ai.nets.samj.SamEnvManager;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;

import java.awt.Polygon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.ui.SAMJLogger;

public class PromptsToEfficientSamJ implements PromptsToNetAdapter {

	private static final Polygon EMPTY_POLYGON = new Polygon(new int[0], new int[0], 0);
	private static final String LONG_NAME = "EfficientSamJ";

	private final EfficientSamJ efficientSamJ;
	private final SAMJLogger log;

	public PromptsToEfficientSamJ(final RandomAccessibleInterval<?> image,
	                              final SAMJLogger log)
	throws IOException, RuntimeException, InterruptedException {
		this.log = log;
		AbstractSamJ.DebugTextPrinter filteringLogger = text -> {
			int idx = text.indexOf("contours_x");
			if (idx > 0) this.log.info( text.substring(0,idx) );
			else this.log.info( text );
		};
		efficientSamJ = EfficientSamJ.initializeSam(
				SamEnvManager.create(), (RandomAccessibleInterval)image,
				filteringLogger, false);
	}

	@Override
	public String getNetName() {
		return "E.SAM";
	}

	@Override
	public List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D) {
		try {
			return efficientSamJ.processPoints(listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]})
					.collect(Collectors.toList()));
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(LONG_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public List<Polygon> fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D) {
		log.info(LONG_NAME+": NOT SUPPORTED YET");
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
			log.error(LONG_NAME+", providing empty result because of some trouble: "+e.getMessage());
			e.printStackTrace();
			List<Polygon> retList = new ArrayList<>(1);
			retList.add( EMPTY_POLYGON );
			return retList;
		}
	}

	@Override
	public void notifyUiHasBeenClosed() {
		log.info(LONG_NAME+": OKAY, I'm closing myself...");
		efficientSamJ.close();
	}
}
