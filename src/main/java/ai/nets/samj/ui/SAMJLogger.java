package ai.nets.samj.ui;

public interface SAMJLogger {

	/**
	 * Method that displays info about what SamJ is doing
	 * @param text
	 * 	the information from the plugin
	 */
	void info(String text);
	/**
	 * Method that displays warnings about what SamJ is doing
	 * @param text
	 * 	the information from the plugin
	 */
	void warn(String text);
	/**
	 * Method that displays infromation about the errors that happened during the program execution
	 * @param text
	 * 	the error string
	 */
	void error(String text);
}