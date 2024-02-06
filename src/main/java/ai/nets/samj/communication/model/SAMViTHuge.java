package ai.nets.samj.communication.model;

import ai.nets.samj.communication.PromptsToNetAdapter;
import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.RandomAccessibleInterval;

public class SAMViTHuge implements SAMModel {

	private boolean installed = false;
	public static final String FULL_NAME = "ViT Huge";
	
	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public String getDescription() {
		return "SAM Official ViT Huge";
	}

	@Override
	public boolean isInstalled() {
		return installed;
	}

	@Override
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) {
		useThisLoggerForIt.error("Sorry, ViT Huge network is actually not installed...");
		return null;
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}
}
