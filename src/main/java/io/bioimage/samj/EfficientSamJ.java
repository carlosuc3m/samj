package io.bioimage.samj;

import java.lang.AutoCloseable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;

import io.bioimage.modelrunner.apposed.appose.Environment;
import io.bioimage.modelrunner.apposed.appose.Service;
import io.bioimage.modelrunner.apposed.appose.Service.Task;
import io.bioimage.modelrunner.apposed.appose.Service.TaskStatus;

import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import io.bioimage.modelrunner.utils.CommonUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class EfficientSamJ extends AbstractSamJ implements AutoCloseable {

	private final Environment env;
	
	private final Service python;
	
	private String script = "";
	
	private SharedMemoryArray shma;
	
	private SamEnvManager manager;
	
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "from skimage import measure" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "import sys" + System.lineSeparator()
			+ "sys.path.append('%s')" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "task.update('import sam')" + System.lineSeparator()
			+ "from efficient_sam.efficient_sam import build_efficient_sam" + System.lineSeparator()
			+ "task.update('imported')" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "predictor = build_efficient_sam(encoder_patch_embed_dim=384,encoder_num_heads=6,checkpoint='%s',).eval()" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['measure'] = measure" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['torch'] = torch" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator();
	private String IMPORTS_FORMATED;

	private EfficientSamJ(SamEnvManager manager) throws IOException, RuntimeException, InterruptedException {
		this(manager, (t) -> {}, false);
	}

	private EfficientSamJ(SamEnvManager manager,
	                      final DebugTextPrinter debugPrinter,
	                      final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {

		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = new Environment() {
			@Override public String base() { return manager.getPythonEnv(); }
			@Override public boolean useSystemPath() { return false; }
			};
		python = env.python();
		python.debug(debugPrinter::printText);
		IMPORTS_FORMATED = String.format(IMPORTS,
				manager.getEfficientSamEnv() + File.separator + SamEnvManager.ESAM_NAME,
				manager.getEfficientSAMSmallWeightsPath());
		printScript(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES, "Edges tracing code");
		Task task = python.task(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES);
		task.waitFor();
		if (task.status == TaskStatus.CANCELED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.FAILED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.CRASHED)
			throw new RuntimeException();
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientSamJ
	initializeSam(SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		EfficientSamJ sam = new EfficientSamJ(manager, debugPrinter, printPythonCode);
		sam.addImage(image);
		return sam;
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientSamJ
	initializeSam(SamEnvManager manager, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		EfficientSamJ sam = new EfficientSamJ(manager);
		sam.addImage(image);
		return sam;
	}
	
	public <T extends RealType<T> & NativeType<T>>
	void updateImage(RandomAccessibleInterval<T> rai) throws IOException, RuntimeException, InterruptedException {
		addImage(rai);
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void addImage(RandomAccessibleInterval<T> rai) 
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		sendImgLib2AsNp(rai);
		this.script += ""
				+ "task.update(str(im.shape))" + System.lineSeparator()
				+ "aa = predictor.get_image_embeddings(im[None, ...])";
		try {
			printScript(script, "Creation of initial embeddings");
			Task task = python.task(script);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
		} catch (IOException | InterruptedException | RuntimeException e) {
			try {
				this.shma.close();
			} catch (IOException e1) {
				throw new IOException(e.toString() + System.lineSeparator() + e1.toString());
			}
			throw e;
		}
	}
	
	private List<Polygon> processAndRetrieveContours(HashMap<String, Object> inputs) 
			throws IOException, RuntimeException, InterruptedException {
		Map<String, Object> results = null;
		try {
			Task task = python.task(script, inputs);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
			else if (task.status != TaskStatus.COMPLETE)
				throw new RuntimeException();
			else if (task.outputs.get("contours_x") == null)
				throw new RuntimeException();
			else if (task.outputs.get("contours_y") == null)
				throw new RuntimeException();
			results = task.outputs;
		} catch (IOException | InterruptedException | RuntimeException e) {
			try {
				this.shma.close();
			} catch (IOException e1) {
				throw new IOException(e.toString() + System.lineSeparator() + e1.toString());
			}
			throw e;
		}

		final List<List<Number>> contours_x_container = (List<List<Number>>)results.get("contours_x");
		final Iterator<List<Number>> contours_x = contours_x_container.iterator();
		final Iterator<List<Number>> contours_y = ((List<List<Number>>)results.get("contours_y")).iterator();
		final List<Polygon> polys = new ArrayList<>(contours_x_container.size());
		while (contours_x.hasNext()) {
			int[] xArr = contours_x.next().stream().mapToInt(Number::intValue).toArray();
			int[] yArr = contours_y.next().stream().mapToInt(Number::intValue).toArray();
			polys.add( new Polygon(xArr, yArr, xArr.length) );
		}
		return polys;
	}
	
	public List<Polygon> processPoints(List<int[]> pointsList)
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processPointsWithSAM(pointsList.size());
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_box", pointsList);
		printScript(script, "Rectangle inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		debugPrinter.printText("processPoints() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	public List<Polygon> processBox(int[] boundingBox)
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processBoxWithSAM();
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_box", boundingBox);
		printScript(script, "Rectangle inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		debugPrinter.printText("processBox() obtained " + polys.size() + " polygons");
		return polys;
	}


	@Override
	public void close() {
		python.close();
	}
	
	private <T extends RealType<T> & NativeType<T>> 
	void sendImgLib2AsNp(RandomAccessibleInterval<T> targetImg) {
		shma = SharedMemoryArray.buildSHMA( normalizedView(targetImg) );
		String code = "";
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		code += IMPORTS_FORMATED+"im_shm = shared_memory.SharedMemory(name='"
							+ shma.getNameForPython() + "', size=" + shma.getSize() 
							+ ")" + System.lineSeparator();
		int size = 1;
		long[] dims = targetImg.dimensionsAsLongArray();
		for (long l : dims) {size *= l;}
		code += "im = np.ndarray(" + size + ", dtype='" 
				+ CommonUtils.getDataType(targetImg) + "', buffer=im_shm.buf).reshape([";
		for (long ll : dims)
			code += ll + ", ";
		code = code.substring(0, code.length() - 2);
		code += "])" + System.lineSeparator();
		code += "input_h = im.shape[0]" + System.lineSeparator();
		code += "input_w = im.shape[1]" + System.lineSeparator();
		code += "globals()['input_h'] = input_h" + System.lineSeparator();
		code += "globals()['input_w'] = input_w" + System.lineSeparator();
		code += "im = torch.from_numpy(np.transpose(im.astype('float32'), (2, 0, 1)))" + System.lineSeparator();
		code += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	private void processPointsWithSAM(int nPoints) {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_points_list = []" + System.lineSeparator();
		for (int n = 0; n < nPoints; n ++)
			code += "input_points_list.append([input_points[" + n + "][0], input_points[" + n + "][1]])" + System.lineSeparator();
		code += ""
				+ "input_points = np.array(input_points_list)" + System.lineSeparator()
				+ "input_points = torch.reshape(torch.tensor(input_points), [1, 1, -1, 2])" + System.lineSeparator()
				+ "input_label = np.array([1] * " + nPoints + ")" + System.lineSeparator()
				+ "input_label = torch.reshape(torch.tensor(input_label), [1, 1, -1])" + System.lineSeparator()
				+ "predicted_logits, predicted_iou = predictor.predict_masks(predictor.encoded_images," + System.lineSeparator()
				+ "    input_points," + System.lineSeparator()
				+ "    input_label," + System.lineSeparator()
				+ "    multimask_output=True," + System.lineSeparator()
				+ "    input_h=input_h," + System.lineSeparator()
				+ "    input_w=input_w," + System.lineSeparator()
				+ "    output_h=input_h," + System.lineSeparator()
				+ "    output_w=input_w,)" + System.lineSeparator()
				+ "sorted_ids = torch.argsort(predicted_iou, dim=-1, descending=True)" + System.lineSeparator()
				+ "predicted_iou = torch.take_along_dim(predicted_iou, sorted_ids, dim=2)" + System.lineSeparator()
				+ "predicted_logits = torch.take_along_dim(predicted_logits, sorted_ids[..., None, None], dim=2)" + System.lineSeparator()
				+ "mask = torch.ge(predicted_logits[0, 0, 0, :, :], 0).cpu().detach().numpy()" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				//+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask)" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private void processBoxWithSAM() {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				+ "input_box = torch.reshape(torch.tensor(input_box), [1, 1, -1, 2])" + System.lineSeparator()
				+ "input_label = np.array([2,3])" + System.lineSeparator()
				+ "input_label = torch.reshape(torch.tensor(input_label), [1, 1, -1])" + System.lineSeparator()
				+ "predicted_logits, predicted_iou = predictor.predict_masks(predictor.encoded_images," + System.lineSeparator()
				+ "    input_box," + System.lineSeparator()
				+ "    input_label," + System.lineSeparator()
				+ "    multimask_output=True," + System.lineSeparator()
				+ "    input_h=input_h," + System.lineSeparator()
				+ "    input_w=input_w," + System.lineSeparator()
				+ "    output_h=input_h," + System.lineSeparator()
				+ "    output_w=input_w,)" + System.lineSeparator()
				+ "sorted_ids = torch.argsort(predicted_iou, dim=-1, descending=True)" + System.lineSeparator()
				+ "predicted_iou = torch.take_along_dim(predicted_iou, sorted_ids, dim=2)" + System.lineSeparator()
				+ "predicted_logits = torch.take_along_dim(predicted_logits, sorted_ids[..., None, None], dim=2)" + System.lineSeparator()
				+ "mask = torch.ge(predicted_logits[0, 0, 0, :, :], 0).cpu().detach().numpy()" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				//+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask)" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType>  createEfficientSAMInputimage(final RandomAccessibleInterval<T> inImg) {
		long[] dims = inImg.dimensionsAsLongArray();
		if ((dims.length != 3 && dims.length != 2) || (dims.length == 3 && dims[2] != 3 && dims[2] != 1)){
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
		return ArrayImgs.floats(new long[] {dims[0], dims[1], 3});
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(final RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<FloatType> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) normalizedView(Views.hyperSlice(ogImg, 2, i));
			RealTypeConverters.copyFromTo( ogImg, targetImg );
		} else if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 1) {
			normalizedView(ogImg);
			debugPrinter.printText("CONVERTED 1 CHANNEL IMAGE INTO 3 TO BE FEEDED TO SAMJ");
			IntervalView<T> resIm = Views.interval( Views.expandMirrorDouble(ogImg, new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, ogImg.dimensionsAsLongArray()[0], ogImg.dimensionsAsLongArray()[1], 2}) );
			RealTypeConverters.copyFromTo( resIm, targetImg );
		} else if (ogImg.numDimensions() == 2) {
			adaptImageToModel(Views.addDimension(ogImg, 0, 0), targetImg);
		} else {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
	}
	
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		img = Views.addDimension(img, 1, 2);
		try (EfficientSamJ sam = initializeSam(SamEnvManager.create(), img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}
}
