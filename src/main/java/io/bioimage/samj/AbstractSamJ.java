package io.bioimage.samj;

import java.time.LocalDateTime;

public class AbstractSamJ {

	/** Essentially, a syntactic-shortcut for Consumer<String> */
	public interface DebugTextPrinter { void printText(String text); }

	protected DebugTextPrinter debugPrinter = System.out::println;
	public void disableDebugPrinting() {
		debugPrinter = (text) -> {};
	}
	public void setDebugPrinter(final DebugTextPrinter lineComsumer) {
		if (lineComsumer != null) debugPrinter = lineComsumer;
	}

	protected boolean isDebugging = true;
	public void setDebugging(boolean newState) {
		isDebugging = newState;
	}
	public boolean isDebugging() {
		return isDebugging;
	}

	public void printScript(final String script, final String designationOfTheScript) {
		if (!isDebugging) return;
		debugPrinter.printText("START: =========== "+designationOfTheScript+" ===========");
		debugPrinter.printText(LocalDateTime.now().toString());
		debugPrinter.printText(script);
		debugPrinter.printText("END:   =========== "+designationOfTheScript+" ===========");
	}
}