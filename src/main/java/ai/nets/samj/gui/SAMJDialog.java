package ai.nets.samj.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.communication.model.SAMModels;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.gui.components.GridPanel;
import ai.nets.samj.gui.icons.ButtonIcon;
import ai.nets.samj.gui.tools.Tools;
import ai.nets.samj.ui.PromptsResultsDisplay;
import ai.nets.samj.ui.SAMJLogger;
import ai.nets.samj.ui.UtilityMethods;
import ai.nets.samj.SamEnvManager;

public class SAMJDialog extends JPanel implements ActionListener, PopupMenuListener {

	private static final long serialVersionUID = -4362794696325316195L;
	
	private static final String RESOURCES_FOLDER = "icons/";
	
	private JButton bnClose = new JButton("Close");
	private JButton bnHelp = new JButton("Help");
	private JButton bnStart = new JButton("Start/Encode");
	private JButton bnStop = new JButton("Stop");
	// TODO private JButton bnComplete = new JButton("Auto-Complete (soon...)");
	// TODO private JButton bnRoi2Mask = new JButton("Create Mask (soon...)");
	private JButton bnComplete = new JButton("Coming soon...");
	private JButton bnRoi2Mask = new JButton("Coming soon...");
	private JTextField txtStatus = new JTextField("(c) SAMJ team 2024");
	
	private ButtonIcon bnRect = new ButtonIcon("Rect", RESOURCES_FOLDER, "rect.png");
	private ButtonIcon bnPoints = new ButtonIcon("Points", RESOURCES_FOLDER, "edit.png");
	private ButtonIcon bnBrush = new ButtonIcon("Brush", RESOURCES_FOLDER, "github.png");
	private ButtonIcon bnMask = new ButtonIcon("Mask", RESOURCES_FOLDER, "help.png");
	private JCheckBox chkROIManager = new JCheckBox("Add to ROI Manager", true);

	private JComboBox<ComboBoxItem> cmbImage = new JComboBox<ComboBoxItem>();
	
	private final SAMModelPanel panelModel;
	private final SAMJLogger GUIsOwnLog;
	private final SAMJLogger logForNetworks;
	private final UtilityMethods consumerMethods;
	
	private Integer selectedID = null;
	
	public interface PromptsFunctionalInterface { PromptsResultsDisplay getPrompts(Object image); }
	private PromptsFunctionalInterface displayInterface;
	private PromptsResultsDisplay display;
	
	private boolean encodingsDone = false;

	public SAMJDialog(final SAMModels availableModel, UtilityMethods consumerMethods) {
		this(availableModel, consumerMethods, null, null);
	}

	public SAMJDialog(final SAMModels availableModel,
						UtilityMethods consumerMethods,
	                    final SAMJLogger guilogger) {
		this(availableModel, consumerMethods, guilogger, null);
	}

