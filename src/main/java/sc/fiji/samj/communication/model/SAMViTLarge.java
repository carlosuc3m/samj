package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import sc.fiji.samj.communication.PromptsToFakeSamJ;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.ui.SAMJLogger;

public class SAMViTLarge implements SAMModel {
	
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
		return true;
	}

	@Override
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) {
		return new PromptsToFakeSamJ(useThisLoggerForIt, "Official_ViT");
	}
}
