package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.ui.SAMJLogger;

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
