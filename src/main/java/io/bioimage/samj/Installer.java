package io.bioimage.samj;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
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
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile();
		return weigthsFile.exists();
	}
	
	public boolean checkEfficientSAMTinyWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile();
		return weigthsFile.exists();
	}
	
	public boolean checkSAMWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile();
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
		Thread downloadThread = new Thread(() -> {
			try {
				downloadFile(ESAMS_URL, Paths.get(path, ESAM_ENV_NAME, ESAM_NAME, "weights", DownloadModel.getFileNameFromURLString(ESAMS_URL)).toFile());
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
