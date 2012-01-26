package org.q2.qtransfer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

class Preferences extends JDialog {
    private boolean edited;
    private final QTransferGUI settings;
    private JTextField textField;
    private JSpinner spinner;
    private JSpinner spinner2;

    public Preferences(QTransferGUI parent) {
		super(parent, true);
		setTitle("Settings");
		edited = false;
		this.settings = parent;
		initGUI();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void initGUI() {
		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent e) {
			}

			public void windowClosed(WindowEvent e) {
		    onClose();
			}

			public void windowClosing(WindowEvent e) {
			}

			public void windowDeactivated(WindowEvent e) {
			}

			public void windowDeiconified(WindowEvent e) {
			}

			public void windowIconified(WindowEvent e) {
			}

			public void windowOpened(WindowEvent e) {
			}
		    });

		JPanel panel = new JPanel(new BorderLayout());
		panel.setLayout(new GridBagLayout());
		panel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
		getContentPane().add(BorderLayout.CENTER, panel);

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.WEST;

		EmptyBorder border = new EmptyBorder(new Insets(0, 0, 0, 10));

		JLabel name = new JLabel("Name");
		name.setBorder(border);
		name.setToolTipText("Name of this device");
		panel.add(name, c);
	
		// column 1
		c.gridx = 1;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;

		textField = new JTextField();
		textField.setPreferredSize(new Dimension(100, 20));
		textField.setText(settings.getName());
		panel.add(textField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.0;

		JLabel segmentSize = new JLabel("Segment size");
		segmentSize.setToolTipText("Maximum size of a segment");
		segmentSize.setBorder(border);
		panel.add(segmentSize, c);

		c.gridx = 1;
		c.weightx = 1.0;

		spinner = new JSpinner(new SpinnerNumberModel(settings.getSegmentSize(), 32, 256, 1));
		spinner.setPreferredSize(new Dimension(100, 20));
		panel.add(spinner, c);

		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.0;

		JLabel timeout = new JLabel("Timeout (ms)");
		timeout.setToolTipText("Time to wait when a an active transfer gets interrupted");
		timeout.setBorder(border);
		panel.add(timeout, c);

		c.gridx = 1;
		c.weightx = 1.0;
	
		spinner2 = new JSpinner(new SpinnerNumberModel(settings.getTimeout(), 30000, 60000, 100));
		panel.add(spinner2, c);

		// finalize
		pack();

		setLocationRelativeTo(getParent());

    }

    private void onClose() {
		settings.setName(textField.getText());
		settings.setSegmentSize((Integer)spinner.getValue());
		settings.setTimeout((Integer)spinner2.getValue());
    }
}
