package ai.nets.samj.gui.components;

import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class CustomComboBoxDropDownMenu extends BasicComboBoxRenderer {

    private JComboBox<ComboBoxItem> comboBox;
    
    private final int EXTRA_PADDING = 10;

    private static final long serialVersionUID = -2458150365733071194L;

	public CustomComboBoxDropDownMenu(JComboBox<ComboBoxItem> comboBox) {
        this.comboBox = comboBox;
    }

	public void updatePreferredSize() {
        if (comboBox != null && comboBox.getModel().getSize() > 0) {
            int elems = comboBox.getModel().getSize();
            int maxSize = 0;
            FontMetrics fm = getFontMetrics(getFont());
            for (int i = 0; i < elems; i ++) {
            	Object selectedItem = comboBox.getModel().getElementAt(i);
                String text = selectedItem == null ? "" : selectedItem.toString();
                int textWidth = SwingUtilities.computeStringWidth(fm, text);
                if (textWidth > maxSize) maxSize = textWidth;
            }
            //Object selectedItem = comboBox.getModel().getSize().getElementAt(comboBox.getSelectedIndex());
            Dimension size = super.getPreferredSize();
            size.width = Math.max(size.width, maxSize + EXTRA_PADDING);
            setPreferredSize(size);
        }
    }
}