	public SAMJDialog(final SAMModels availableModel, UtilityMethods consumerMethods,
	                  final SAMJLogger guilogger,
	                  final SAMJLogger networkLogger) {
		// TODO super(new JFrame(), "SAMJ Annotator");
		if (guilogger == null) {
			this.GUIsOwnLog = new SAMJLogger () {
				@Override
				public void info(String text) {}
				@Override
				public void warn(String text) {}
				@Override
				public void error(String text) {}
			};
		} else {
			this.GUIsOwnLog = guilogger;
		}
		if (networkLogger == null) {
			this.logForNetworks = new SAMJLogger () {
				@Override
				public void info(String text) {}
				@Override
				public void warn(String text) {}
				@Override
				public void error(String text) {}
			};
		} else {
			this.logForNetworks = networkLogger;
		}
		this.consumerMethods = consumerMethods;

		panelModel = new SAMModelPanel(availableModel, (boolean bol) -> this.updateInterface(bol));
		// Buttons
		JPanel pnButtons = new JPanel(new FlowLayout());
		pnButtons.add(bnRect);
		pnButtons.add(bnPoints);
		pnButtons.add(bnBrush);
		pnButtons.add(bnMask);
		
		// Status
		JToolBar pnStatus = new JToolBar();
		pnStatus.setFloatable(false);
		pnStatus.setLayout(new BorderLayout());
		pnStatus.add(bnHelp, BorderLayout.EAST);
		txtStatus.setEditable(false);
		pnStatus.add(txtStatus, BorderLayout.CENTER);
		pnStatus.add(bnClose, BorderLayout.WEST);

		JPanel pnActions = new JPanel(new FlowLayout());
		pnActions.add(bnRoi2Mask);
		pnActions.add(bnComplete);
		pnActions.add(chkROIManager);
		
		List<ComboBoxItem> listImages = this.consumerMethods.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			cmbImage.addItem(item);
		cmbImage.addPopupMenuListener(this);
		
		GridBagLayout middleLayout = new GridBagLayout();
		//middleLayout.columnWidths = new int[] {1, 5};
		middleLayout.columnWeights = new double[] {1.0, 10.0};
		middleLayout.rowHeights = new int[] {1};
		JPanel panelImage = new JPanel(middleLayout);
		panelImage.setBorder(BorderFactory.createEtchedBorder());
		
		GridBagConstraints gbc0 = new GridBagConstraints();
		gbc0.fill = GridBagConstraints.HORIZONTAL;
		gbc0.gridx = 0;
		gbc0.insets = new Insets(5, 5, 5, 0);
		panelImage.add(bnStart, gbc0);
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		gbc1.gridx = 1;
		gbc1.insets = new Insets(5, 5, 5, 6);
		panelImage.add(cmbImage, gbc1);
		cmbImage.setPreferredSize(new Dimension(panelModel.getWidth(), 25));
		
		GridPanel pn = new GridPanel();
		pn.place(1, 1, panelModel);
		pn.place(2, 1, panelImage);
		pn.place(3, 1, pnButtons);
		pn.place(4, 1, pnActions);
		
		setLayout(new BorderLayout());
		add(pn, BorderLayout.NORTH);		
		add(pnStatus, BorderLayout.SOUTH);		

		// TODO not ready yet bnRoi2Mask.addActionListener(this);		
		// TODO not ready yet bnComplete.addActionListener(this);
		bnRoi2Mask.setEnabled(false);
		bnComplete.setEnabled(false);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		chkROIManager.addActionListener(this);
		
		bnStart.addActionListener(this);
		bnStop.addActionListener(this);
		bnRect.addActionListener(this);
		bnPoints.addActionListener(this);
		bnBrush.addActionListener(this);
		bnMask.addActionListener(this);
		bnMask.setDropTarget(new LocalDropTarget());
		
		add(pn);
		updateInterface();
	}
	
	public void setPromptsProvider(PromptsFunctionalInterface pp) {
		this.displayInterface = pp;
	}
	
