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

public class EfficientSamJ implements AutoCloseable {
		
	private final Environment env;
	
	private final Service python;
	
	private String script = "";
	
	private SharedMemoryArray shma;
	
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "task.update('import sam')" + System.lineSeparator()
			+ "from efficient_sam.build_efficient_sam import build_efficient_sam_vits" + System.lineSeparator()
			+ "task.update('imported')" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "predictor = build_efficient_sam_vits()" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['torch'] = torch" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator();
	
	private EfficientSamJ(String envPath) throws IOException, RuntimeException, InterruptedException {
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
	
	
	public static <T extends RealType<T> & NativeType<T>> EfficientSamJ 
	initializeSam(String envPath, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		EfficientSamJ sam = new EfficientSamJ(envPath);
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
				+ "task.update(str(box.shape))" + System.lineSeparator()
				+ "aa = predictor.get_image_embeddings(box[None, ...])";
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
							+ shma.getNameForPython() + "', size=" + shma.getSize() 
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
		code += "input_h = box.shape[0]" + System.lineSeparator();
		code += "input_w = box.shape[1]" + System.lineSeparator();
		code += "globals()['input_h'] = input_h" + System.lineSeparator();
		code += "globals()['input_w'] = input_w" + System.lineSeparator();
		code += "box = torch.from_numpy(np.transpose(box, (2, 0, 1))).float()" + System.lineSeparator();
		code += "box_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	private void processWithSAM() {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				//+ "input_box = np.array([[200, 60], [220, 80]])" + System.lineSeparator()
				+ "input_box = torch.reshape(torch.tensor(input_box), [1, 1, -1, 2])" + System.lineSeparator()
				//+ "input_box = torch.from_numpy(np.array(input_box).reshape(2, 2)).unsqueeze(0).unsqueeze(0)" + System.lineSeparator()
				+ "input_label = np.array([2,3])" + System.lineSeparator()
				+ "input_label = torch.reshape(torch.tensor(input_label), [1, 1, -1])" + System.lineSeparator()
				+ "predicted_logits, predicted_iou = predictor.predict_masks(predictor.encoded_images," + System.lineSeparator()
				+ "    input_box," + System.lineSeparator()
				+ "    input_label," + System.lineSeparator()
				+ "    multimask_output=True," + System.lineSeparator()
				+ "    input_h=163," + System.lineSeparator()
				+ "    input_w=481," + System.lineSeparator()
				+ "    output_h=163," + System.lineSeparator()
				+ "    output_w=481,)" + System.lineSeparator()
				+ "sorted_ids = torch.argsort(predicted_iou, dim=-1, descending=True)" + System.lineSeparator()
				+ "predicted_iou = torch.take_along_dim(predicted_iou, sorted_ids, dim=2)" + System.lineSeparator()
				+ "predicted_logits = torch.take_along_dim(predicted_logits, sorted_ids[..., None, None], dim=2)" + System.lineSeparator()
				+ "mask = torch.ge(predicted_logits[0, 0, 0, :, :], 0).cpu().detach().numpy()" + System.lineSeparator()
				//+ "task.update('end predict')" + System.lineSeparator()
				//+ "task.update(str(mask.shape))" + System.lineSeparator()
				//+ "print(mask.shape)" + System.lineSeparator()
				//+ "np.save('/home/carlos/git/aa.npy', mask)" + System.lineSeparator()
				+ "non_zero = np.where(mask != 0)" + System.lineSeparator()
				//+ "task.update(str(non_zero[1][0]))" + System.lineSeparator()
				//+ "task.update(str(non_zero[0][0]))" + System.lineSeparator()
				+ "contours, _ = trace_edge(mask, non_zero[1][0], non_zero[0][0])" + System.lineSeparator()
				//+ "task.update('mmmmmmmmmmmmmm')" + System.lineSeparator()
				//+ "task.update(contours)" + System.lineSeparator()
				+ "contours = np.array(contours)" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours[:, 0].tolist()" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours[:, 1].tolist()" + System.lineSeparator();
		this.script = code;
	}
	
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		String envPath = "/home/carlos/micromamba/envs/sam";
		try (EfficientSamJ sam = initializeSam(envPath, img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}
}
