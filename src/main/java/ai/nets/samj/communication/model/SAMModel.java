package ai.nets.samj.communication.model;

import ai.nets.samj.communication.PromptsToNetAdapter;
import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.RandomAccessibleInterval;

/**
 * A common ground for various placeholder classes to inform
 * the system that even this network may be available/installed.
 * It is, however, not creating/instancing any connection to any
 * (Python) network code or whatsoever, that happens only after the
 * {@link SAMModel#instantiate(Logger)} is called, and reference
 * to such connection is returned.
 */
public interface SAMModel {

	String getName();
	String getDescription();
	boolean isInstalled();
	void setInstalled(boolean installed);

	/** Returns null if it is no installed. */
	PromptsToNetAdapter instantiate(
			final RandomAccessibleInterval<?> image,
			final SAMJLogger useThisLoggerForIt);
}