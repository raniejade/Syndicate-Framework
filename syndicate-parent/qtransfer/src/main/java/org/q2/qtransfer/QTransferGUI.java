package org.q2.qtransfer;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;
import java.nio.*;

public class QTransferGUI extends JFrame {
    private final JList list;
    private String name;
    private int segmentSize;
    private int timeout;

    private final JButton refresh;
    private final JMenu file, preferences, about;

    private final MessageDispatcher dispatcher;
    private volatile Vector<MessageDispatcher.Identity> identities;

    public QTransferGUI() {
	list = new JList();
	refresh = new JButton("Refresh");
	file = new JMenu("File");
	preferences = new JMenu("Preferences");
	about = new JMenu("About");
	identities = null;

	dispatcher = new MessageDispatcher(this);

	dispatcher.start();

	// initialize GUI
	initGUI();
	// center of screen
	setLocationRelativeTo(null);
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setTitle("QTransfer");
	setVisible(true);
	// load configuration
	loadConfigs();
	setResizable(false);
    }

    private void initGUI() {
	initMenuBar();
	initBody();

	addWindowListener(new WindowListener() {
		public void windowActivated(WindowEvent e) {
		}
		
		public void windowClosed(WindowEvent e) {
		    QTransferGUI.this.saveConfigs();
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

	final JPopupMenu popup = new JPopupMenu();
	JMenuItem transfer = new JMenuItem("Request Transfer");
	popup.add(transfer);

	transfer.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser fs = new JFileChooser();
		    int returnVal = fs.showOpenDialog(QTransferGUI.this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
			try {
			    File file = fs.getSelectedFile();
			    FileInputStream is = new FileInputStream(file);
			    int size = is.available();
			    int segmentCount = (int)Math.ceil((float)(size / getSegmentSize()));
			    String name = file.getName();
			
			    System.out.println("Name: " + name);
			    System.out.println("Size: " + size);
			    System.out.println("# of Segments: " + segmentCount);

			    Segment[] segments = new Segment[segmentCount];
			    int i = 0;
			    int segmentSize = getSegmentSize();

			    int ssize = size;

			    while(i < segmentCount) {
				if(size > segmentSize) {
				    byte[] tmp = new byte[segmentSize];
				    is.read(tmp);
				    segments[i] = new Segment(tmp);
				    size -= segmentSize;
				} else if (size > 0) {
				    // what is left
				    byte[] tmp = new byte[size];
				    is.read(tmp);
				    segments[i] = new Segment(tmp);
				    size = 0;
				}
				//System.out.println("size: " + size);
				i++;
			    }

			    System.out.println("Segments created, preparing to transfer");
			    ByteBuffer buffer = ByteBuffer.allocate(9 + name.getBytes().length);
			    buffer.put(MessageDispatcher.TRANSFER_REQUEST);
			    buffer.putInt(size);
			    buffer.putInt(segmentCount);
			    buffer.put(name.getBytes());
			    
			    TransferDialog td = new TransferDialog(QTransferGUI.this, buffer.array(), name, ssize, segmentCount, segments);
			    td.show();

			    // close stream
			    is.close();
			} catch (FileNotFoundException et) {
			    et.printStackTrace(); 
			} catch (IOException et2) {
			    et2.printStackTrace();
			}
		    }
		}
	    });

	list.addMouseListener(new MouseAdapter() {
		public void mousePressed(MouseEvent e) {
		    //System.out.println("i was called");
		    if(e.getButton() ==  MouseEvent.BUTTON3) {
			int idx = list.locationToIndex(e.getPoint());
			list.setSelectedIndex(idx);
			if(!list.isSelectionEmpty()) {
			    popup.show(list, e.getX(), e.getY());
			}
			//System.out.println("i am here now");
		    }
		}
	    });
    }

