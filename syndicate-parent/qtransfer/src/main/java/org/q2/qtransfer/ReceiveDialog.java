package org.q2.qtransfer;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;


import org.q2.syndicate.*;

import java.nio.*;

import java.io.*;

class ReceiveDialog extends JDialog implements ActionListener, PropertyChangeListener {
    private final String name;
    private int size;
    private int segmentCount;

    private  File file;

    private JButton cancel, ok;
    private JProgressBar progress;

    private final QTransferGUI handle;

    private final String sender;

    private int current = 0;

    private int receivedData = 0;

    private Segment[] segments;

    private final Task task;

    public ReceiveDialog(QTransferGUI owner, byte[] data, String sender) {
	super(owner, true);
	this.file = file;
	ByteBuffer buffer = ByteBuffer.wrap(data);
	
	// read byte
	buffer.get();
	
	size = buffer.getInt();

	this.sender = sender;
	
	segmentCount = buffer.getInt();

	byte[] tmp = new byte[data.length - 9];

	buffer.get(tmp);
	file = null;

	name = new String(tmp);

	this.handle = owner;

	setTitle("Transfer[Receiver]");
	initGUI();
	
	setLocation(owner.getX() + owner.getWidth() / 2 - getWidth() / 2, owner.getY() + owner.getHeight() / 2 - getHeight() / 2) ;
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	setResizable(false);

	segments = new Segment[segmentCount];

	task = new Task();
	task.addPropertyChangeListener(this);
	task.execute();
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

	progress.setStringPainted(true);

	pack();
    }

    public void actionPerformed(ActionEvent e) {
	//System.out.println("called");
	if(e.getSource() == cancel) {
	    //System.out.println("wew");
	    task.cancel(true);
	    dispose();
	} else if(e.getSource() == ok) {
	}
    }

    public void propertyChange(PropertyChangeEvent e) {
	if("progress".equals(e.getPropertyName())) {
	    int value = (Integer)e.getNewValue();
	    progress.setValue(value);
	}
    }

    private volatile int errorCode = -1;

    private final int TIMEOUT_REACHED = 0x0;
    private final int SAVE_FILE_CANCELED = 0x1;
    private final int TRANSFER_COMPLETE = 0x2;

    private class Task extends SwingWorker<Void, Void> {
	private final int SAVE_FILE_LOCATION = 0x0;
	private final int RECEIVE_SEGMENT = 0x1;
	public Void doInBackground() {
	    SCC scc = SCC.getInstance();
	    Connection con = scc.openConnection(sender);
	    int currentState = SAVE_FILE_LOCATION;

	    boolean first = true;

	    long time = 0;

	    while(current < segmentCount) {
		if(currentState == SAVE_FILE_LOCATION) {
		    JFileChooser fs = new JFileChooser();
		    fs.setSelectedFile(new File(name));
		    int returnVal = fs.showSaveDialog(ReceiveDialog.this);

		    if(returnVal == JFileChooser.APPROVE_OPTION) {
			file = fs.getSelectedFile();
			currentState = RECEIVE_SEGMENT;
		    } else {
			//setProgress(100);
			errorCode = SAVE_FILE_CANCELED; 
			break;
		    }
		} else if(currentState == RECEIVE_SEGMENT) {
		    if(first) {
			first = false;
			time = System.currentTimeMillis();
		    }

		    //if(current == segmentCount - 1) {
		    //	errorCode = TRANSFER_COMPLETE;
		    //	break;
		    //}

		    SCC.Data data = scc.receive();
		    if(data != null) {
			if(data.data[0] == MessageDispatcher.TRANSFER_SEGMENT) {
			    ByteBuffer buffer = ByteBuffer.wrap(data.data);
			    
			    // read byte
			    buffer.get();
			    
			    // size
			    int receivedSize = buffer.getInt();

			    int receivedSegment = buffer.getInt();

			    byte[] tmp = new byte[data.data.length - 9];
			    
			    buffer.get(tmp);

			    //System.out.println("received segment #" + receivedSegment);

			    boolean cont = false;
			    
			    // valid
			    if(receivedSegment == current) {
				segments[current] = new Segment(tmp);
				receivedData += receivedSize;
				current++;
				first = true;
				cont = true;
			    }
		    
			    System.out.println("Request segment: #" + current);

			    buffer = ByteBuffer.allocate(5);
			    buffer.put(MessageDispatcher.TRANSFER_SEGMENT_REPLY);
			    buffer.putInt(current);

			    try {
				con.send(buffer.array());
			    if(current >= segmentCount) {
				System.out.println("wew");
				errorCode = TRANSFER_COMPLETE;
				break;
			    }
			    
			    float progress = ((float)receivedData / size) * 100;
			    setProgress((int)progress);

			    if(cont)
				continue;
			    } catch (DestinationUnreachableException e) {
			    }


			}
		    }

		    if(System.currentTimeMillis() - time >= handle.getTimeout()) {
			//setProgress(100);
			errorCode = TIMEOUT_REACHED;
			break;
		    }
		}
	    }
	    setProgress(100);
	    return null;
	}

	protected void done() {
	    if(errorCode == TIMEOUT_REACHED) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    onTransferTimeout();
			}
		    });
	    } else if(errorCode == TRANSFER_COMPLETE) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    onTransferComplete();
			}
		    });
	    }
	}
    }

    public void onTransferComplete() {
	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	try {
	    FileOutputStream os = new FileOutputStream(file);
	    for(Segment c : segments) {
		if(c != null)
		    os.write(c.data);
	    }
	    os.close();
	    JOptionPane.showMessageDialog(this, "Transfer complete, file saved.", "Done", JOptionPane.INFORMATION_MESSAGE);
	} catch (FileNotFoundException e) {
	    JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	} catch (IOException e) {
	    JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}
	setCursor(null);
	dispose();
    }

    public void onTransferTimeout() {
	JOptionPane.showMessageDialog(this, "Transfer timeout reached, transfer cancelled", "Timeout Reached", JOptionPane.INFORMATION_MESSAGE);
	dispose();
    }
}