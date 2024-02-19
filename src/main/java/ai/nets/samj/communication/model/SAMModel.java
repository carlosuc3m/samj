package ai.nets.samj.communication.model;

import java.awt.Polygon;
import java.util.List;

import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A common ground for various placeholder classes to inform
 * the system that even this network may be available/installed.
 * It is, however, not creating/instancing any connection to any
 * (Python) network code or whatsoever, that happens only after the
 * {@link SAMModel#instantiate(Logger)} is called, and reference
 * to such connection is returned.
 */
public interface SAMModel {

	String getName();
	String getInputImageAxes();
	String getDescription();
	boolean isInstalled();
	void setInstalled(boolean installed);

	/** Returns null if it is no installed. */
	SAMModel instantiate(
			final RandomAccessibleInterval<?> image,
			final SAMJLogger useThisLoggerForIt);

	List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D);

	List<Polygon> fetch2dSegmentation(Localizable lineStartPoint2D, Localizable lineEndPoint2D);

	List<Polygon> fetch2dSegmentation(Interval boundingBox2D);

	public <T extends RealType<T> & NativeType<T>> List<Polygon> fetch2dSegmentationFromMask(RandomAccessibleInterval<T> rai);

	void closeProcess();

	void notifyUiHasBeenClosed();
}