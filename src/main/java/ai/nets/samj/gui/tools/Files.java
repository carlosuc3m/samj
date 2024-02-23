/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.gui.tools;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class Files {

	public static String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}

	public static String getHomeDirectory() {
		return FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator;	
	}
	
	public static String getDesktopDirectory() {
		return getHomeDirectory() + "Desktop" + File.separator;
	}
	
	public static File browseFile(String path) {
		JFileChooser fc = new JFileChooser(); 
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File dir = new File(path);
		if (dir.exists())
			fc.setCurrentDirectory(dir);
		
		int ret = fc.showOpenDialog(null); 
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = new File(fc.getSelectedFile().getAbsolutePath());
			if (file.exists())
				return file;
		}
		return null;
	}
	
	public static File browseDirectory(String path) {
		JFileChooser fc = new JFileChooser(); 
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File dir = new File(path);
		if (dir.exists())
			fc.setCurrentDirectory(dir);

		int ret = fc.showOpenDialog(null); 
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = new File(fc.getSelectedFile().getAbsolutePath());
			if (file.exists())
				return file;
		}
		return null;
	}
}