	public SamEnvManager getModelInstallationManager() {
		return this.panelModel.getInstallationManager();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == bnRect && !bnRect.isSelected()) {
			display.switchToUsingRectangles();
			bnRect.setPressed(true); bnPoints.setPressed(false); bnBrush.setPressed(false);
		} else if (e.getSource() == bnPoints && !bnPoints.isSelected()) {
			display.switchToUsingPoints();
			bnRect.setPressed(false); bnPoints.setPressed(true); bnBrush.setPressed(false);
		} else if (e.getSource() == bnBrush && !bnBrush.isSelected()) {
			display.switchToUsingLines();
			bnRect.setPressed(false); bnPoints.setPressed(false); bnBrush.setPressed(true);
		} else if (e.getSource() == bnRect || e.getSource() == bnPoints || e.getSource() == bnBrush) {
			display.switchToNone();
			bnRect.setPressed(false); bnPoints.setPressed(false); bnBrush.setPressed(false);
		} else if (e.getSource() == bnHelp) {
			Tools.help();
		} else if (e.getSource() == bnClose) {
			this.close();
		} else if (e.getSource() == bnComplete) {
			GUIsOwnLog.warn("TO DO call Auto-complete");
		} else if (e.getSource() == bnStart) {
			if (!panelModel.getSelectedModel().isInstalled())
				GUIsOwnLog.warn("Not starting encoding as the selected model is not installed.");

			GUIsOwnLog.warn("Start the encoding");
			if (display == null || !display.getFocusedImage().equals(((ComboBoxItem) this.cmbImage.getSelectedItem()).getValue()))
				display = displayInterface.getPrompts(((ComboBoxItem) this.cmbImage.getSelectedItem()).getValue());
			SAMModel selecetdSAMModel = this.panelModel.getSelectedModel();
			SAMModel netAdapter = panelModel
					.getSelectedModel()
					.instantiate(display.giveProcessedSubImage(selecetdSAMModel), logForNetworks);
			if (netAdapter == null) return;
			display.switchToThisNet(netAdapter);
			GUIsOwnLog.warn("Finished the encoding");
			//TODO: encoding should be a property of a model
			this.setEncodingsDone(true);
		} else if (e.getSource() == chkROIManager) {
			if (display != null)
				display.enableAddingToRoiManager(chkROIManager.isSelected());
		} else if (e.getSource() == this.bnMask) {
			JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                this.display.improveExistingMask(fileChooser.getSelectedFile());
            }
		}

		updateInterface();
	}

	public void updateInterface() {
		updateInterface(false);
	}

	public void updateInterface(boolean deleteEncodings) {
		if (deleteEncodings) this.setEncodingsDone(false);
		if (!this.panelModel.isSelectedModelInstalled()) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(false);
			setEncodingsDone(false);
		} else if (this.panelModel.isSelectedModelInstalled()
				&& !this.encodingsDone
				&& this.cmbImage.getSelectedItem() != null 
				&& ((ComboBoxItem) this.cmbImage.getSelectedItem()).getId() != -1) {
			this.bnStart.setEnabled(true);
			this.cmbImage.setEnabled(true);
			this.chkROIManager.setEnabled(true);
		} else if (this.panelModel.isSelectedModelInstalled()
				&& this.cmbImage.getSelectedItem() != null 
				&& ((ComboBoxItem) this.cmbImage.getSelectedItem()).getId() != -1) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(true);
		} else if (this.panelModel.isSelectedModelInstalled()) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(true);
		}
		// TODO not ready yet bnComplete.setEnabled(this.encodingsDone);
		// TODO not ready yet bnRoi2Mask.setEnabled(this.encodingsDone);
		chkROIManager.setEnabled(this.encodingsDone);
		bnRect.setEnabled(this.encodingsDone);
		bnPoints.setEnabled(this.encodingsDone);
		bnBrush.setEnabled(this.encodingsDone);
		bnMask.setEnabled(this.encodingsDone);
		if (!encodingsDone) {
			this.bnRect.setPressed(false);
			this.bnPoints.setPressed(false);
			this.bnBrush.setPressed(false);
			this.bnMask.setPressed(false);
		}
	}
	
	private void setEncodingsDone(boolean isDone) {
		this.encodingsDone = isDone;
		if (!isDone) {
			if (display != null) {
				display.notifyNetToClose();
				display.switchToNone();
			}
			this.bnRect.setPressed(false);
			this.bnPoints.setPressed(false);
			this.bnBrush.setPressed(false);
			this.bnMask.setPressed(false);
		}
	}

	public class LocalDropTarget extends DropTarget {

		private static final long serialVersionUID = 286813958463411816L;

		@Override
		public void drop(DropTargetDropEvent e) {
			e.acceptDrop(DnDConstants.ACTION_COPY);
			e.getTransferable().getTransferDataFlavors();
			Transferable transferable = e.getTransferable();
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (DataFlavor flavor : flavors) {
				if (flavor.isFlavorJavaFileListType()) {
					try {
						List<File> files = (List<File>) transferable.getTransferData(flavor);
						for (File file : files) {
							GUIsOwnLog.info("Taking mask from file " + file.getAbsolutePath());
							display.improveExistingMask(file);
						}
					}
					catch (UnsupportedFlavorException ex) {
						ex.printStackTrace();
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			e.dropComplete(true);
			super.drop(e);
		}
	}
	
	public void close() {
		if (display != null)
			display.notifyNetToClose();
		if (SwingUtilities.windowForComponent(this).isDisplayable())
			SwingUtilities.windowForComponent(this).dispose();
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		Object item = this.cmbImage.getSelectedItem();
        List<ComboBoxItem> openSeqs = consumerMethods.getListOfOpenImages();
        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
        this.cmbImage.setModel(comboBoxModel);
        if (item != null) 
        	this.cmbImage.setSelectedIndex(
        			IntStream.range(0, objects.length).filter(i -> objects[i].getId() == ((ComboBoxItem) item).getId()).findFirst().orElse(0)
        			);
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		ComboBoxItem item = (ComboBoxItem) this.cmbImage.getSelectedItem();
		if ( item == null || (item != null && item.getId() == -1) ) {
			setEncodingsDone(false);
			selectedID = null;
		} else if (selectedID == null || (selectedID != null && selectedID != item.getId())) {
			setEncodingsDone(false);
        	selectedID = item.getId();
		}
    	this.updateInterface();
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
	}
}
