package sc.fiji.samj.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import io.bioimage.samj.SamEnvManager;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;
import sc.fiji.samj.communication.PromptsToNetAdapter;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.components.ComboBoxItem;
import sc.fiji.samj.gui.components.GridPanel;
import sc.fiji.samj.gui.icons.ButtonIcon;
import sc.fiji.samj.gui.tools.Tools;
import sc.fiji.samj.ui.ExternalMethodsInterface;
import sc.fiji.samj.ui.PromptsResultsDisplay;
import sc.fiji.samj.ui.SAMJLogger;

public class SAMJDialog extends JPanel implements ActionListener, PopupMenuListener {

	private static final long serialVersionUID = -4362794696325316195L;
	
	private JButton bnClose = new JButton("Close");
	private JButton bnHelp = new JButton("Help");
	private JButton bnStart = new JButton("Start/Encode");
	private JButton bnStop = new JButton("Stop");
	private JButton bnComplete = new JButton("Auto-Complete");
	private JButton bnRoi2Mask = new JButton("Create Mask");
	private JTextField txtStatus = new JTextField("(c) SAMJ team 2024");

	private RandomAccessibleInterval<?> mask;
	
	private ButtonIcon bnRect = new ButtonIcon("Rect", "rect.png");
	private ButtonIcon bnPoints = new ButtonIcon("Points", "edit.png");
	private ButtonIcon bnBrush = new ButtonIcon("Brush", "github.png");
	private ButtonIcon bnMask = new ButtonIcon("Mask", "help.png");
	private JCheckBox chkROIManager = new JCheckBox("Add to ROI Manager", true);

	private JComboBox<ComboBoxItem> cmbImage = new JComboBox<ComboBoxItem>();
	
	private final SAMModelPanel panelModel;
	private final ExternalMethodsInterface softwareMethods;
	private final SAMJLogger GUIsOwnLog;
	private final SAMJLogger logForNetworks;
	
	private Integer selectedID = null;

	private boolean encodingDone = false;
	
	public interface PromptsFunctionalInterface { PromptsResultsDisplay getPrompts(Object image); }
	private PromptsFunctionalInterface displayInterface;
	private PromptsResultsDisplay display;

	public SAMJDialog(final SAMModels availableModel,
	                  final ExternalMethodsInterface softwareMethods) {
		this(availableModel, softwareMethods, null, null);
	}

	public SAMJDialog(final SAMModels availableModel,
	                  final ExternalMethodsInterface softwareMethods,
	                  final SAMJLogger guilogger) {
		this(availableModel, softwareMethods, guilogger, null);
	}

	public SAMJDialog(final SAMModels availableModel,
	                  final ExternalMethodsInterface softwareMethods,
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
		this.softwareMethods = softwareMethods;

		panelModel = new SAMModelPanel(availableModel, () -> this.updateInterface());
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
		
		List<ComboBoxItem> listImages = this.softwareMethods.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			cmbImage.addItem(item);
		cmbImage.addPopupMenuListener(this);
	
		GridPanel panelImage = new GridPanel(true);
		panelImage.place(1, 1, 1, 1, bnStart);
		panelImage.place(1, 2, 1, 1, cmbImage);
		
		GridPanel pn = new GridPanel();
		pn.place(1, 1, panelModel);
		pn.place(2, 1, panelImage);
		pn.place(3, 1, pnButtons);
		pn.place(4, 1, pnActions);
		
		setLayout(new BorderLayout());
		add(pn, BorderLayout.NORTH);		
		add(pnStatus, BorderLayout.SOUTH);		

		bnRoi2Mask.addActionListener(this);		
		bnComplete.addActionListener(this);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		chkROIManager.addActionListener(this);
		
		bnStart.addActionListener(this);
		bnStop.addActionListener(this);
		bnRect.addActionListener(this);
		bnPoints.addActionListener(this);
		bnBrush.addActionListener(this);
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

		if (e.getSource() == bnRect) {
			display.switchToUsingRectangles();
		}
		if (e.getSource() == bnPoints) {
			display.switchToUsingPoints();
		}
		if (e.getSource() == bnBrush) {
			display.switchToUsingLines();
		}

		if (e.getSource() == bnHelp) {
			Tools.help();
		}
		
		if (e.getSource() == bnClose) {
			display.notifyNetToClose();
		}
		
		if (e.getSource() == bnComplete) {
			GUIsOwnLog.warn("TO DO call Auto-complete");
		}

		if (e.getSource() == bnStart) {
			if (!panelModel.getSelectedModel().isInstalled())
				GUIsOwnLog.warn("Not starting encoding as the selected model is not installed.");

			GUIsOwnLog.warn("TO DO Start the encoding");
			display = displayInterface.getPrompts(((ComboBoxItem) this.cmbImage.getSelectedItem()).getValue());
			PromptsToNetAdapter netAdapter = panelModel
					.getSelectedModel()
					.instantiate(display.giveProcessedSubImage(), logForNetworks);
			//TODO: if this netAdapter has already encoded, we don't do it again
			display.switchToThisNet(netAdapter);
			GUIsOwnLog.warn("TO DO End the encoding");
			//TODO: encoding should be a property of a model
			encodingDone = true;
		}

		if (e.getSource() == chkROIManager) {
			if (display != null)
				display.enableAddingToRoiManager(chkROIManager.isSelected());
		}

		updateInterface();
	}

	public void updateInterface() {
		if (!this.panelModel.isSelectedModelInstalled()) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(false);
			bnComplete.setEnabled(false);
			bnRoi2Mask.setEnabled(false);
			this.chkROIManager.setEnabled(false);
			encodingDone = false;
		} else if (this.panelModel.isSelectedModelInstalled() 
				&& this.cmbImage.getSelectedItem() != null 
				&& ((ComboBoxItem) this.cmbImage.getSelectedItem()).getId() != -1) {
			this.bnStart.setEnabled(true);
			this.cmbImage.setEnabled(true);
			bnComplete.setEnabled(true);
			bnRoi2Mask.setEnabled(true);
			this.chkROIManager.setEnabled(true);
		} else if (this.panelModel.isSelectedModelInstalled()) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(true);
			bnComplete.setEnabled(false);
			bnRoi2Mask.setEnabled(false);
			this.chkROIManager.setEnabled(false);
		}
		bnRect.setEnabled(encodingDone);
		bnPoints.setEnabled(encodingDone);
		bnBrush.setEnabled(encodingDone);
		bnMask.setEnabled(encodingDone);
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
							mask = SAMJDialog.this.softwareMethods.getImageMask(file);
							if (mask != null) {
								return;
							}
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
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        List<ComboBoxItem> openSeqs = softwareMethods.getListOfOpenImages();
        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
        this.cmbImage.setModel(comboBoxModel);
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		ComboBoxItem item = (ComboBoxItem) this.cmbImage.getSelectedItem();
		if (item == null) {
			selectedID = null;
			this.bnStart.setEnabled(false);
			return;
		}
        int newSelectedImageId = item.getId();
        if (newSelectedImageId == -1) {selectedID = newSelectedImageId; this.bnStart.setEnabled(false); return;}
        if (selectedID != null && selectedID == newSelectedImageId) return;
    	selectedID = newSelectedImageId;
    	this.updateInterface();
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {	
	}
}
