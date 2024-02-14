package ai.nets.samj.communication.model;

import java.io.IOException;

import ai.nets.samj.communication.PromptsToEfficientSamJ;
import ai.nets.samj.communication.PromptsToNetAdapter;
import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.RandomAccessibleInterval;

public class EfficientViTSAML2 implements SAMModel {
	public static final String FULL_NAME = "EfficientViTSAM l2";

	private boolean installed = false;
	
	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public String getDescription() {
		return "Bla bla SAM Official ViT";
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
			useThisLoggerForIt.error(FULL_NAME + " experienced an error: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}
}
