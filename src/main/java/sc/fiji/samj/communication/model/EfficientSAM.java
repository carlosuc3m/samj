package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.ui.SAMJLogger;
import sc.fiji.samj.communication.PromptsToEfficientSamJ;
import java.io.IOException;

public class EfficientSAM implements SAMModel {
	private boolean installed = false;
	private static final String FULL_NAME = "Efficient SAM";

	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public String getDescription() {
		return "Bla bla Efficient SAM";
	}

	@Override
	public boolean isInstalled() {
		return installed;
	}

	@Override
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) {
		try {
			return new PromptsToEfficientSamJ(image,useThisLoggerForIt);
		} catch (IOException | InterruptedException | RuntimeException e) {
			useThisLoggerForIt.error(FULL_NAME+" experienced an error: "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}
}