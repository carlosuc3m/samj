package io.bioimage.samj;

import java.lang.AutoCloseable;
import java.io.IOException;

import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import io.bioimage.modelrunner.utils.CommonUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class SamJ implements AutoCloseable {
		
	private final Environment env;
	
	private final Service python;
	
	private String script = "";
	
	private SharedMemoryArray shma;
	
	public static final String IMPORTS = ""
			+ "import numpy as np" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "from segment_anything import SamPredictor, sam_model_registry" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "sam = sam_model_registry[\"" + InstallSAM.SAM_MODEL_TYPE 
			+ "\"](checkpoint='" + InstallSAM.getWeightsFName() + "')" + System.lineSeparator()
			+ "predictor = SamPredictor(sam)" + System.lineSeparator();
	
	private static final String  FIND_CONTOURS = ""
			+"def find_contours(mask):" + System.lineSeparator()
			+ "    # Ensure mask is binary" + System.lineSeparator()
			+ "    mask = np.where(mask > 0, 1, 0)" + System.lineSeparator()
			+ "    # Find edges: shift the mask in each direction and find where it differs from the original" + System.lineSeparator()
			+ "    edges = np.zeros_like(mask)" + System.lineSeparator()
			+ "    edges[:-1, :] |= (mask[1:, :] != mask[:-1, :])  # vertical edges" + System.lineSeparator()
			+ "    edges[:, :-1] |= (mask[:, 1:] != mask[:, :-1])  # horizontal edges" + System.lineSeparator()
			+ "    return edges" + System.lineSeparator();
	
	private SamJ(String envPath) throws IOException {
		this.env = new Environment() {
			@Override public String base() { return envPath; }
			@Override public boolean useSystemPath() { return false; }
			};
		python = env.python();
    	python.debug(line -> System.err.println(line));
    	python.task(IMPORTS + FIND_CONTOURS);
	}
	
	
	public static <T extends RealType<T> & NativeType<T>> SamJ 
	initializeSam(String envPath, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		SamJ sam = new SamJ(envPath);
		sam.addImage(image);
		return sam;
	}
	
	public <T extends RealType<T> & NativeType<T>> 
	RandomAccessibleInterval<T> addImage(RandomAccessibleInterval<T> rai) 
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		sendImgLib2AsNp(rai);
		this.script = "predictor.set_image(box)";
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
		
		return shma.getSharedRAI();
	}
	
	public <T extends RealType<T> & NativeType<T>> 
	RandomAccessibleInterval<T> processBox(int[] boundingBox) 
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processWithSAM();
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
		
		return shma.getSharedRAI();
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
		code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	private void processWithSAM() {
		
		String code = "" + System.lineSeparator()
				+ "input_box = np.array(input_box)" + System.lineSeparator()
				+ "masks, _, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=None," + System.lineSeparator()
				+ "    point_labels=None," + System.lineSeparator()
				+ "    box=input_box[None, :]," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ ")" + System.lineSeparator();
		this.script = code;
	}
}
