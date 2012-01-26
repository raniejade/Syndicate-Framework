package org.q2.qtransfer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import java.io.*;
import java.util.*;

import org.q2.syndicate.*;

import java.nio.*;

class TransferDialog extends JDialog implements PropertyChangeListener, ActionListener {
    public static final int NO_SUCH_DEVICE = 0x0;
    public static final int TRANSFER_DECLINED = 0x1;
    public static final int TRANSFER_INCOMPLETE = 0x2;
    public static final int TRANSFER_COMPLETE = 0x3;

    private JButton cancel, ok;
    private JProgressBar progress;

    private byte[] data;
    private final String name;
    private final int size;
    private final int segmentCount;
    private Segment[] segments;
    private int[] csegment;
    private int current;

    private final QTransferGUI handle;
    private final String destination;

    private int segmentSent = 0;

    private long completionTime = 0;
    private long startTime = 0;

    private final Task task;

    public TransferDialog(JFrame owner, byte[] data, String name, int size, int segmentCount, Segment[] segments, String destination) {
        super(owner, true);
        this.data = data;
        this.name = name;
        this.size = size;
        this.segmentCount = segmentCount;
        this.segments = segments;
        this.destination = destination;
        this.csegment = new int[segments.length];
        for(int i = 0; i < segments.length; i++)
            csegment[i] = 0;
        handle = (QTransferGUI)owner;
        current = 0;
        //setUndecorated(true);
        setTitle("Transfer[Sender]");
        initGUI();
        
        setLocation(owner.getX() + owner.getWidth() / 2 - getWidth() / 2, owner.getY() + owner.getHeight() / 2 - getHeight() / 2) ;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

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


    private class Task extends SwingWorker<Void, Void> {
	    // send request for transfer
	final int SEND_REQUEST = 0x0;
	    // wait for reply (YES/NO)
	final int WAIT_REPLY = 0x1;
	    
	// send current segment
	final int TRANSFER = 0x2;
	// wait for ack
	final int TRANSFER_WAIT_REPLY = 0x3;
	
	final int TIMEOUT_REACHED = 0x4;
	
	private volatile int errorCode = -1;
	
	public Void doInBackground() {
	    
	    int currentState = SEND_REQUEST;
	    
	    SCC scc = SCC.getInstance();
	    Connection con = scc.openConnection(destination);
	    boolean first = true;
        errorCode = TRANSFER_COMPLETE;
	    
	    long time = 0;
	    int dataSent = 0;
        while(current < segmentCount) {
            if(currentState == SEND_REQUEST) {
                try {
                    con.send(data);
                    first = true;
                    progress.setIndeterminate(true);
                    currentState = WAIT_REPLY;
                } catch (DestinationUnreachableException e) {
                    errorCode = NO_SUCH_DEVICE;
                    break;
                }
            } else if(currentState == WAIT_REPLY) {
                if(first) {
                    first = false;
                    time = System.currentTimeMillis();
                }

                SCC.Data rec = scc.receive();

                if(rec != null) {
                    if(rec.data[0] == MessageDispatcher.TRANSFER_ACK) {
                        first = true;
                        //time = System.currentTimeMillis();
                        currentState = TRANSFER;
                        progress.setIndeterminate(false);
                        startTime = System.currentTimeMillis();
                    } else if(rec.data[0] == MessageDispatcher.TRANSFER_NACK) {
                        errorCode = TRANSFER_DECLINED;
                        break;
                    }
                }

                /*if(System.currentTimeMillis() - time >= handle.getTimeout()) {
                    errorCode = TIMEOUT_REACHED;
                    break;
                }*/
            } else if(currentState == TRANSFER) {
                if(first) {
                    first = false;
                    time = System.currentTimeMillis();
                }

                SCC.Data rec = scc.receive();
                if(rec != null) {
                    if(rec.data[0] == MessageDispatcher.TRANSFER_SEGMENT_REPLY) {
                        ByteBuffer buffer = ByteBuffer.wrap(rec.data);
                        // type
                        buffer.get();

                        current = buffer.getInt();
                        if(current == segmentCount)
                            break;
                        
                        Segment c = segments[current];

                        ByteBuffer buff = ByteBuffer.allocate(9 + c.size);
                        buff.put(MessageDispatcher.TRANSFER_SEGMENT);
                        buff.putInt(c.size);
                        buff.putInt(current);
                        buff.put(c.data);

                        try {
                            con.send(buff.array());
                            segmentSent++;
                            csegment[current]++;
                            float progress = ((float)current / segmentCount) * 100;
                            setProgress((int)progress);
                            first = true;
                            continue;
                            
                        } catch (DestinationUnreachableException e) {
                        }
                    }
                }

                /*if(System.currentTimeMillis() - time >= handle.getTimeout()) {
                    errorCode = TRANSFER_INCOMPLETE;
                    break;
                }*/
            }
        }
	    /*
	    while(current < segmentCount) {
		    if(currentState == SEND_REQUEST) {
		        try {
			    con.send(data);
			    currentState = WAIT_REPLY;
		    	first = true;
		    	//progress.setString("Waiting for reply");
		    	System.out.println("Request sent.. waiting for reply");
		    	progress.setIndeterminate(true);
		       } catch (DestinationUnreachableException e) {
		    	//setProgress(100);
		    	errorCode = NO_SUCH_DEVICE;
		    	break;
		       }
	    	} else if(currentState == WAIT_REPLY) {
		        if(first) {
		    	first = false;
		      	time = System.currentTimeMillis();
		      }
		    
		      SCC.Data rec = scc.receive();
		    
		       if(rec != null) {
		        if(rec.data[0] == MessageDispatcher.TRANSFER_ACK) {
			          first = true;
			          currentState = TRANSFER;
			          startTime = System.currentTimeMillis();
			    System.out.println("Transfer Acknowledge");
			    continue;
			} else if(rec.data[0] == MessageDispatcher.TRANSFER_NACK) {
			    //setProgress(100);
			    errorCode = TRANSFER_DECLINED;
			    break;
			}
		    }
		    
		    if(System.currentTimeMillis() - time >= 30000) {
			//setProgress(100);
			errorCode = TIMEOUT_REACHED;
			break;
		    }
		} else if(currentState == TRANSFER) {
		    if(first) {
			time = System.currentTimeMillis();
			first = false;
			progress.setIndeterminate(false);
		    }
		    
		    if(current == segmentCount) {
			//setProgress(100);
			errorCode = TRANSFER_COMPLETE;
			//System.out.println("Sent-> segment #: " + (current + 1));
			completionTime = System.currentTimeMillis();
			break;
			}
		    
		    try {
			Segment c = segments[current];
			ByteBuffer buffer = ByteBuffer.allocate(9 + c.size);
			buffer.put(MessageDispatcher.TRANSFER_SEGMENT);
			buffer.putInt(c.size);
			buffer.putInt(current);
			buffer.put(c.data);
			//System.out.println("Sending segment: #" + current);
			con.send(buffer.array());
			currentState = TRANSFER_WAIT_REPLY;
            csegment[current]++;
			first = true;
			segmentSent++;
			continue;
		    } catch (DestinationUnreachableException e) {
		    }
		    
		    if(System.currentTimeMillis() - time >= handle.getTimeout()) {
			//setProgress(100);
			errorCode = TRANSFER_INCOMPLETE;
			break;
		    }
		} else if(currentState == TRANSFER_WAIT_REPLY) {
		    if(first) {
			first = false;
			time = System.currentTimeMillis();
		    }
		    
		    SCC.Data reply = scc.receive();
		    if(reply != null) {
			if(reply.data[0] == MessageDispatcher.TRANSFER_SEGMENT_REPLY) {
			    ByteBuffer b = ByteBuffer.wrap(reply.data);
			    // read byte
			    b.get();
			    
			    int next = b.getInt();

			    //System.out.println("received segment request: #" + next);
			    
			    // next segment was requested
			    if(next > current) {
				dataSent += segments[current].size;
				current++;

				if(current >= segmentCount) {
				    System.out.println("awww");
				    errorCode = TRANSFER_COMPLETE;
				    completionTime = System.currentTimeMillis();
				    break;
				 }

				float percent = ((float)dataSent / size) * 100;
				setProgress((int) percent);


				//if(current == segmentCount) {
				//  errorCode = TRANSFER_COMPLETE;
				//   break;
				//}
			    } //else {
				// either the segment was lost
				// send again lost segment
				//first = true;
				//continue;
			    //}
                else {
                    //current = next;
                }
			    currentState = TRANSFER;
			    first = true;
			    continue;
			}
		    }
		    
		    if(System.currentTimeMillis() - time >= handle.getTimeout()) {
			//setProgress(100);
			//errorCode = TRANSFER_INCOMPLETE;
			//break;
                currentState = TRANSFER;
                first = true;
                continue;
		    }
		}
	    }*/
	    //setProgress(100);
        completionTime = System.currentTimeMillis();
        setProgress(100);
	    return null;
	}
	
	protected void done() {
	    if(errorCode == NO_SUCH_DEVICE) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    onTransferNoSuchDevice();
			}
		    });
	    } else if(errorCode == TRANSFER_DECLINED) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    onTransferDeclined();
			}
		    });
	    } else if(errorCode == TRANSFER_INCOMPLETE) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    onTransferIncomplete();
			}
		    });
	    } else if(errorCode == TIMEOUT_REACHED) {
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
        JOptionPane.showMessageDialog(this, "Transfer complete. Well done", "Done", JOptionPane.INFORMATION_MESSAGE);
        showStatistics(true);
        dispose();
    }

    public void onTransferTimeout() {
        JOptionPane.showMessageDialog(this, "Transfer timeout has been reached, transfer is cancelled", "Timeout Reached", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public void onTransferIncomplete() {
        JOptionPane.showMessageDialog(this, "Transfer incomplete, remote device disappeared", "Transfer Incomplete", JOptionPane.INFORMATION_MESSAGE);
        showStatistics(false);
        dispose();
    }

    public void onTransferDeclined() {
        JOptionPane.showMessageDialog(this, "Transfer declined by remote device", "Transfer Declined", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public void onTransferNoSuchDevice() {
        JOptionPane.showMessageDialog(this, "Device not found in the network", "No such device", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public void showStatistics(boolean complete) {
        int e = segmentSent - segmentCount;
        float percentError = ((float)e / segmentCount) * 100;
        float efficiency = 100.0f - percentError;
        if(complete) {
            long ms = completionTime - startTime;
            int minutes = (int)ms / 60000;
            int seconds = ((int)ms % 60000) / 1000;
            
            float speed = (float)(size / 1000) / ((minutes * 60) + seconds);
            
            //String res = String.format("Completion time: %d:%d Efficiency: %.1f%c Speed: %.2f kb/s", minutes, seconds, efficiency, '%' , speed);
            String res = String.format("Completion time: %d:%d Successful Transmission(s): %d Failed Transimission(s): %d Speed: %.2f kb/s", minutes, seconds, 
                segmentSent, e, speed);
            
            JOptionPane.showMessageDialog(this, res, "Results", JOptionPane.INFORMATION_MESSAGE);
            dumpInfo();
        } else {
            String res = String.format("Efficiency: %.2f", efficiency);
            
            JOptionPane.showMessageDialog(this, res, "Results", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void dumpInfo() {
        try {
            PrintWriter writer = new PrintWriter("dump.info");
        
            for(int i = 0; i < segmentCount; i++) {
                writer.println("Segment[" + (i+1) + "] Sent: " + csegment[i] + " Failure(s): " + (csegment[i]-1));
            }   
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
