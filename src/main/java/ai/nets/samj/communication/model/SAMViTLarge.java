package ai.nets.samj.communication.model;

import ai.nets.samj.communication.PromptsToFakeSamJ;
import ai.nets.samj.communication.PromptsToNetAdapter;
import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.RandomAccessibleInterval;

public class SAMViTLarge implements SAMModel {

	private boolean installed = false;
	
	@Override
	public String getName() {
		return "ViT Large";
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
		return new PromptsToFakeSamJ(useThisLoggerForIt, "Official_ViT");
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}
}
