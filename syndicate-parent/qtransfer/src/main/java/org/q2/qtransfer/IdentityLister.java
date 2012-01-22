package org.q2.qtransfer;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

class IdentityLister extends JDialog {
    public final static int ACCEPTED = 0;
    public final static int REJECTED = 1;

    private int mode;

    private Vector<RequestHandler.Identity> idents;

    private IdentityLister(JFrame owner, Vector<RequestHandler.Identity> identities) {
	super(owner, true);
	idents = identities;
	initGUI();
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	setLocationRelativeTo(getParent());
	mode = REJECTED;
	setTitle("Choose device");
	setResizable(false);
    }

    private void initGUI() {
	String[] data = new String[idents.size()];
	int i = 0;
	for(RequestHandler.Identity id : idents) {
	    data[i] = new String(id.name + "(" + id.address + ")");
	    i++;
	}

	JList list = new JList(data);
	list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	list.setLayoutOrientation(JList.VERTICAL);
	list.setVisibleRowCount(-1);

	JScrollPane sp = new JScrollPane(list);
	sp.setPreferredSize(new Dimension(300, 250));

	JPanel panel = new JPanel(new BorderLayout());
	panel.setLayout(new GridBagLayout());
	panel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
	getContentPane().add(BorderLayout.CENTER, panel);

	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(2, 2, 2, 2);
	c.weightx = 1;
	c.weighty = 1;
	c.gridwidth = 3;
	c.gridheight = 2;

	panel.add(sp, c);

	JButton choose = new JButton("Choose");
	choose.setEnabled(false);
	JButton cancel = new JButton("Cancel");

	choose.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    mode = ACCEPTED;
		    IdentityLister.this.dispose();
		}
	    });
	cancel.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    mode = REJECTED;
		    IdentityLister.this.dispose();
		}
	    });

	c.gridwidth = 1;
	c.gridheight = 1;
	c.gridx = 1;
	c.gridy = 2;

	panel.add(cancel, c);

	c.gridx = 2;
	
	panel.add(choose, c);

	pack();
    }

    public static int showDialog(JFrame owner, Vector<RequestHandler.Identity> identities) {
	IdentityLister lister = new IdentityLister(owner, identities);
	lister.show();
	return lister.mode;
    }
}