package sc.fiji.samj.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import io.bioimage.samj.SamEnvManager;
import sc.fiji.samj.communication.model.SAMModel;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.components.GridPanel;
import sc.fiji.samj.gui.components.HTMLPane;

public class SAMModelPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 7623385356575804931L;

	private HTMLPane info = new HTMLPane(400, 70);
	
	private JButton bnInstall = new JButton("Install");
	private JButton bnUninstall = new JButton("Uninstall");
	
	private JProgressBar progressInstallation = new JProgressBar();

	private ArrayList<JRadioButton> rbModels = new ArrayList<JRadioButton>();
	private SAMModels models;
	private final SamEnvManager manager;
	
	public SAMModelPanel(SAMModels models) {
		super();
		manager = SamEnvManager.create();
		this.models = models;
		JToolBar pnToolbarModel = new JToolBar();
		pnToolbarModel.setFloatable(false);
		pnToolbarModel.setLayout(new GridLayout(1, 2));
		pnToolbarModel.add(bnInstall);
		pnToolbarModel.add(bnUninstall);
		
		ButtonGroup group = new ButtonGroup();
		for(SAMModel model : models) {
			JRadioButton rb = new JRadioButton(model.getName(), model.isInstalled());
			rbModels.add(rb);
			rb.addActionListener(this);
			group.add(rb);
		}
		rbModels.get(0).setSelected(true);
	
		JPanel pnManageModel = new JPanel(new BorderLayout());
		pnManageModel.add(pnToolbarModel, BorderLayout.NORTH);
		pnManageModel.add(progressInstallation, BorderLayout.SOUTH);
		pnManageModel.add(new JScrollPane(info), BorderLayout.CENTER);
		
		GridPanel pnModel = new GridPanel(true);
		int col = 1;
		for(JRadioButton rb : rbModels)
			pnModel.place(1, col++, 1, 1, rb);
		
		pnModel.place(2, 1, 5, 2, pnManageModel);
		
		add(pnModel);
		info.append("p", "Description of the model");
		info.append("p", "Link to source");
		bnInstall.addActionListener(this);
		bnUninstall.addActionListener(this);
		
		updateInterface();
	}
	
	private void updateInterface() {
		for(int i=0; i<rbModels.size(); i++) {
			if (!rbModels.get(i).isSelected()) continue;
			info.clear();
			info.append("p", models.get(i).getDescription());
			bnInstall.setEnabled(!models.get(i).isInstalled());
			bnUninstall.setEnabled(models.get(i).isInstalled());
		}
	}
	
	public SAMModel getSelectedModel() {
		for(int i=0; i<rbModels.size(); i++) {
			if (rbModels.get(i).isSelected())
				return models.get(i);
		}
		return null;
	}
	
	public SamEnvManager getInstallationManager() {
		return this.manager;
	}
	
	public boolean isInstallationEnabled() {
		return this.bnInstall.isEnabled();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == bnInstall) {
			Thread currentThread = Thread.currentThread();
			Thread installThread = new Thread(() -> {
				try {
					SwingUtilities.invokeLater(() -> installationInProcess(true));
					//this.manager.installEfficientSAMSmall();
					this.progressInstallation.setValue(100);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				SwingUtilities.invokeLater(() -> installationInProcess(false));
			});
			installThread.start();
			
			Thread controlThread = new Thread(() -> {
				while (currentThread.isAlive() && installThread.isAlive()) {
					try{ Thread.sleep(50); } catch(Exception ex) {}
				}
				if (!currentThread.isAlive()) installThread.interrupt();
			});
			controlThread.start();
		} else if (e.getSource() == bnUninstall) {
			//TODO IJ.log("TODO: call the uninstallation of ");
		}
		
		updateInterface();
	}
	
	private void installationInProcess(boolean install) {
		this.bnUninstall.setEnabled(!install);
		this.bnInstall.setEnabled(!install);
		this.rbModels.stream().forEach(btn -> btn.setEnabled(!install));
		this.progressInstallation.setIndeterminate(install);
	}
}