    private void loadConfigs() {
	try {
	    Scanner reader = new Scanner(new File("qtransfer.config"));
	    String name = reader.nextLine();
	    int segmentSize = reader.nextInt();
	    int timeout = reader.nextInt();
	    setName(name);
	    setSegmentSize(segmentSize);
	    setTimeout(timeout);

	} catch (FileNotFoundException e) {
	    setName("QTransfer");
	    setSegmentSize(256);
	    setTimeout(10000);
	}
    }

    private void saveConfigs() {
	try {
	    PrintWriter writer = new PrintWriter("qtransfer.config");
	    writer.println(name);
	    writer.println(segmentSize);
	    writer.println(timeout);
	    writer.close();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
    }

    private void initMenuBar() {
	JMenuBar mb = new JMenuBar();

	//JMenu file, preferences, about;

	JMenuItem exit, settings, aboutSyndicate, aboutQTransfer;

	//file = new JMenu("File");
	//preferences = new JMenu("Preferences");
	//about = new JMenu("About");

	exit = new JMenuItem("Exit");
	exit.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    QTransferGUI.this.dispose();
		}
	    });

	settings = new JMenuItem("Settings");
	settings.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Preferences prefs = new Preferences(QTransferGUI.this);
		    prefs.show();
		}
	    });
	
	aboutSyndicate = new JMenuItem("Syndicate");
	aboutQTransfer = new JMenuItem("QTransfer");

	file.add(exit);
	preferences.add(settings);
	about.add(aboutSyndicate);
	about.add(aboutQTransfer);

	mb.add(file);
	mb.add(preferences);
	mb.add(about);

	setJMenuBar(mb);
    }

    private void initBody() {
	JPanel panel = new JPanel(new BorderLayout());
	panel.setLayout(new GridBagLayout());
	panel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
	getContentPane().add(BorderLayout.CENTER, panel);

	GridBagConstraints c = new GridBagConstraints();
	c.insets = new Insets(2, 2, 2, 2);
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 1.0;
	c.weighty = 1.0;
	c.gridwidth = 2;
	list.setPreferredSize(new Dimension(250, 300));

	JLabel label = new JLabel("Devices");
	label.setPreferredSize(new Dimension(250, 17));
	label.setHorizontalAlignment(SwingConstants.CENTER);

	panel.add(label, c);

	c.gridy = 1;

	panel.add(list, c);

	refresh.setPreferredSize(new Dimension(250, 25));
	c.gridy = 2;
	c.weightx = 1.0;
	c.weighty = 0.0;
	panel.add(refresh, c);

	refresh.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    disableControls(false);
		    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		    WaitingThread task = new WaitingThread();
		    task.execute();
		    dispatcher.startRequestIdentity();
		}
	    });

	pack();
    }

    public void setName(String name) {
	this.name = name;
    }

    public void setSegmentSize(int size) {
	this.segmentSize = size;
    }

    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    public String getName() {
	return name;
    }

    public int getSegmentSize() {
	return segmentSize;
    }

    public int getTimeout() {
	return timeout;
    }

    private class WaitingThread extends SwingWorker<Void, Void> {
	public Void doInBackground() {
	    long current = System.currentTimeMillis();
	    while(System.currentTimeMillis() - current < getTimeout()) {
		//System.out.println((System.currentTimeMillis() - current) + " : " + getTimeout());
		//setProgress((int)(System.currentTimeMillis() - current));
	    }
	    return null;
	}

	public void done() {
	    setCursor(null);
	    disableControls(true);
	    identities = dispatcher.stopRequestIdentity();
	    updateList();
	}
    }

    private void updateList() {
	if(identities.size() > 0) {
	    String[] data = new String[identities.size()];
	    int i = 0;
	    for(MessageDispatcher.Identity id : identities) {
		data[i] = id.name + "(" + id.address + ")";
		i++;
	    }

	    list.setListData(data);
	}
    }

    private void disableControls(boolean t) {
	refresh.setEnabled(t);
	list.setEnabled(t);
	file.setEnabled(t);
	preferences.setEnabled(t);
	about.setEnabled(t);
    }

    public static void main(String[] args) {
	new QTransferGUI();
    }
}