package antipasto.GUI.ImageListView;

import javax.swing.BoxLayout;

import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.io.*;

import antipasto.*;

import processing.app.Base;
import processing.app.Serial;
import processing.app.SerialException;
import processing.app.FlashTransfer;
import javax.swing.TransferHandler;

import javax.swing.*;
import java.awt.datatransfer.*;
import java.util.List;
import java.util.Iterator;
import java.net.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.math.*;

import java.awt.BorderLayout;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ImageListPanel extends JPanel implements ComponentListener {
	private ImageListView list;
	final FlashTransfer imageTransfer;
	Serial mySerial = null;
	private JButton removeButton;
	private JButton transferButton;
	private JProgressBar progressBar;
	boolean isTransfering = false;
	private JLabel infoLabel;
	private JLabel progressLabel;
	private long totalSize = 0;
	private long totalFileCount = 0;
	private JScrollPane scrollPane;

	//We'll use this static function to take control of the serial port when
	//we want to
	private static boolean killTransfer = false;

	public ImageListPanel(File imagePath){
		imageTransfer = new FlashTransfer();
		initSubPanels();
		setDirectory(imagePath);
		setVisible(true);
	}

	//This function is used to kill any active transfer that may be hogging
	//the serial port.
	public static void killActiveTransfer(){
		ImageListPanel.killTransfer = true;
		FlashTransfer.killTransfer();
	}

	private void initSubPanels(){
		this.createTransferButton();
		this.transferButton.setVisible(true);

		this.setLayout(new BorderLayout());

		this.createRemoveButton();

		/* The north panel */
		JPanel northPanel = new JPanel();
		northPanel.setBackground(new Color(0x04, 0x4F, 0x6F));
		northPanel.setOpaque(true);
		northPanel.setLayout(new BorderLayout());

		/* I'm the transfer buttons area */
		JPanel northButtonPanel = new JPanel();
		northButtonPanel.setLayout(new BorderLayout());
		northButtonPanel.setBackground(new Color(0x04, 0x4F, 0x6F));
		northButtonPanel.setOpaque(true);
		northButtonPanel.add(transferButton, BorderLayout.WEST);
		northButtonPanel.add(removeButton, BorderLayout.EAST);

		/* I'm the transfer label area */
		JPanel northLabelPanel = new JPanel();
		northLabelPanel.setBackground(new Color(0x04, 0x4F, 0x6F));
		northLabelPanel.setOpaque(true);

		JLabel infoLabel = new JLabel(" Drop files below. ");
		infoLabel.setForeground(Color.white);
		infoLabel.setBackground(new Color(0x04, 0x4F, 0x6F));
		infoLabel.setOpaque(true);
		northLabelPanel.add(infoLabel);


		/* Add everything that's North */
		northPanel.add(northButtonPanel,BorderLayout.EAST);
		northPanel.add(northLabelPanel, BorderLayout.WEST);
		this.add(northPanel, BorderLayout.NORTH);

		/* The south label */
		JPanel southProgressPanel = new JPanel();
		southProgressPanel.setLayout(new BorderLayout());
		southProgressPanel.setBackground(new Color(0x04, 0x4f, 0x6f));
		southProgressPanel.setOpaque(true);

		/* A progress bar for the transfer */
		progressBar = new JProgressBar();
		progressBar.setVisible(false);

		/* A label to describe stuff */
		progressLabel = new JLabel(" ");
		progressLabel.setForeground(Color.WHITE);

		/* Add everything that's South */
		southProgressPanel.add(progressBar,BorderLayout.EAST);
		southProgressPanel.add(progressLabel, BorderLayout.WEST);

		this.add(southProgressPanel, BorderLayout.SOUTH);

		this.transferButton.setVisible(true);

		this.removeButton.setVisible(true);
		this.progressBar.setVisible(true);
		this.progressLabel.setVisible(true);



		this.setVisible(true);
	}

	public void setDirectory(File imagePath){
	     	/* Build the center */
	     	list = new ImageListView(imagePath);
	     	if(scrollPane != null){
	     		this.remove(scrollPane);
	     		scrollPane.setVisible(false);
	     		scrollPane = null;				//let's take out the trash!
	     	}
	     	scrollPane = new JScrollPane(list);
		this.add(scrollPane, BorderLayout.CENTER);
		scrollPane.setSize(this.getWidth(), this.getHeight() - transferButton.getHeight());
		scrollPane.setVisible(true);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		list.setVisible(true);
		this.transferButton.setVisible(true);
	  }

	public void paint(java.awt.Graphics g){

		totalFileCount = list.getModel().getSize();
		progressLabel.setText(" Files: " + totalFileCount);

		super.paint(g);
	}

	private JButton createRemoveButton(){
		this.removeButton = new JButton("Remove File");
		this.removeButton.addMouseListener(
			new MouseListener(){

			public void mouseClicked(MouseEvent arg0) {
				if(list.getSelectedValue() != null){

					list.removeSelected();
				}
			}

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent arg0) {
			}

			public void mouseReleased(MouseEvent arg0) {
			}

			});
		return this.removeButton;
	}

	private void createTransferButton(){
		this.transferButton = new JButton("Transfer");
		this.transferButton.addMouseListener(new MouseListener(){

			public void mouseClicked(MouseEvent arg0) {
				if (isTransfering == false) {
					transfer();
				}
			}

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {
			}

			public void mousePressed(MouseEvent arg0) {
			}

			public void mouseReleased(MouseEvent arg0) {
			}

		});
	}

	/*
	 *Transfers the files
	 */
    /*made public for api..soon depracted*/
	public void transfer(){
		try {
			final File fileList[] = list.getDirectory().listFiles();
			final FlashTransfer transfer = imageTransfer;

			transferButton.setText("Sending...");
			progressBar.setVisible(true);
			progressBar.setMaximum(fileList.length+1);
			progressBar.setValue(1);	//bump the progress bar
										//up one for visual friendliness

			final ImageListPanel ilist = this;

			killTransfer = false;

			new Thread(
					   new Runnable() {
					   public void run() {

						   try {
							   mySerial = new Serial(115200);
							   isTransfering = true;
							   transfer.setSerial(mySerial);

							   transfer.format();

							   boolean errorFree = true;
							   /* For each file... */
							   for(int i = 0; i < fileList.length; i++){
								   if(killTransfer == true){
									   FlashTransfer.killTransfer();
									   System.out.println("Stopping the transfer");
									   break;
								   }

								   System.out.println("sending: " + fileList[i].getName());

								   if (transfer.sendFile(fileList[i])) {
									   /* Made some progress */
									   progressBar.setValue(progressBar.getValue()+1);
									   progressBar.repaint();
								   } else {
									   errorFree = false;
									   progressBar.setValue(progressBar.getValue()+1);
									   progressBar.repaint();
								   }
							   }
							   /* Exit the image transfer */
							   transfer.close();
							   isTransfering = false;
							   //close the serial
							   mySerial.dispose();
							   /* Reset UI after transfer */
							   ilist.resetUI();
						   } catch (SerialException err) {
							   err.printStackTrace();
							   try { Thread.sleep(500); } catch (Exception e) { ilist.resetUI(); }
							   isTransfering = false;

							   /* Reset UI after transfer */
							   ilist.resetUI();
						   }

					   }}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * Reset the UI after transfer.
	 */
	public void resetUI() {
		   transferButton.setText("Transfer");
		   progressBar.setValue(0);
		   progressBar.setVisible(false);
		   this.repaint();
	}

	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub

	}

	public void componentResized(ComponentEvent e) {
		// TODO Auto-generated method stub
		this.show();
		this.validate();
		this.repaint();

	}

	public void componentShown(ComponentEvent e) {
		this.resetUI();
	}

}
