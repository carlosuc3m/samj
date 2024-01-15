package io.bioimage.samj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apposed.appose.Conda;

public class Installer {
	final public static String SAM_WEIGHTS_NAME3 = "sam_vit_h_4b8939.pth";
	final public static String SAM_WEIGHTS_NAME = "sam_vit_b_01ec64.pth";
	final public static String SAM_MODEL_TYPE = "vit_b";
	
	final public static long SAM_BYTE_SIZE = 375042383;
	
	
	final static public String COMMON_ENV_NAME = "sam_common_env";
	final static public String ESAM_ENV_NAME = "efficient_sam_env";
	final static public String ESAM_NAME = "EfficientSAM";
	
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
	
	public boolean checkCommonPythonInstalled() {
		// TODO check whether the common environment is created or not
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
		mamba = new Conda(path);
		if (!checkCommonPythonInstalled() || force)
			mamba.create(COMMON_ENV_NAME, true, "-c", "conda-forge", "python=3.11", "-c", "pytorch", "pytorch", "torchvision", "cpuonly");
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
		if (checkEfficientSAMPackageInstalled() && !force)
			return;
		mamba.create(ESAM_ENV_NAME, true);
		String zipResourcePath = "EfficientSAM.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + ESAM_ENV_NAME + File.separator + ESAM_NAME;
        try (
        	InputStream zipInputStream = Installer.class.getResourceAsStream(zipResourcePath);
        	ZipInputStream zipInput = new ZipInputStream(zipInputStream);
        		) {
        	ZipEntry entry;
        	while ((entry = zipInput.getNextEntry()) != null) {
                File entryFile = new File(outputDirectory + File.separator + entry.getName());
                entryFile.getParentFile().mkdirs();
                try (OutputStream entryOutput = new FileOutputStream(entryFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInput.read(buffer)) != -1) {
                        entryOutput.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
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
