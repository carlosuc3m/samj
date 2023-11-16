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
			+ "from segment_anything import SamPredictor, sam_model_registry" + System.lineSeparator();
	
	private SamJ(String envPath) throws IOException {
		this.env = new Environment() {
			@Override public String base() { return envPath; }
			@Override public boolean useSystemPath() { return false; }
			};
		python = env.python();
    	python.debug(line -> System.err.println(line));
    	python.task(IMPORTS);
	}
	
	
	public static SamJ initializeSam(String envPath) throws IOException {
		return new SamJ(envPath);
	}
	
	public <T extends RealType<T> & NativeType<T>> 
	RandomAccessibleInterval<T> processBox(RandomAccessibleInterval<T> boundingBox) 
			throws IOException, RuntimeException, InterruptedException{
		sendImgLib2AsNp(boundingBox);
		processWithSAM((int) boundingBox.dimensionsAsLongArray()[2]);
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
		this.script += code;
	}
	
	private void processWithSAM(int nChannels) {
		String code = "";
		if (nChannels == 1) {
			code += "box = processWithSAM()" + System.lineSeparator();
		} else {
			code += "box[:, :, 0] = processWithSAM()" + System.lineSeparator();
			code += "box = box[:, :, 0] * np.ones((1, 1, box.shape[2]))" + System.lineSeparator();
		}
		code += "box_shm.unlink()" + System.lineSeparator();
		code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
}
