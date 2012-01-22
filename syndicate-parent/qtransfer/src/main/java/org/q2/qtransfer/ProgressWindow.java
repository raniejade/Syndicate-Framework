package org.q2.qtransfer;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;


class ProgressWindow extends JDialog implements ChangeListener{
    private Thread backdoor;
    private final int limit;
    
    public ProgressWindow(JFrame owner, int timeout) {
	super(owner, true);
	limit = timeout;
	initGUI();
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void initGUI() {
	final JProgressBar progress = new JProgressBar(0, limit);
	setUndecorated(true);
	progress.addChangeListener(this);

	progress.setStringPainted(true);
	//progress.setString("Identify request sent... Giving ample amount of time to receive replies");
	progress.setIndeterminate(false);

	getContentPane().setLayout(new GridBagLayout());

	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(2, 2, 2, 2);

	getContentPane().add(progress, c);

	JLabel label = new JLabel("Please wait...");

	c.gridy = 1;
	
	getContentPane().add(label, c);

	pack();

	int x = getParent().getWidth() / 2 - getWidth() / 2;
	int y = getParent().getHeight() / 2 - getHeight() / 2;
	setLocation(getParent().getX() + x, getParent().getY() + y);

	backdoor = new Thread(new Runnable(){
		public void run() {
		    final long current = System.currentTimeMillis();
		    while(System.currentTimeMillis() - current < limit) {
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
				    progress.setValue((int)(System.currentTimeMillis() - current));
				}
			    });
		    }
		}
	    });
	backdoor.setDaemon(true);
	backdoor.start();
    }

    public void stateChanged(ChangeEvent e) {
	JProgressBar progress = (JProgressBar)e.getSource();
	if(progress.getValue() >= limit) {
	    dispose();
	    try {
		backdoor.join();
	    } catch (InterruptedException et) {
		et.printStackTrace();
	    }
	}
    }
}