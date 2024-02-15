package ai.nets.samj;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.bioimageio.download.DownloadTracker;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.apposed.appose.MambaInstallerUtils;

/*
 * Class that is manages the installation of SAM and EfficientSAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class SamEnvManager {
	final public static String SAM_WEIGHTS_NAME = "sam_vit_h_4b8939.pth";
	final public static String ESAM_SMALL_WEIGHTS_NAME ="efficient_sam_vits.pt";
	final public static String SAM_MODEL_TYPE = "vit_b";
	final public static String DEFAULT_EVITSAM = "l0";
	
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch", "torchvision", "skimage"});
	
	final public static List<String> CHECK_DEPS_EVSAM = Arrays.asList(new String[] {"onnxsim", "timm", "onnx", "segment_anything"});
	
	final public static List<String> INSTALL_CONDA_DEPS = Arrays.asList(new String[] {"scikit-image", "pytorch", "torchvision", "cpuonly"});
	
	final public static List<String> INSTALL_EVSAM_CONDA_DEPS = Arrays.asList(new String[] {"cmake", "onnx", "timm"});

	final public static List<String> INSTALL_PIP_DEPS = Arrays.asList(new String[] {"appose"});

	final public static List<String> INSTALL_EVSAM_PIP_DEPS = Arrays.asList(new String[] {"onnxsim", "segment_anything"});

	final public static long SAM_BYTE_SIZE = 375042383;
	
	
	final static public String COMMON_ENV_NAME = "sam_common_env";
	final static public String ESAM_ENV_NAME = "efficient_sam_env";
	final static public String EVITSAM_ENV_NAME = "efficientvit_sam_env";
	final static public String SAM_ENV_NAME = "sam_env";
	final static public String ESAM_NAME = "EfficientSAM";
	final static public String EVITSAM_NAME = "efficientvit";
	final static public String SAM_NAME = "SAM";
	
	final static public String ESAMS_URL = "https://raw.githubusercontent.com/yformer/EfficientSAM/main/weights/efficient_sam_vits.pt.zip";
	final static private String EVITSAM_URL = "https://huggingface.co/han-cai/efficientvit-sam/resolve/main/%s.pt?download=true";
	final static public String DEFAULT_DIR = new File("appose_x86_64").getAbsolutePath();
	
	private static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	private static long millis = System.currentTimeMillis();
	
	private final static String MAMBA_RELATIVE_PATH = PlatformDetection.isWindows() ? 
			 File.separator + "Library" + File.separator + "bin" + File.separator + "micromamba.exe" 
			: File.separator + "bin" + File.separator + "micromamba";	

	private String path;
	private Mamba mamba;
	private Consumer<String> consumer;
	
	public static SamEnvManager create(String path) {
		return create(path, (ss) -> {});
	}
	
	public static SamEnvManager create(String path, Consumer<String> consumer) {
		SamEnvManager installer = new SamEnvManager();
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	public static SamEnvManager create() {
		return create(DEFAULT_DIR);
	}
	
	public static SamEnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, consumer);
	}
	
	private void passToConsumer(String str) {
		consumer.accept(str);
		millis = System.currentTimeMillis();
	}
	
	public boolean checkMambaInstalled() {
		File ff = new File(path + MAMBA_RELATIVE_PATH);
		if (!ff.exists()) return false;
		return mamba.checkMambaInstalled();
	}
	
	public boolean checkEfficientSAMPythonInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", COMMON_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		List<String> uninstalled;
		try {
			uninstalled = mamba.checkUninstalledDependenciesInEnv(pythonEnv.getAbsolutePath(), CHECK_DEPS);
		} catch (MambaInstallException e) {
			return false;
		}
		
		return uninstalled.size() == 0;
	}
	
	public boolean checkEfficientViTSAMPythonInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", EVITSAM_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		List<String> uninstalled;
		try {
			uninstalled = mamba.checkUninstalledDependenciesInEnv(pythonEnv.getAbsolutePath(), CHECK_DEPS_EVSAM);
		} catch (MambaInstallException e) {
			return false;
		}
		
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
	
	public boolean checkEfficientViTSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
	}
	
	public boolean checkEfficientSAMSmallWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		return weigthsFile.isFile();
	}
	
	public boolean checkEfficientViTSAMWeightsDownloaded(String modelType) {
		if (!EfficientViTSamJ.getListOfSupportedEfficientViTSAM().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientViT models: " 
												+ EfficientViTSamJ.getListOfSupportedEfficientViTSAM());
		File weigthsFile = Paths.get(this.path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toFile();
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
	
	public void downloadESAMSmall() throws IOException, InterruptedException {
		downloadESAMSmall(false);
	}
	
	public void downloadESAMSmall(boolean force) throws IOException {
		if (!force && checkEfficientSAMSmallWeightsDownloaded())
			return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTSAM WEIGHTS");
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
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTSAM WEIGHTS INSTALLATION");
            throw ex;
        }
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTSAM WEIGHTS INSTALLED");
	}
	
	public void downloadEfficientViTSAM() throws IOException, InterruptedException {
		downloadEfficientViTSAM(DEFAULT_EVITSAM, false, null);
	}
	
	public void downloadEfficientViTSAM(boolean force) throws IOException, InterruptedException {
		downloadEfficientViTSAM(DEFAULT_EVITSAM, force, null);
	}
	
	public void downloadEfficientViTSAM(String modelType) throws IOException, InterruptedException {
		downloadEfficientViTSAM(modelType, false, null);
	}
	
	public void downloadEfficientViTSAM(String modelType, boolean force) throws IOException, InterruptedException {
		downloadEfficientViTSAM(modelType, force, null);
	}

	public void downloadEfficientViTSAM(String modelType, DownloadTracker.TwoParameterConsumer<String, Double> consumer) throws IOException, InterruptedException {
		downloadEfficientViTSAM(modelType, false, consumer); 
	}
	
	public void downloadEfficientViTSAM(String modelType, boolean force, 
			DownloadTracker.TwoParameterConsumer<String, Double> consumer2) throws IOException, InterruptedException {
		if (!EfficientViTSamJ.getListOfSupportedEfficientViTSAM().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientViT models: " 
												+ EfficientViTSamJ.getListOfSupportedEfficientViTSAM());
		if (!force && checkEfficientViTSAMWeightsDownloaded(modelType))
			return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTVITSAM WEIGHTS (" + modelType + ")");
        try {
    		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", DownloadModel.getFileNameFromURLString(String.format(EVITSAM_URL, modelType))).toFile();
    		file.getParentFile().mkdirs();
    		URL url = MambaInstallerUtils.redirectedURL(new URL(String.format(EVITSAM_URL, modelType)));
    		Thread downloadThread = new Thread(() -> {
    			try {
    				downloadFile(url.toString(), file);
    			} catch (IOException | URISyntaxException e) {
    				e.printStackTrace();
    			}
            });
    		downloadThread.start();
    		long size = DownloadModel.getFileSize(url);
        	while (downloadThread.isAlive()) {
        		try {Thread.sleep(280);} catch (InterruptedException e) {break;}
        		double progress = Math.round( (double) 100 * file.length() / size ); 
        		if (progress < 0 || progress > 100) passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM WEIGHTS DOWNLOAD: UNKNOWN%");
        		else passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM WEIGHTS DOWNLOAD: " + progress + "%");
        	}
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for EfficientViTSAM " + modelType + ": " + String.format(EVITSAM_URL, modelType));

		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM WEIGHTS INSTALLED");
	}
	
	public void installEfficientSAMPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException, MambaInstallException {
		installEfficientSAMPython(false);
	}
	
	public void installEfficientSAMPython(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!checkEfficientSAMPythonInstalled() || force) {
			try {
				mamba.create(COMMON_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			ArrayList<String> pipInstall = new ArrayList<String>();
			for (String ss : new String[] {"-m", "pip", "install"}) pipInstall.add(ss);
			for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			try {
				Mamba.runPythonIn(Paths.get(path,  "envs", COMMON_ENV_NAME).toFile(), pipInstall.stream().toArray( String[]::new ));
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- PYTHON ENVIRONMENT CREATED");
	}
	
	public void installEfficientViTSAMPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException, MambaInstallException {
		installEfficientViTSAMPython(false);
	}
	
	public void installEfficientViTSAMPython(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE EFFICIENTVITSAM PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size() + INSTALL_EVSAM_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		for (String ss : INSTALL_EVSAM_CONDA_DEPS) args[c ++] = ss;
		if (!checkEfficientViTSAMPythonInstalled() || force) {
			try {
				mamba.create(EVITSAM_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			try {
				installOnnxsim(Paths.get(path, "envs", EVITSAM_ENV_NAME).toFile() );
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM PYTHON ENVIRONMENT CREATED");
	}
	
	// TODO move this to mamba
	private void installOnnxsim(File envFile) throws IOException, InterruptedException {
		final List< String > cmd = new ArrayList<>();
		if ( PlatformDetection.isWindows() )
			cmd.addAll( Arrays.asList( "cmd.exe", "/c" ) );
		cmd.add( Paths.get( envFile.getAbsolutePath(), (PlatformDetection.isWindows() ? "python.exe" : "bin/python") ).toAbsolutePath().toString() );
		cmd.addAll( Arrays.asList( new String[] {"-m", "pip", "install"} ) );
		cmd.addAll( INSTALL_PIP_DEPS );
		cmd.addAll( INSTALL_EVSAM_PIP_DEPS );
		final ProcessBuilder builder = new ProcessBuilder().directory( envFile );
		//builder.inheritIO();
		if ( PlatformDetection.isWindows() )
		{
			final Map< String, String > envs = builder.environment();
			final String envDir = envFile.getAbsolutePath();
			envs.put( "Path", envDir + ";" + envs.get( "Path" ) );
			envs.put( "Path", Paths.get( envDir, "Scripts" ).toString() + ";" + envs.get( "Path" ) );
			envs.put( "Path", Paths.get( envDir, "Library" ).toString() + ";" + envs.get( "Path" ) );
			envs.put( "Path", Paths.get( envDir, "Library", "Bin" ).toString() + ";" + envs.get( "Path" ) );
		} else {
			final Map< String, String > envs = builder.environment();
			final String envDir = envFile.getAbsolutePath();
			envs.put( "PATH", envDir + ":" + envs.get( "PATH" ) );
			envs.put( "PATH", Paths.get( envDir, "bin" ).toString() + ":" + envs.get( "PATH" ) );
		}
		if ( builder.command( cmd ).start().waitFor() != 0 )
			throw new RuntimeException();
	}
	
	public void installSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		installSAMPackage(false);
	}
	
	public void installSAMPackage(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (checkSAMPackageInstalled() && !force)
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to SAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'SAM' PYTHON PACKAGE");
		try {
			mamba.create(ESAM_ENV_NAME, true);
		} catch (MambaInstallException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'SAM' PYTHON PACKAGE INSTALLATION");
			throw new MambaInstallException("Unable to SAM without first installing Mamba.");
		} catch (IOException | InterruptedException | RuntimeException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'SAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
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
        } catch (IOException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'SAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'SAM' PYTHON PACKAGE INSATLLED");
	}
	
	public void installEfficientSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		installEfficientSAMPackage(false);
	}
	
	public void installEfficientSAMPackage(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (checkEfficientSAMPackageInstalled() && !force)
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to EfficientSAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'EFFICIENTSAM' PYTHON PACKAGE");
		try {
			mamba.create(ESAM_ENV_NAME, true);
		} catch (MambaInstallException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTSAM' PYTHON PACKAGE INSTALLATION");
			throw new MambaInstallException("Unable to install EfficientSAM without first installing Mamba.");
		} catch (IOException | InterruptedException | RuntimeException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTSAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
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
        } catch (IOException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTSAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'EFFICIENTSAM' PYTHON PACKAGE INSATLLED");
	}
	
	public void installEfficientViTSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		installEfficientViTSAMPackage(false);
	}
	
	public void installEfficientViTSAMPackage(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (checkEfficientViTSAMPackageInstalled() && !force)
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to EfficientViTSAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'EFFICIENTVITSAM' PYTHON PACKAGE");
		String zipResourcePath = "efficientvit.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + EVITSAM_ENV_NAME;
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
        } catch (IOException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTVITSAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'EFFICIENTVITSAM' PYTHON PACKAGE INSATLLED");
	}
	
	public void installMambaPython() throws IOException, InterruptedException, 
	ArchiveException, URISyntaxException, MambaInstallException{
		if (checkMambaInstalled()) return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING MICROMAMBA");
		try {
			mamba.installMicromamba();
		} catch (IOException | InterruptedException | ArchiveException | URISyntaxException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MICROMAMBA INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MICROMAMBA INSTALLED");
	}
	
	public void installSAM() throws IOException, InterruptedException, 
									ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkEfficientSAMPythonInstalled()) this.installEfficientSAMPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadSAM(false);
	}
	
	public void installEfficientSAMSmall() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkEfficientSAMPythonInstalled()) this.installEfficientSAMPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadESAMSmall(false);
	}
	
	public void installEfficientViTSAM() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		installEfficientViTSAM(DEFAULT_EVITSAM);
	}
	
	public void installEfficientViTSAM(String modelType) throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkEfficientViTSAMPythonInstalled()) this.installEfficientViTSAMPython();
		
		if (!this.checkEfficientViTSAMPackageInstalled()) this.installEfficientViTSAMPackage();
		
		if (!this.checkEfficientViTSAMWeightsDownloaded(modelType)) this.downloadEfficientViTSAM(modelType, false);
	}
	
	public String getEfficientSAMSmallWeightsPath() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	public String getEfficientViTSAMWeightsPath() {
		return getEfficientViTSAMWeightsPath(DEFAULT_EVITSAM);
	}
	
	public String getEfficientViTSAMWeightsPath(String modelType) {
		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toFile();
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
	
	public String getEfficientViTSamEnv() {
		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	public String getEnvsPath() {
		return Paths.get(path, "envs").toFile().getAbsolutePath();
	}
	
	public static String getSAMWeightsName() {
		return SAM_WEIGHTS_NAME;
	}
	
	public static String getEfficientSAMSmallWeightsName() {
		return ESAM_SMALL_WEIGHTS_NAME;
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
	
	/**
	 * For a fresh, installation, SAMJ might need to download first micromamba. In that case, this method
	 * returns the progress made for its download.
	 * @return progress made downloading Micromamba
	 */
	public double getMambaInstallationProcess() {
		return this.mamba.getMicromambaDownloadProgress();
	}
	
	public String getEnvCreationProgress() {
		return this.getEnvCreationProgress();
	}
	
	private Thread reportProgress(String startStr) {
		Thread currentThread = Thread.currentThread();
		Thread thread = new Thread (() -> {
			passToConsumer(startStr);
			while (currentThread.isAlive()) {
				try {Thread.sleep(300);} catch (InterruptedException e) {break;}
				if (System.currentTimeMillis() - millis > 300)
					passToConsumer("");
			}
		});
		thread.start();
		return thread;
	}
}
