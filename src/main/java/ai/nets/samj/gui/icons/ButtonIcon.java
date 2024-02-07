package ai.nets.samj.gui.icons;

import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

public class ButtonIcon extends JButton {
	
	private static final long serialVersionUID = 7396676607184967666L;
	
	private static final String PRESSED_PREFIX = "pressed_";

	private ImageIcon normalIcon;
	private ImageIcon pressedIcon;

	public ButtonIcon(String text, String filePath, String filename) {
		super();
		try {
			normalIcon = getIcon(filePath + "/" + filename);
			pressedIcon = getIcon(filePath + "/" + PRESSED_PREFIX + filename);
			if (normalIcon != null) {
				setIcon(normalIcon);
				setBorder(BorderFactory.createEtchedBorder());
				setOpaque(false);
				setContentAreaFilled(false);
				setPreferredSize(new Dimension(58, 58));
				setVerticalTextPosition(SwingConstants.BOTTOM);
				setHorizontalTextPosition(SwingConstants.CENTER);
				setText(text);
			}
			if (pressedIcon != null) {
				this.setPressedIcon(pressedIcon);
			}
		} 
		catch (Exception ex) {
			setText(text);
		}
	}
	
	private ImageIcon getIcon(String path) {
		URL url = ButtonIcon.class.getClassLoader().getResource(path);
		if (url != null) {
			ImageIcon img = new ImageIcon(url, "") ;  
			Image image = img.getImage();
			Image scaled = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
		}
		return null;
	}
	
	public void setPressed(boolean isPressed) {
		if (isPressed) this.setIcon(pressedIcon);
		else this.setIcon(normalIcon);
		
		this.setSelected(isPressed);
	}
}
