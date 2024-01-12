package io.bioimage.samj;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apposed.appose.Conda;

public class Installer {
	final public static String SAM_WEIGHTS_NAME3 = "sam_vit_h_4b8939.pth";
	final public static String SAM_WEIGHTS_NAME = "sam_vit_b_01ec64.pth";
	final public static String SAM_MODEL_TYPE = "vit_b";
	
	final public static long SAM_BYTE_SIZE = 375042383;
	
	
	private final static String MAMBA_RELATIVE_PATH = PlatformDetection.isWindows() ? 
			 File.separator + "Library" + File.separator + "bin" + File.separator + "micromamba.exe" 
			: File.separator + "bin" + File.separator + "micromamba";
	
	public static String getWeightsFName() {
		//return Paths.get("models", "sam", SAM_WEIGHTS_NAME).toString();
		return "/home/carlos/Downloads/" + SAM_WEIGHTS_NAME;
	}
	

	private String path;
	private String mambaPath;
	private Conda mamba;
	
	public static Installer createInstaller(String path) {
		Installer installer = new Installer();
		installer.path = path;
		installer.mambaPath = path + MAMBA_RELATIVE_PATH;
		return installer;
	}
	
	public boolean checkPythonMambaInstalled() {
		if (new File(mambaPath).isFile() == false) 
			return false;
		try {
			mamba = new Conda(path);
		} catch (IOException | InterruptedException | ArchiveException | URISyntaxException e) {
			return false;
		}
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
	
	public void installPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		installPython(false);
	}
	
	public void installPython(boolean force) throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		if (!force)
			mamba = new Conda(path);
		else
			mamba = null;// TODO decide if it is necessary
	}
	
	public void installSAMPackage() throws IOException, InterruptedException {
		installSAMPackage(false);
	}
	
	public void installSAMPackage(boolean force) throws IOException, InterruptedException {
		mamba.create("samJ", false, null);
	}
	
	public void installEfficientSAMPackage() throws IOException, InterruptedException {
		installEfficientSAMPackage(false);
	}
	
	public void installEfficientSAMPackage(boolean force) throws IOException, InterruptedException {
		mamba.create("efficientSamJ", false, null);
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
