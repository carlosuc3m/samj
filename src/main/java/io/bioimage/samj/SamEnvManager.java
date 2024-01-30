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

import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker.TwoParameterConsumer;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;

public class SamEnvManager {
	final public static String SAM_WEIGHTS_NAME = "sam_vit_h_4b8939.pth";
	final public static String ESAM_SMALL_WEIGHTS_NAME ="efficient_sam_vits.pt";
	final public static String ESAM_TINY_WEIGHTS_NAME ="efficient_sam_vitt.pt";
	final public static String SAM_MODEL_TYPE = "vit_b";
	
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch", "torchvision", "skimage"});
	
	final public static List<String> INSTALL_CONDA_DEPS = Arrays.asList(new String[] {"scikit-image", "pytorch", "torchvision", "cpuonly"});

	final public static List<String> INSTALL_PIP_DEPS = Arrays.asList(new String[] {"appose"});

	final public static long SAM_BYTE_SIZE = 375042383;
	
	
	final static public String COMMON_ENV_NAME = "sam_common_env";
	final static public String ESAM_ENV_NAME = "efficient_sam_env";
	final static public String SAM_ENV_NAME = "sam_env";
	final static public String ESAM_NAME = "EfficientSAM";
	final static public String SAM_NAME = "SAM";
	
	final static public String ESAMS_URL = "https://raw.githubusercontent.com/yformer/EfficientSAM/main/weights/efficient_sam_vits.pt.zip";
	final static public String DEFAULT_DIR = new File("appose_x86_64").getAbsolutePath();
	
	private final static String MAMBA_RELATIVE_PATH = PlatformDetection.isWindows() ? 
			 File.separator + "Library" + File.separator + "bin" + File.separator + "micromamba.exe" 
			: File.separator + "bin" + File.separator + "micromamba";	

	private String path;
	private String mambaPath;
	private Mamba mamba;
	
	public static SamEnvManager create(String path) {
		SamEnvManager installer = new SamEnvManager();
		installer.path = path;
		installer.mambaPath = path + MAMBA_RELATIVE_PATH;
		return installer;
	}
	
	public static SamEnvManager create() {
		return create(DEFAULT_DIR);
	}
	
	public boolean checkMambaInstalled() {
		if (mamba != null) return true;
		File ff = new File(path + MAMBA_RELATIVE_PATH);
		if (!ff.exists()) return false;
		try {
			mamba = new Mamba(path);
		} catch (IOException | InterruptedException | ArchiveException | URISyntaxException | MambaInstallException e) {
			return false;
		}
		return true;
	}
	
	public boolean checkCommonPythonInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", COMMON_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		List<String> uninstalled = mamba.checkUninstalledDependenciesInEnv(pythonEnv.getAbsolutePath(), CHECK_DEPS);
		
