package io.bioimage.samj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.utils.ZipUtils;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apposed.appose.Conda;

public class Installer {
	final public static String SAM_WEIGHTS_NAME = "sam_vit_h_4b8939.pth";
	final public static String SAM_MODEL_TYPE = "vit_b";
	
	final public static List<String> REQUIRED_DEPS = Arrays.asList(new String[] {"pytorch", "torchvision", "cpuonly"});
	
	final public static long SAM_BYTE_SIZE = 375042383;
	
	
	final static public String COMMON_ENV_NAME = "sam_common_env";
	final static public String ESAM_ENV_NAME = "efficient_sam_env";
	final static public String SAM_ENV_NAME = "sam_env";
	final static public String ESAM_NAME = "EfficientSAM";
	final static public String SAM_NAME = "SAM";
	
	final static public String ESAMS_URL = "https://github.com/yformer/EfficientSAM/raw/main/weights/efficient_sam_vits.pt.zip";
	
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
	
	public boolean checkMambaInstalled() {
		if (mamba != null) return true;
		File ff = new File(path + MAMBA_RELATIVE_PATH);
		if (!ff.exists()) return false;
		try {
			mamba = new Conda(path);
		} catch (IOException | InterruptedException | ArchiveException | URISyntaxException e) {
			return false;
		}
		return true;
	}
	
	public boolean checkCommonPythonInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", COMMON_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		int sizeUninstalled = 0;
		// TODO int sizeUninstalled = REQUIRED_DEPS.stream().filter(dep -> !mamba.isPackageInEnv()).collect(Collectors.toList()).size();
		
		return sizeUninstalled == 0;
	}
	
	public boolean checkSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", SAM_ENV_NAME, SAM_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		return true;
	}
	
	public boolean checkEfficientSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		return true;
	}
	
	public boolean checkEfficientSAMSmallWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile();
		return weigthsFile.exists();
	}
	
	public boolean checkEfficientSAMTinyWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile();
		return weigthsFile.exists();
	}
	
	public boolean checkSAMWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", SAM_ENV_NAME, SAM_NAME, "weights").toFile();
		return weigthsFile.exists();
	}
	
	public void downloadSAM() {
		downloadSAM(false);
	}
	
	public void downloadSAM(boolean force) {
		if (!force && checkSAMWeightsDownloaded())
			return;
	}
	
	public void downloadESAMSmall(DownloadTracker.TwoParameterConsumer<String, Double> consumer) throws IOException {
		downloadESAMSmall(false);
	}
	
	public void downloadESAMSmall(boolean force, DownloadTracker.TwoParameterConsumer<String, Double> consumer) throws IOException {
		if (!force && checkEfficientSAMSmallWeightsDownloaded())
			return;
		File file = Paths.get(path, ESAM_ENV_NAME, ESAM_NAME, "weights", DownloadModel.getFileNameFromURLString(ESAMS_URL)).toFile();
		Thread downloadThread = new Thread(() -> {
			try {
				downloadFile(ESAMS_URL, file);
			} catch (IOException e) {
				e.printStackTrace();
			}
        });
		downloadThread.start();
		DownloadTracker tracker = DownloadTracker.getFilesDownloadTracker(Paths.get(path, ESAM_ENV_NAME, ESAM_NAME, "weights").toFile().toString(),
				null, Arrays.asList(new String[] {ESAMS_URL}), downloadThread);
		Thread trackerThread = new Thread(() -> {
            try {
            	tracker.track();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
        });
		trackerThread.start();
		ZipUtils.unzipFolder(file.getAbsolutePath(), file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4));
	}
	
	public void downloadESAMSmall() throws IOException {
		downloadESAMSmall(false);
	}
	
	public void downloadESAMSmall(boolean force) throws IOException {
		if (!force && checkEfficientSAMSmallWeightsDownloaded())
			return;
		TwoParameterConsumer<String, Double> consumer = DownloadTracker.createConsumerProgress();
		downloadESAMSmall(force, consumer);
	}
	
	public void downloadESAMTiny() {
		downloadESAMTiny(false);
	}
	
	public void downloadESAMTiny(boolean force) {
		if (!force && checkEfficientSAMTinyWeightsDownloaded())
			return;
		
	}
	
	public void installPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		installPython(false);
	}
	
	public void installPython(boolean force) throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : REQUIRED_DEPS) args[c ++] = ss;
		if (!checkCommonPythonInstalled() || force)
			mamba.create(COMMON_ENV_NAME, true, args);
	}
	
	public void installSAMPackage() throws IOException, InterruptedException {
		installSAMPackage(false);
	}
	
	public void installSAMPackage(boolean force) throws IOException, InterruptedException {
		if (checkEfficientSAMPackageInstalled() && !force)
			return;
		mamba.create(ESAM_ENV_NAME, true);
		String zipResourcePath = "SAM.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + SAM_ENV_NAME + File.separator + SAM_NAME;
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
	
	public void installMambaPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		if (checkMambaInstalled()) return;
		mamba = new Conda(path);
	}
	
	public void installSAM() throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkCommonPythonInstalled()) this.installPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadSAM(false);
	}
	
	public void installEfficientSAMSmall() throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkCommonPythonInstalled()) this.installPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadESAMSmall(false);
	}
	
	public void installEfficientSAMTiny() throws IOException, InterruptedException, ArchiveException, URISyntaxException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkCommonPythonInstalled()) this.installPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkEfficientSAMTinyWeightsDownloaded()) this.downloadESAMTiny(false);
	}
	
	public static String getSAMWeightsName() {
		return SAM_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMSmallWeightsName() {
		return SAM_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMTinyWeightsName() {
		return SAM_WEIGHTS_NAME;
	}
	
	/**
	 * Method that downloads a file
	 * @param downloadURL
	 * 	url of the file to be downloaded
	 * @param targetFile
	 * 	file where the file from the url will be downloaded too
	 * @throws IOException if there si any error downloading the file
	 */
	public void downloadFile(String downloadURL, File targetFile) throws IOException {
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			URL website = new URL(downloadURL);
			rbc = Channels.newChannel(website.openStream());
			// Create the new model file as a zip
			fos = new FileOutputStream(targetFile);
			// Send the correct parameters to the progress screen
			FileDownloader downloader = new FileDownloader(rbc, fos);
		} finally {
			if (fos != null)
				fos.close();
			if (rbc != null)
				rbc.close();
		}
	}
}
