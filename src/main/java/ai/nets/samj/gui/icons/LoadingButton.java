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
package ai.nets.samj.gui.icons;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
/**
 * This class is a normal button that while pressed displays a loading animation
 * @author Carlos Garcia
 * @author Daniel Sage
 */
public class LoadingButton extends JButton {
	/**
	 * Serial version unique identifier
	 */
	private static final long serialVersionUID = -286903277589716471L;
	/**
	 * Icon for when the button is pressed
	 */
	private ImageIcon gifIcon;
	/**
	 * Label containing the text that the button displays when it is not pressed
	 */
	private JLabel textLabel;
	/**
	 * Label containing the loading animation to be displayed while the button is pressed
	 */
	private JLabel gifLabel;

	/**
	 * Constructor. Creates a button that has an icon inside. The icon changes when pressed.
	 * @param text
	 * 	the text inside the button
	 * @param filePath
	 * 	the path to the file that contains the image that is going to be used
	 * @param filename
	 * 	the name of the file that is going to be used
	 */
	public LoadingButton(String text, String filePath, String filename) {
		super();
        textLabel = new JLabel(text);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(textLabel);
        gifIcon = getIcon(filePath + "/" + filename, 30, 30);
        // Create JLabel to display GIF animation
        gifLabel = new JLabel(gifIcon);
        gifLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gifLabel.setVerticalAlignment(SwingConstants.CENTER);
        gifLabel.setVisible(false); // Initially hide GIF animation
        add(gifLabel);
	}

    // Override paintComponent to customize button appearance
    @Override
    protected void paintComponent(Graphics g) {
        // Draw the background
        if (getModel().isPressed()) {
            g.setColor(getBackground());
        } else {
            g.setColor(getBackground().darker());
        }
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw the text
        super.paintComponent(g);
    }
	
	private ImageIcon getIcon(String path, int width, int height) {
		while (path.indexOf("//") != -1) path = path.replace("//", "/");
		URL url = LoadingButton.class.getClassLoader().getResource(path);
		if (url == null) {
			File f = findJarFile(LoadingButton.class);
			if (f.getName().endsWith(".jar")) {
				try (URLClassLoader clsloader = new URLClassLoader(new URL[]{f.toURI().toURL()})){
					url = clsloader.getResource(path);
				} catch (IOException e) {
				}
			}
		}
		if (url != null) {
			ImageIcon img = new ImageIcon(url) ; 
			img.setImage(img.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT));
			return img;
		}
		return null;
	}
	
	private static File findJarFile(Class<?> clazz) {
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        if (protectionDomain != null) {
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                URL location = codeSource.getLocation();
                if (location != null) {
                    try {
                        return new File(URI.create(location.toURI().getSchemeSpecificPart()).getPath());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
	
	/**
	 * Set the button as pressed or not pressed, changing the image displayed
	 * @param isPressed
	 * 	whether the button is pressed or not
	 */
	public void setPressed(boolean isPressed) {
        textLabel.setVisible(!isPressed);

        gifLabel.setVisible(isPressed);
		
		this.setSelected(isPressed);
	}
}