		return uninstalled.size() == 0;
	}
	
	public boolean checkSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", SAM_ENV_NAME, SAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
	}
	
	public boolean checkEfficientSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
	}
	
	public boolean checkEfficientSAMSmallWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		return weigthsFile.isFile();
	}
	
	public boolean checkEfficientSAMTinyWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_TINY_WEIGHTS_NAME, ESAM_TINY_WEIGHTS_NAME).toFile();
		return weigthsFile.isFile();
	}
	
	public boolean checkSAMWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", SAM_ENV_NAME, SAM_NAME, "weights", SAM_WEIGHTS_NAME).toFile();
		return weigthsFile.isFile();
	}
	
	public void downloadSAM() {
		downloadSAM(false);
	}
	
	public void downloadSAM(boolean force) {
		if (!force && checkSAMWeightsDownloaded())
			return;
	}
	
	public void downloadESAMSmall(DownloadTracker.TwoParameterConsumer<String, Double> consumer) throws IOException, InterruptedException {
		downloadESAMSmall(false);
	}
	
	// TODO
	public void downloadESAMSmall(boolean force, DownloadTracker.TwoParameterConsumer<String, Double> consumer) throws IOException, InterruptedException {
		if (!force && checkEfficientSAMSmallWeightsDownloaded())
			return;

		String zipResourcePath = "efficient_sam_vits.pt.zip";
        String outputDirectory = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile().getAbsolutePath();
        try (
        	InputStream zipInputStream = SamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
        	ZipInputStream zipInput = new ZipInputStream(zipInputStream);
        		) {
        	ZipEntry entry;
        	while ((entry = zipInput.getNextEntry()) != null) {
                File entryFile = new File(outputDirectory + File.separator + entry.getName());
                if (entry.isDirectory()) {
                	entryFile.mkdirs();
                	continue;
                }
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
		/** TODO AVOID DOWONLOADING EFF SAM
		File file = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", DownloadModel.getFileNameFromURLString(ESAMS_URL)).toFile();
		file.getParentFile().mkdirs();
		Thread downloadThread = new Thread(() -> {
			try {
				downloadFile(ESAMS_URL, file);
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
        });
		DownloadTracker tracker = DownloadTracker.getFilesDownloadTracker(Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile().toString(),
				consumer, Arrays.asList(new String[] {ESAMS_URL}), downloadThread);
		downloadThread.start();
		Thread trackerThread = new Thread(() -> {
            try {
            	tracker.track();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
        });
		trackerThread.start();
		try { DownloadTracker.printProgress(downloadThread, consumer); } 
		catch (InterruptedException ex) { throw new InterruptedException("Model download interrupted."); }
		ZipUtils.unzipFolder(file.getAbsolutePath(), file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4));
		**/
	}
	
	public void downloadESAMSmall() throws IOException, InterruptedException {
		downloadESAMSmall(false);
	}
	
	public void downloadESAMSmall(boolean force) throws IOException, InterruptedException {
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
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!checkCommonPythonInstalled() || force) {
			mamba.create(COMMON_ENV_NAME, true, null, Arrays.asList(args));
			List<String> pipInstall = Arrays.asList(new String[] {"-m", "pip", "install"});
			for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			Mamba.runPythonIn(Paths.get(path,  "envs", COMMON_ENV_NAME).toFile(), pipInstall.stream().toArray( String[]::new ));
		}
	}
	
	public void installSAMPackage() throws IOException, InterruptedException {
		installSAMPackage(false);
	}
	
	public void installSAMPackage(boolean force) throws IOException, InterruptedException {
		if (checkSAMPackageInstalled() && !force)
			return;
		mamba.create(ESAM_ENV_NAME, true);
		String zipResourcePath = "SAM.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + SAM_ENV_NAME + File.separator + SAM_NAME;
        try (
        	InputStream zipInputStream = SamEnvManager.class.getResourceAsStream(zipResourcePath);
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
        String outputDirectory = mamba.getEnvsDir() + File.separator + ESAM_ENV_NAME;
        try (
        	InputStream zipInputStream = SamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
        	ZipInputStream zipInput = new ZipInputStream(zipInputStream);
        		) {
        	ZipEntry entry;
        	while ((entry = zipInput.getNextEntry()) != null) {
                File entryFile = new File(outputDirectory + File.separator + entry.getName());
                if (entry.isDirectory()) {
                	entryFile.mkdirs();
                	continue;
                }
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
	
	public void installMambaPython() throws IOException, InterruptedException, 
											ArchiveException, URISyntaxException, MambaInstallException {
		if (checkMambaInstalled()) return;
		mamba = new Mamba(path);
	}
	
	public void installSAM() throws IOException, InterruptedException, 
									ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkCommonPythonInstalled()) this.installPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadSAM(false);
	}
	
	public void installEfficientSAMSmall() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkCommonPythonInstalled()) this.installPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadESAMSmall(false);
	}
	
	public void installEfficientSAMTiny() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkCommonPythonInstalled()) this.installPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkEfficientSAMTinyWeightsDownloaded()) this.downloadESAMTiny(false);
	}
	
	public String getEfficientSAMSmallWeightsPath() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	public String getEfficientSAMTinyWeightsPath() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_TINY_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	public String getSAMWeightsPath() {
		File file = Paths.get(path, "envs", SAM_ENV_NAME, SAM_NAME, "weights", SAM_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	public String getPythonEnv() {
		File file = Paths.get(path, "envs", COMMON_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	public String getEfficientSamEnv() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	public static String getSAMWeightsName() {
		return SAM_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMSmallWeightsName() {
		return ESAM_SMALL_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMTinyWeightsName() {
		return ESAM_TINY_WEIGHTS_NAME;
	}
	
	/**
	 * Method that downloads a file
	 * @param downloadURL
	 * 	url of the file to be downloaded
	 * @param targetFile
	 * 	file where the file from the url will be downloaded too
	 * @throws IOException if there si any error downloading the file
	 * @throws URISyntaxException 
	 */
	public void downloadFile(String downloadURL, File targetFile) throws IOException, URISyntaxException {
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			URL website = new URL(downloadURL);
			rbc = Channels.newChannel(website.openStream());
			// Create the new model file as a zip
			fos = new FileOutputStream(targetFile);
			// Send the correct parameters to the progress screen
			FileDownloader downloader = new FileDownloader(rbc, fos);
			downloader.call();
		} finally {
			if (fos != null)
				fos.close();
			if (rbc != null)
				rbc.close();
		}
	}
}
