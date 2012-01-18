package org.q2.qtransfer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class QTransferGUI extends JFrame {
    public QTransferGUI() {
	setTitle("QTransfer");
	setSize(300, 350);
	setLocationRelativeTo(null);
	setDefaultCloseOperation(EXIT_ON_CLOSE);
	setupMenuBar();
    }

    private void setupMenuBar() {
	JMenuBar menuBar = new JMenuBar();

	JMenu file = new JMenu("File");
	JMenu about = new JMenu("About");
	JMenu preferences = new JMenu("Preferences");

	JMenuItem transfer = new JMenuItem("Transfer file");
	JMenuItem exit = new JMenuItem("Exit");

	exit.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
		    System.exit(0);
		}
	});

	JMenuItem qtransfer = new JMenuItem("QTransfer");
	JMenuItem syndicate = new JMenuItem("Syndicate");

	JMenuItem settings = new JMenuItem("Settings");

	file.add(transfer);
	file.add(exit);

	about.add(qtransfer);
	about.add(syndicate);

	preferences.add(settings);

	menuBar.add(file);
	menuBar.add(preferences);
	menuBar.add(about);

	setJMenuBar(menuBar);
    }
}