package ai.nets.samj.communication.model;

import ai.nets.samj.communication.PromptsToNetAdapter;
import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.RandomAccessibleInterval;

public class MicroSAM implements SAMModel {

	private boolean installed = false;

	@Override
	public String getName() {
		return "Micro SAM";
	}

	@Override
	public String getDescription() {
		return "Bla bla Micro SAM";
	}

	@Override
	public boolean isInstalled() {
		return installed;
	}

	@Override
	public PromptsToNetAdapter instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) {
		useThisLoggerForIt.error("Sorry, MicroSAM network is actually not installed...");
		return null;
	}

	@Override
	public void setInstalled(boolean installed) {
		this.installed = installed;		
	}
}
