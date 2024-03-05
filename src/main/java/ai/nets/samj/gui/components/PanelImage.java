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
package ai.nets.samj.gui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * A custom JPanel that displays an image.
 *
 * @author Daniel Sage
 */
public class PanelImage extends JPanel {

	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = -1274516935269808463L;
	/**
	 * The image to display.
	 */
	private Image	image;
	/**
	 * The width of the image.
	 */
	private int		w	= -1;
	/**
	 * The height of the image.
	 */
	private int		h	= -1;

	/**
	 * Creates an empty PanelImage object.
	 */
	public PanelImage() {
		super();
	}

	/**
	 * Creates a PanelImage object with the specified width and height.
	 *
	 * @param w the width of the image
	 * @param h the height of the image
	 */
	public PanelImage(int w, int h) {
		super();
		image = null;
		this.w = w;
		this.h = h;
	}

	/**
	 * Creates a PanelImage object with the specified image file and width and height.
	 *
	 * @param filename
	 *  the name of the image file
	 * @param w
	 *  the width of the image
	 * @param h
	 *  the height of the image
	 */
	public PanelImage(String filename, int w, int h) {
		super();
		System.out.println("Working dir:  " + System.getProperty("user.dir"));
		File file = new File("celegans.jpg");
		System.out.println(file.toString());
		try {
			image = ImageIO.read(file);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sets the image to display.
	 *
	 * @param image
	 *  the image to display
	 */
	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}

	@Override
	/**
	 * Paints the image on the panel.
	 *
	 * @param g
	 *  the Graphics object used for painting
	 */
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			if (w < 0)
				g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			else {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.drawImage(image, (getWidth()-w)/2, 0, w, h, null);
			}
		}
		else {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
	}

}
