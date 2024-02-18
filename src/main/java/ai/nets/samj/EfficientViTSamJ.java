package ai.nets.samj;

import java.lang.AutoCloseable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;

import io.bioimage.modelrunner.apposed.appose.Environment;
import io.bioimage.modelrunner.apposed.appose.Service;
import io.bioimage.modelrunner.apposed.appose.Service.Task;
import io.bioimage.modelrunner.apposed.appose.Service.TaskStatus;

import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class EfficientViTSamJ extends AbstractSamJ implements AutoCloseable {

	private final Environment env;
	
	private final Service python;
	
	private String script = "";
	
	private SharedMemoryArray shma;
	
	private long[] targetDims;
	
	private SamEnvManager manager;
	
	private static final HashMap<String, String> MODELS_DICT = new HashMap<String, String>();
	static {
		MODELS_DICT.put("l0", "efficientvit_sam_l0");
		MODELS_DICT.put("l1", "efficientvit_sam_l1");
		MODELS_DICT.put("l2", "efficientvit_sam_l2");
		MODELS_DICT.put("xl0", "efficientvit_sam_xl0");
		MODELS_DICT.put("xl1", "efficientvit_sam_xl1");
	}
	
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "from skimage import measure" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "import sys" + System.lineSeparator()
			+ "import os" + System.lineSeparator()
			+ "os.chdir('%s')" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "task.update('import sam')" + System.lineSeparator()
			+ "from efficientvit.models.efficientvit import EfficientViTSam, %s" + System.lineSeparator()
			+ "from efficientvit.models.efficientvit.sam import EfficientViTSamPredictor" + System.lineSeparator()
			+ "task.update('imported')" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "model = %s().cpu().eval()" + System.lineSeparator()
			//+ "from efficientvit.models.nn.norm import set_norm_eps" + System.lineSeparator()
			//+ "set_norm_eps(model, 1e-6)" + System.lineSeparator()
			//+ "from efficientvit.models.utils import load_state_dict_from_file" + System.lineSeparator()
			//+ "weight = load_state_dict_from_file('%s')" + System.lineSeparator()
			//+ "model.load_state_dict(weight)" + System.lineSeparator()
			+ "eps = 1e-6" + System.lineSeparator()
			+ "for m in model.modules():" + System.lineSeparator()
			+ "  if isinstance(m, (torch.nn.GroupNorm, torch.nn.LayerNorm, torch.nn.modules.batchnorm._BatchNorm)):" + System.lineSeparator()
			+ "    if eps is not None:" + System.lineSeparator()
			+ "      m.eps = eps" + System.lineSeparator()
			+ "f_name = os.path.realpath(os.path.expanduser('%s'))" + System.lineSeparator()
			+ "weight = torch.load(f_name, map_location='cpu')" + System.lineSeparator()
			+ "if \"state_dict\" in weight:" + System.lineSeparator()
			+ "  weight = weight[\"state_dict\"]" + System.lineSeparator()
			+ "model.load_state_dict(weight)" + System.lineSeparator()
			+ "predictor = EfficientViTSamPredictor(model)" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['measure'] = measure" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['torch'] = torch" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator();
	private String IMPORTS_FORMATED;

	private EfficientViTSamJ(SamEnvManager manager, String type) throws IOException, RuntimeException, InterruptedException {
		this(manager, type, (t) -> {}, false);
	}

	private EfficientViTSamJ(SamEnvManager manager, String type,
	                      final DebugTextPrinter debugPrinter,
	                      final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {

		if (!MODELS_DICT.keySet().contains(type))
			throw new IllegalArgumentException("The model type should be one of hte following: " 
							+ MODELS_DICT.keySet().stream().collect(Collectors.toList()));
		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = new Environment() {
			@Override public String base() { return manager.getEfficientViTSamEnv(); }
			@Override public boolean useSystemPath() { return false; }
			};
		python = env.python();
		python.debug(debugPrinter::printText);
		IMPORTS_FORMATED = String.format(IMPORTS,
									manager.getEfficientViTSamEnv() + File.separator + SamEnvManager.EVITSAM_NAME,
									MODELS_DICT.get(type), MODELS_DICT.get(type), manager.getEfficientViTSAMWeightsPath(type));
		
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

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(String modelType, SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		EfficientViTSamJ sam = null;
		try{
			sam = new EfficientViTSamJ(manager, modelType, debugPrinter, printPythonCode);
			sam.addImage(image);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(String modelType, SamEnvManager manager, RandomAccessibleInterval<T> image) 
				throws IOException, RuntimeException, InterruptedException {
		EfficientViTSamJ sam = null;
		try{
			sam = new EfficientViTSamJ(manager, modelType);
			sam.addImage(image);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(SamEnvManager.DEFAULT_EVITSAM, manager, image, debugPrinter, printPythonCode);
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(SamEnvManager manager, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(SamEnvManager.DEFAULT_EVITSAM, manager, image);
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
				+ "predictor.set_image(im)";
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
			this.shma.close();
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
		processPointsWithSAM(pointsList.size(), 0);
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_points", pointsList);
		printScript(script, "Points inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		debugPrinter.printText("processPoints() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	public List<Polygon> processPoints(List<int[]> pointsList, List<int[]> pointsNegList)
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processPointsWithSAM(pointsList.size(), pointsNegList.size());
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_points", pointsList);
		inputs.put("input_neg_points", pointsNegList);
		printScript(script, "Points and negative points inference");
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
		if (python != null) python.close();
	}
	
	private <T extends RealType<T> & NativeType<T>> 
	void sendImgLib2AsNp(RandomAccessibleInterval<T> targetImg) {
		shma = createEfficientSAMInputSHM(targetImg);
		adaptImageToModel(targetImg, shma.getSharedRAI());
		String code = "";
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		code += "im_shm = shared_memory.SharedMemory(name='"
							+ shma.getNameForPython() + "', size=" + shma.getSize() 
							+ ")" + System.lineSeparator();
		int size = 1;
		for (long l : targetDims) {size *= l;}
		code += "im = np.ndarray(" + size + ", dtype='uint8', buffer=im_shm.buf).reshape([";
		for (long ll : targetDims)
			code += ll + ", ";
		code = code.substring(0, code.length() - 2);
		code += "])" + System.lineSeparator();
		code += "np.save('/home/carlos/git/aa.npy', im)" + System.lineSeparator();
		code += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	public <T extends RealType<T> & NativeType<T>>
	List<Polygon> processMask(RandomAccessibleInterval<T> img) throws IOException, RuntimeException, InterruptedException {
		long[] dims = img.dimensionsAsLongArray();
		if (dims.length != 2 && dims[0] != this.shma.getOriginalShape()[1] && dims[1] != this.shma.getOriginalShape()[0]) {
			throw new IllegalArgumentException("The provided mask should be a 2d image with just one channel of width "
					+ this.shma.getOriginalShape()[1] + " and height " + this.shma.getOriginalShape()[0]);
		}
		SharedMemoryArray maskShma = SharedMemoryArray.buildNumpyLikeSHMA(img);
		try {
			return processMask(maskShma);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			maskShma.close();
			throw ex;
		}
	}
	
	public List<Polygon> processMask(SharedMemoryArray shmArr) throws IOException, RuntimeException, InterruptedException {
		this.script = "";
		processMasksWithSam(shmArr);
		printScript(script, "Pre-computed mask inference");
		List<Polygon> polys = processAndRetrieveContours(null);
		debugPrinter.printText("processMask() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	private void processMasksWithSam(SharedMemoryArray shmArr) {
		String code = "";
		code += "shm_mask = shared_memory.SharedMemory(name='" + shmArr.getNameForPython() + "')" + System.lineSeparator();
		code += "mask = np.frombuffer(buffer=shm_mask.buf, dtype='" + shmArr.getOriginalDataType() + "').reshape([";
		for (long l : shmArr.getOriginalShape()) 
			code += l + ",";
		code += "])" + System.lineSeparator();
		code += "different_mask_vals = np.unique(mask)" + System.lineSeparator();
		code += "contours_x = []" + System.lineSeparator();
		code += "contours_y = []" + System.lineSeparator();
		code += "for val in different_mask_vals:" + System.lineSeparator()
			  + "  if val < 1:" + System.lineSeparator()
			  + "    continue" + System.lineSeparator()
			  + "  locations = np.where(mask == val)" + System.lineSeparator()
			  + "  input_points_pos = np.zeros((locations[0].shape[0], 2))" + System.lineSeparator()
			  + "  input_labels_pos = np.ones((locations[0].shape[0]))" + System.lineSeparator()
			  + "  locations_neg = np.where((mask != val) & (mask != 0))" + System.lineSeparator()
			  + "  input_points_neg = np.zeros((locations_neg[0].shape[0], 2))" + System.lineSeparator()
			  + "  input_labels_neg = np.zeros((locations_neg[0].shape[0]))" + System.lineSeparator()
			  + "  input_points_pos[:, 0] = locations[1]" + System.lineSeparator()
			  + "  input_points_pos[:, 1] = locations[0]" + System.lineSeparator()
			  + "  input_points_neg[:, 0] = locations_neg[1]" + System.lineSeparator()
			  + "  input_points_neg[:, 1] = locations_neg[0]" + System.lineSeparator()
			  + "  mask_val, _, _ = predictor.predict(" + System.lineSeparator()
			  + "    point_coords=input_points," + System.lineSeparator()
			  + "    point_labels=input_label," + System.lineSeparator()
			  + "    multimask_output=False," + System.lineSeparator()
			  + "    box=None,)" + System.lineSeparator()
			  //+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
			  + "  contours_x_val,contours_y_val = get_polygons_from_binary_mask(mask_val[0])" + System.lineSeparator()
			  + "  contours_x.append(contours_x_val)" + System.lineSeparator()
			  + "  contours_y.append(contours_y_val)" + System.lineSeparator()
			  + "task.update('all contours traced')" + System.lineSeparator()
			  + "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
			  + "task.outputs['contours_y'] = contours_y" + System.lineSeparator();;
		code += "mask = np.frombuffer(buffer=shm_mask.buf, dtype='" + shmArr.getOriginalDataType() + "')" + System.lineSeparator();
		this.script = code;
	}
	
	private void processPointsWithSAM(int nPoints, int nNegPoints) {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_points_list = []" + System.lineSeparator()
				+ "input_neg_points_list = []" + System.lineSeparator();
		for (int n = 0; n < nPoints; n ++)
			code += "input_points_list.append([input_points[" + n + "][0], input_points[" + n + "][1]])" + System.lineSeparator();
		for (int n = 0; n < nNegPoints; n ++)
			code += "input_neg_points_list.append([input_neg_points[" + n + "][0], input_neg_points[" + n + "][1]])" + System.lineSeparator();
		code += ""
				+ "input_points = np.concatenate("
						+ "(np.array(input_points_list).reshape(" + nPoints + ", 2), np.array(input_neg_points_list).reshape(" + nNegPoints + ", 2))"
						+ ", axis=0)" + System.lineSeparator()
				+ "input_label = np.array([1] * " + (nPoints + nNegPoints) + ")" + System.lineSeparator()
				+ "input_label[" + nPoints + ":] -= 1" + System.lineSeparator()
				+ "print(input_points)" + System.lineSeparator()
				+ "print(input_label)" + System.lineSeparator()
				+ "np.save('/home/carlos/git/feats.npy', predictor.features.detach().numpy())" + System.lineSeparator()
				+ "mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=input_points," + System.lineSeparator()
				+ "    point_labels=input_label," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ "    box=None,)" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				+ "np.save('/home/carlos/git/mask.npy', mask)" + System.lineSeparator()
				//+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask[0])" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private void processBoxWithSAM() {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				+ "mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=None," + System.lineSeparator()
				+ "    point_labels=None," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ "    box=input_box,)" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				+ "np.save('/home/carlos/git/mask.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask[0])" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	SharedMemoryArray  createEfficientSAMInputSHM(final RandomAccessibleInterval<T> inImg) {
		long[] dims = inImg.dimensionsAsLongArray();
		if ((dims.length != 3 && dims.length != 2) || (dims.length == 3 && dims[2] != 3 && dims[2] != 1)) {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, float32, ...) 2D images."
					+ "The image dimensions order should be 'xyc', first dimension height, second width and third channels.");
		}
		return SharedMemoryArray.buildMemorySegmentForImage(new long[] {dims[0], dims[1], 3}, new UnsignedByteType());
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(final RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<UnsignedByteType> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) 
				RealTypeConverters.copyFromTo( convertViewToRGB(Views.hyperSlice(ogImg, 2, i)), Views.hyperSlice(targetImg, 2, i) );
		} else if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 1) {
			debugPrinter.printText("CONVERTED 1 CHANNEL IMAGE INTO 3 TO BE FEEDED TO SAMJ");
			IntervalView<UnsignedByteType> resIm = Views.interval( Views.expandMirrorDouble(convertViewToRGB(ogImg), new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, ogImg.dimensionsAsLongArray()[0], ogImg.dimensionsAsLongArray()[1], 2}) );
			RealTypeConverters.copyFromTo( resIm, targetImg );
		} else if (ogImg.numDimensions() == 2) {
			adaptImageToModel(Views.addDimension(ogImg, 0, 0), targetImg);
		} else {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
		this.targetDims = targetImg.dimensionsAsLongArray();
	}
	
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		img = Views.addDimension(img, 1, 2);
		try (EfficientViTSamJ sam = initializeSam(SamEnvManager.create(), img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}
	
	public static List<String> getListOfSupportedEfficientViTSAM(){
		return MODELS_DICT.keySet().stream().collect(Collectors.toList());
	}
}
