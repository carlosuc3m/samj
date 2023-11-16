package io.bioimage.samj;

import java.lang.AutoCloseable;
import java.io.IOException;

import org.apposed.appose.Environment;
import org.apposed.appose.Service;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class SamJ implements AutoCloseable {
		
	private final Environment env;
	
	private final Service python;
	
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
	}
	
	
	public static SamJ initializeSam(String envPath) throws IOException {
		return new SamJ(envPath);
	}
	
	public <T extends RealType<T> & NativeType<T>> 
	RandomAccessibleInterval<T> processBox(RandomAccessibleInterval<T> boundingBox){
		String code = sendImgLib2AsNp(boundingBox);
		return null;
	}


	@Override
	public void close() {
		python.close();
	}
	
	private static <T extends RealType<T> & NativeType<T>> 
	String sendImgLib2AsNp(RandomAccessibleInterval<T> boundingBox) {
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		shmInstancesCode += ogName + APPOSE_SHM_KEY + " = shared_memory.SharedMemory(name='" 
							+ shma.getMemoryLocationPythonName() + "', size=" + shma.getSize() + ")" + System.lineSeparator();
		shmInstancesCode += "shm_in_list.append(" + ogName + APPOSE_SHM_KEY + ")" + System.lineSeparator();
		int size = 1;
		long[] dims = rai.dimensionsAsLongArray();
		for (long l : dims) {size *= l;}
		tensorRecreationCode += ogName + " = np.ndarray(" + size + ", dtype='" 
				+ CommonUtils.getDataType(rai) + "', buffer=" 
				+ ogName + APPOSE_SHM_KEY + ".buf).reshape([";
		for (long ll : dims)
			tensorRecreationCode += ll + ", ";
		tensorRecreationCode = 
				tensorRecreationCode.substring(0, tensorRecreationCode.length() - 2);
		tensorRecreationCode += "])" + System.lineSeparator();
	}
}
