package org.q2.qtransfer;

import java.io.*;
import java.nio.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;

import java.util.*;

public class QTransferGUI extends JFrame {
    private RequestHandler handler;
    private JFileChooser fc;

    public QTransferGUI() {
	setTitle("QTransfer");
	setSize(300, 350);
	setLocationRelativeTo(null);
	setDefaultCloseOperation(EXIT_ON_CLOSE);
	setupMenuBar();
	setupPreferences();
	setupRequestHandler();
    }

    private SettingsManager settings;

    public String getName() {
	return settings.getName();
    }

    private void setupPreferences() {
	if(!SettingsManager.checkIfExists("qtransfer.config")) {
	    settings = SettingsManager.createDefault();
	} else {
	    settings = SettingsManager.load("qtransfer.config");
	}
    }

    private void setupRequestHandler() {
	handler = new RequestHandler(this);
	handler.start();
    }

    private void setupMenuBar() {
	JMenuBar menuBar = new JMenuBar();

	JMenu file = new JMenu("File");
	JMenu about = new JMenu("About");
	JMenu preferences = new JMenu("Preferences");

	JMenuItem transfer = new JMenuItem("Transfer file");
	JMenuItem exit = new JMenuItem("Exit");

	fc = new JFileChooser();

	transfer.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    int retVal = fc.showOpenDialog(QTransferGUI.this);
		    if(retVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			try {
			    FileInputStream fs = new FileInputStream(file);
			    long length = file.length();
			    byte[] data = new byte[(int)length];
			    fs.read(data);
			    fs.close();
			    // System.out.println("File size: " + length);
			    //System.out.println("Number of segments: " + Math.ceil((int)length / settings.getSegmentSize()));

			    int len = (int)length;
			    int segmentCount = (int)Math.ceil((int)length / settings.getSegmentSize());
			    String filename = file.getName();
			    ByteBuffer buffer = ByteBuffer.allocate(12 + filename.length());
			    buffer.putInt(len);
			    buffer.putInt(segmentCount);
			    buffer.putInt(filename.length());
			    buffer.put(filename.getBytes());

			    handler.startAcceptIdentityReply();
			    ProgressWindow pw = new ProgressWindow(QTransferGUI.this, (int)settings.getTimeout());
			    pw.show();
			    //System.out.println("wew");
			    Vector<RequestHandler.Identity> results = handler.stopAcceptIdentityReply();

			    int retCode = IdentityLister.showDialog(QTransferGUI.this, results);
			    
			    //while(!pw.isFinished());

			} catch (IOException et) {
			    et.printStackTrace();
			}
		    }
		}
	    });

	exit.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    QTransferGUI.this.dispose();
		}
	    });

	JMenuItem qtransfer = new JMenuItem("QTransfer");
	JMenuItem syndicate = new JMenuItem("Syndicate");

	JMenuItem settings = new JMenuItem("Settings");

	settings.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Preferences prefs = new Preferences(QTransferGUI.this, ((QTransferGUI)QTransferGUI.this).getSettingsManager());
		    prefs.show();
		}
	    });

	file.add(transfer);
	file.add(exit);

	about.add(qtransfer);
	about.add(syndicate);

	preferences.add(settings);

	menuBar.add(file);
	menuBar.add(preferences);
	menuBar.add(about);

	setJMenuBar(menuBar);

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
	
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void onClose() {
	try {
	    SettingsManager.save(settings, "qtransfer.config");
	} catch (java.io.FileNotFoundException e) {
	}
    }

    public SettingsManager getSettingsManager() {
	return settings;
    }
}