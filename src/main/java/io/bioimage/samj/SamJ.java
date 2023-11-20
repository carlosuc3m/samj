package io.bioimage.samj;

import java.lang.AutoCloseable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.Polygon;
import java.io.IOException;

import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import io.bioimage.modelrunner.utils.CommonUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;

public class SamJ implements AutoCloseable {
		
	private final Environment env;
	
	private final Service python;
	
	private String script = "";
	
	private SharedMemoryArray shma;
	
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "task.update('import sam')" + System.lineSeparator()
			+ "from segment_anything import SamPredictor, sam_model_registry" + System.lineSeparator()
			+ "task.update('imported')" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "sam = sam_model_registry[\"" + InstallSAM.SAM_MODEL_TYPE 
			+ "\"](checkpoint='" + InstallSAM.getWeightsFName() + "')" + System.lineSeparator()
			+ "predictor = SamPredictor(sam)" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator();
	
	private SamJ(String envPath) throws IOException, RuntimeException, InterruptedException {
		this.env = new Environment() {
			@Override public String base() { return envPath; }
			@Override public boolean useSystemPath() { return false; }
			};
		python = env.python();
    	python.debug(line -> System.err.println(line));
    	Task task = python.task(IMPORTS + PythonMethods.TRACE_EDGES);
		task.waitFor();
		if (task.status == TaskStatus.CANCELED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.FAILED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.CRASHED)
			throw new RuntimeException();
	}
	
	
	public static <T extends RealType<T> & NativeType<T>> SamJ 
	initializeSam(String envPath, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		SamJ sam = new SamJ(envPath);
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
				+ "task.update('starting encoding')" + System.lineSeparator()
				+ "predictor.set_image(box)";
		try {
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
	
	public Polygon processBox(int[] boundingBox) 
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processWithSAM();
		Map<String, Object> results = null;
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_box", boundingBox);
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
		int[] contours_x = 
				((List<Number>) results.get("contours_x")).stream().mapToInt(j -> j.intValue()).toArray();
		int[] contours_y = 
				((List<Number>) results.get("contours_y")).stream().mapToInt(j -> j.intValue()).toArray();
		return new Polygon(contours_x, contours_y, contours_x.length);
	}


	@Override
	public void close() {
		python.close();
	}
	
	private <T extends RealType<T> & NativeType<T>> 
	void sendImgLib2AsNp(RandomAccessibleInterval<T> boundingBox) {
		shma = SharedMemoryArray.buildSHMA(boundingBox);
		String code = "";
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		code += "box_shm = shared_memory.SharedMemory(name='" 
							+ shma.getMemoryLocationPythonName() + "', size=" + shma.getSize() 
							+ ")" + System.lineSeparator();
		int size = 1;
		long[] dims = boundingBox.dimensionsAsLongArray();
		for (long l : dims) {size *= l;}
		code += "box = np.ndarray(" + size + ", dtype='" 
				+ CommonUtils.getDataType(boundingBox) + "', buffer=box_shm.buf).reshape([";
		for (long ll : dims)
			code += ll + ", ";
		code = code.substring(0, code.length() - 2);
		code += "])" + System.lineSeparator();
		code += "box_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	private void processWithSAM() {
		
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array(input_box)" + System.lineSeparator()
				+ "mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=None," + System.lineSeparator()
				+ "    point_labels=None," + System.lineSeparator()
				+ "    box=input_box[None, :]," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ ")" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask[0].shape))" + System.lineSeparator()
				+ "non_zero = np.where(mask[0] != 0)" + System.lineSeparator()
				+ "task.update(str(non_zero[1][0]))" + System.lineSeparator()
				+ "task.update(str(non_zero[0][0]))" + System.lineSeparator()
				+ "contours, _ = trace_edge(mask[0], non_zero[1][0], non_zero[0][0])" + System.lineSeparator()
				+ "task.update(contours)" + System.lineSeparator()
				+ "contours = np.array(contours)" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours[:, 0].tolist()" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours[:, 1].tolist()" + System.lineSeparator();
		this.script = code;
	}
	
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		String envPath = "/home/carlos/micromamba/envs/sam";
		try (SamJ sam = initializeSam(envPath, img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}
}
