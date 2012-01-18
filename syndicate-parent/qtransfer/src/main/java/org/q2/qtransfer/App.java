package org.q2.qtransfer;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;

public class App {
    public static void main( String[] args ) {
	SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    createAndShowGUI();
		}
	});
    }

    private static void createAndShowGUI() {
	if(!SystemTray.isSupported()) {
	    throw new RuntimeException("System tray not supported");
	}

	final PopupMenu popup = new PopupMenu();
	final TrayIcon trayIcon = new TrayIcon(createImage("/icon.png", "icon"));
	trayIcon.setImageAutoSize(true);
	final SystemTray sysTray = SystemTray.getSystemTray();

	MenuItem aboutMenuItem = new MenuItem("About");
	MenuItem transferMenuItem = new MenuItem("Transfer");
	MenuItem exitMenuItem = new MenuItem("Exit");

	popup.add(transferMenuItem);
	popup.add(aboutMenuItem);
	popup.add(exitMenuItem);

	trayIcon.setPopupMenu(popup);

	/*trayIcon.addMouseListener(new MouseListener() {
		public void mouseClicked(MouseEvent e) {
		    popup.show((Component)popup.getParent(), ((Component)popup.getParent()).getWidth(),((Component) popup.getParent()).getHeight());
		}
		
		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
		
		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}
	});*/

	try {
	    sysTray.add(trayIcon);
	} catch (AWTException e) {
	    throw new RuntimeException(e.getMessage());
	}

	exitMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    sysTray.remove(trayIcon);
		    System.exit(0);
		}
	});
    }

    //Obtain the image URL
    protected static Image createImage(String path, String description) {
        URL imageURL = App.class.getResource(path);
        
        if (imageURL == null) {
            System.err.println("Resource not found: " + path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }
}
