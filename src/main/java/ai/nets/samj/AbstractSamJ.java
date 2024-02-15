package ai.nets.samj;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.time.LocalDateTime;

public class AbstractSamJ {

	/** Essentially, a syntactic-shortcut for Consumer<String> */
	public interface DebugTextPrinter { void printText(String text); }

	protected DebugTextPrinter debugPrinter = System.out::println;
	public void disableDebugPrinting() {
		debugPrinter = (text) -> {};
	}
	public void setDebugPrinter(final DebugTextPrinter lineComsumer) {
		if (lineComsumer != null) debugPrinter = lineComsumer;
	}

	protected boolean isDebugging = true;
	public void setDebugging(boolean newState) {
		isDebugging = newState;
	}
	public boolean isDebugging() {
		return isDebugging;
	}

	public void printScript(final String script, final String designationOfTheScript) {
		if (!isDebugging) return;
		debugPrinter.printText("START: =========== "+designationOfTheScript+" ===========");
		debugPrinter.printText(LocalDateTime.now().toString());
		debugPrinter.printText(script);
		debugPrinter.printText("END:   =========== "+designationOfTheScript+" ===========");
	}

	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<T> adaptImageToModel(final RandomAccessibleInterval<T> inImg) {
		return inImg;
	}

	public static <T extends RealType<T> & NativeType<T>>
	void getMinMaxPixelValue(final IterableInterval<T> inImg, final double[] outMinMax) {
		double min = inImg.firstElement().getRealDouble();
		double max = min;

		for (T px : inImg) {
			double val = px.getRealDouble();
			min = Math.min(min,val);
			max = Math.max(max,val);
		}

		if (outMinMax.length > 1) {
			outMinMax[0] = min;
			outMinMax[1] = max;
		}
	}

	public static boolean isNormalizedInterval(final double[] inMinMax) {
		return (inMinMax[0] >= 0 && inMinMax[0] <= 1
			&& inMinMax[1] >= 0 && inMinMax[1] <= 1);
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType> normalizedView(final RandomAccessibleInterval<T> inImg, final double[] inMinMax) {
		final double min = inMinMax[0];
		final double range = inMinMax[1] - min;
		return Converters.convert(inImg, (i, o) -> o.setReal((i.getRealFloat() - min) / (range + 1e-9)), new FloatType());
	}

	/**
	 * Checks the input RAI if its min and max pixel values are between [0,1].
	 * If they are not, the RAI will be subject to {@link Converters#convert(RandomAccessibleInterval, Converter, Type)}
	 * with here-created Converter that knows how to bring the pixel values into the interval [0,1].
	 *
	 * @param inImg RAI to be potentially normalized.
	 * @return The input image itself or a View of it.
	 */
	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType> normalizedView(final RandomAccessibleInterval<T> inImg) {
		final double[] minMax = new double[2];
		getMinMaxPixelValue(Views.iterable(inImg), minMax);
		///debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ NORMALIZED, returning Converted view");
		//return normalizedView(inImg, minMax);
		if (isNormalizedInterval(minMax) && Util.getTypeFromInterval(inImg) instanceof FloatType) {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS NORMALIZED, returning directly itself");
			return Cast.unchecked(inImg);
		} else if (isNormalizedInterval(minMax)) {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS NORMALIZED, returning directly itself");
			return  Converters.convert(inImg, (i, o) -> o.setReal(i.getRealFloat()), new FloatType());
		} else {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ NORMALIZED, returning Converted view");
			return normalizedView(inImg, minMax);
		}
	}

	public static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<UnsignedByteType> convertViewToRGB(final RandomAccessibleInterval<T> inImg, final double[] inMinMax) {
		final double min = inMinMax[0];
		final double range = inMinMax[1] - min;
		return Converters.convert(inImg, (i, o) -> o.setReal(255 * (i.getRealDouble() - min) / range), new UnsignedByteType());
	}

	/**
	 * Checks the input RAI if its min and max pixel values are between [0,1].
	 * If they are not, the RAI will be subject to {@link Converters#convert(RandomAccessibleInterval, Converter, Type)}
	 * with here-created Converter that knows how to bring the pixel values into the interval [0,1].
	 *
	 * @param inImg RAI to be potentially normalized.
	 * @return The input image itself or a View of it.
	 */
	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<UnsignedByteType> convertViewToRGB(final RandomAccessibleInterval<T> inImg) {
		if (Util.getTypeFromInterval(inImg) instanceof UnsignedByteType) {
			debugPrinter.printText("IMAGE IS RGB, returning directly itself");
			return Cast.unchecked(inImg);
		}
		final double[] minMax = new double[2];
		debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ RGB, returning Converted view");
		getMinMaxPixelValue(Views.iterable(inImg), minMax);
		return convertViewToRGB(inImg, minMax);
	}
}