package org.q2.qtransfer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

class TransferDialog extends JDialog implements PropertyChangeListener, ActionListener {
    private JButton cancel, ok;
    private JProgressBar progress;

    private byte[] data;
    private final String name;
    private final int size;
    private final int segmentCount;
    private Segment[] segments;
    private int current;

    public TransferDialog(JFrame owner, byte[] data, String name, int size, int segmentCount, Segment[] segments) {
	super(owner, true);
	this.data = data;
	this.name = name;
	this.size = size;
	this.segmentCount = segmentCount;
	this.segments = segments;
	current = 0;
	//setUndecorated(true);
	setTitle("Transfer");
	initGUI();
	
	setLocation(owner.getX() + owner.getWidth() / 2 - getWidth() / 2, owner.getY() + owner.getHeight() / 2 - getHeight() / 2) ;
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setResizable(false);
    }

    private void initGUI() {
	JPanel panel = new JPanel(new BorderLayout());
	panel.setLayout(new GridBagLayout());
	panel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
	getContentPane().add(BorderLayout.CENTER, panel);

	GridBagConstraints c = new GridBagConstraints();

	progress = new JProgressBar();
	c.gridwidth = 2;
	c.insets = new Insets(2, 2, 2, 2);
	progress.setPreferredSize(new Dimension(250, 20));
	panel.add(progress, c);

	c.gridwidth = 1;
	
	JLabel label = new JLabel("Name");
	label.setToolTipText("Name of the file");
	label.setPreferredSize(new Dimension(50, 20));
	label.setHorizontalAlignment(SwingConstants.LEFT);
	c.gridy = 1;
	panel.add(label, c);

	JTextField namefield = new JTextField(name);
	namefield.setEditable(false);
	namefield.setToolTipText(name);
	namefield.setPreferredSize(new Dimension(150, 20));
	//namefield.setHorizontalAlignment(JTextField.RIGHT);
	c.gridx = 1;
	panel.add(namefield, c);

	JLabel label1 = new JLabel("Size");
	label1.setToolTipText("Size of the file (in megabytes)");
	label1.setPreferredSize(new Dimension(50, 20));
	label1.setHorizontalAlignment(SwingConstants.LEFT);
	c.gridy = 2;
	c.gridx = 0;
	panel.add(label1, c);

	float msize = (float)size / 1000000;

	JTextField sizefield = new JTextField(String.format("%.1f MB", msize));
	sizefield.setPreferredSize(new Dimension(150, 20));
	sizefield.setHorizontalAlignment(JTextField.RIGHT);
	sizefield.setEditable(false);
	c.gridx = 1;
	panel.add(sizefield, c);

	JLabel label3 = new JLabel("Segments");
	label3.setToolTipText("Number of segments to be transmitted");
	label3.setPreferredSize(new Dimension(50, 20));
	label3.setHorizontalAlignment(SwingConstants.LEFT);
	c.gridy = 3;
	c.gridx = 0;
	panel.add(label3, c);

	JTextField segmentfield = new JTextField(String.valueOf(segmentCount));
	segmentfield.setPreferredSize(new Dimension(150, 20));
	segmentfield.setHorizontalAlignment(JTextField.RIGHT);
	segmentfield.setEditable(false);
	c.gridx = 1;
	panel.add(segmentfield, c);

	c.weightx = 1.0;

	JPanel box = new JPanel();

	cancel = new JButton("Cancel");
	cancel.setPreferredSize(new Dimension(120, 20));
	c.gridy = 4;
	c.gridx = 0;
	c.gridwidth = 2;
	cancel.addActionListener(this);
	c.anchor = GridBagConstraints.CENTER;
	//c.fill = GridBagConstraints.BOTH;
	//cancel.addActionListener(this);
	//panel.add(cancel, c);
	box.add(cancel);

	ok = new JButton("Done");
	//c.gridx = 1;
	ok.setPreferredSize(new Dimension(120, 20));
	ok.addActionListener(this);
	ok.setEnabled(false);
	box.add(ok);
	//panel.add(ok, c);

	panel.add(box, c);

	pack();
    }

    public void actionPerformed(ActionEvent e) {
	//System.out.println("called");
	if(e.getSource() == cancel) {
	    //System.out.println("wew");
	    dispose();
	} else if(e.getSource() == ok) {
	}
    }

    public void propertyChange(PropertyChangeEvent e) {
    }
}