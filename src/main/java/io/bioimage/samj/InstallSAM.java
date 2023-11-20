package io.bioimage.samj;

import java.nio.file.Paths;

public class InstallSAM {
	final public static String SAM_WEIGHTS_NAME = "sam_vit_b_01ec64.pth";
	final public static String SAM_MODEL_TYPE = "vit_b";
	
	final public static long SAM_BYTE_SIZE = 375042383;
	
	public static String getWeightsFName() {
		//return Paths.get("models", "sam", SAM_WEIGHTS_NAME).toString();
		return "/home/carlos/Downloads/" + SAM_WEIGHTS_NAME;
	}
}
