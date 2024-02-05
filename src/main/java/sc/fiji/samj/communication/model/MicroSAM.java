package sc.fiji.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.ui.SAMJLogger;

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
