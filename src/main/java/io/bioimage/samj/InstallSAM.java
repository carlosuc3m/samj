package io.bioimage.samj;

import java.nio.file.Paths;

public class InstallSAM {
	final public static String SAM_WEIGHTS_NAME3 = "sam_vit_h_4b8939.pth";
	final public static String SAM_WEIGHTS_NAME = "sam_vit_b_01ec64.pth";
	final public static String SAM_MODEL_TYPE = "vit_b";
	
	final public static long SAM_BYTE_SIZE = 375042383;
	
	public static String getWeightsFName() {
		//return Paths.get("models", "sam", SAM_WEIGHTS_NAME).toString();
		return "/home/carlos/Downloads/" + SAM_WEIGHTS_NAME;
	}
	
	
	public static void createInstaller() {
		
	}
	
	public boolean checkPythonMambaInstalled() {
		return false;
	}
	
	public boolean checkSAMPackageInstalled() {
		return false;
	}
	
	public boolean checkEfficientSAMPackageInstalled() {
		return false;
	}
	
	public boolean checkEfficientSAMSmallWeightsDownloaded() {
		return false;
	}
	
	public boolean checkEfficientSAMTinyWeightsDownloaded() {
		return false;
	}
	
	public boolean checkSAMWeightsDownloaded() {
		return false;
	}
	
	public void installPython() {
		
	}
	
	public void installSAMPackage() {
		
	}
	
	public void installEfficientSAMPackage() {
		
	}
	
	public void installSAM() {
		
	}
	
	public void installEfficientSAMSmall() {
		
	}
	
	public void installEfficientSAMTiny() {
		
	}
	
	public static String getSAMWeights() {
		return SAM_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMSmallWeights() {
		return SAM_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMTinyWeights() {
		return SAM_WEIGHTS_NAME;
	}
}
