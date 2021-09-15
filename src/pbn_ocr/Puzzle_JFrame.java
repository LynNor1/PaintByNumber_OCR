/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.DataBufferByte;
import java.awt.image.LookupOp;
import java.awt.image.ByteLookupTable;
import java.awt.Point;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Rect;

/**
 *
 * @author Lynne
 */
public class Puzzle_JFrame extends javax.swing.JFrame {

private BufferedImage bimage;
private ImageComponent ic;
ProcessCol_JFrame processcol_JFrame = null;
ProcessRow_JFrame processrow_JFrame = null;

private static final byte[] invertTable;

static {
    invertTable = new byte[256];
    for (int i = 0; i < 256; i++) {
        invertTable[i] = (byte) (255 - i);
    }
}

/**
 * Creates new form Puzzle_JFrame
 */
public Puzzle_JFrame(BufferedImage img, String name) {

	bimage = img;	
	ic = new ImageComponent (bimage, this);	
	initComponents();
	
	setTitle (name);
}

private float AutoCalculateRotation ()
{
	// Take the center 50% of the scanned image
	int width = bimage.getWidth();
	int height = bimage.getHeight();
	int half_width = Math.floorDiv(width, 2);
	int half_height = Math.floorDiv(height, 2);
	int optimalDFTRows = Core.getOptimalDFTSize(half_height);
	int optimalDFTCols = Core.getOptimalDFTSize(half_width);	
	int start_col = Math.floorDiv (width-optimalDFTCols, 2);
	int start_row = Math.floorDiv (height-optimalDFTRows, 2);	
//	System.out.println ("Optimal rows and cols: " + optimalDFTRows + " " + optimalDFTCols);
	BufferedImage centered_bimg = bimage.getSubimage(start_col, start_row, optimalDFTCols, optimalDFTRows);
	
	// Display center of image being processed in a new window	
	/*
	EventQueue.invokeLater(new Runnable()
	{
		public void run(){			
			Image_JFrame genericImg_JFrame = new Image_JFrame (centered_bimg, "Center of Image");
			genericImg_JFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			genericImg_JFrame.setVisible(true);
		}
	}
	);		
	*/
	
	// Convert centered bimg to Mat
	byte[] pixels = ((DataBufferByte) centered_bimg.getRaster().getDataBuffer()).getData();
	Mat newMat = new Mat(optimalDFTRows, optimalDFTCols, CvType.CV_8UC1);
	newMat.put (0, 0, pixels);
	
	// Convert to float
	newMat.convertTo(newMat, CvType.CV_32FC1);
	
	// Now make an ArrayList of Mats to hold Re and Im
	ArrayList<Mat> planes = new ArrayList<Mat>();
	
	// Create a Mat of zeros
	Mat zerosMat = Mat.zeros(optimalDFTRows, optimalDFTCols, CvType.CV_32FC1);
	
	// Combine real and imaginary into a complex Mat
	planes.add(newMat);
	planes.add(zerosMat);
	Mat complexI = new Mat();
	Core.merge(planes, complexI);         // Add to the expanded another plane with zeros	
	
	// Now calculate 2D FFT of centered image
	Core.dft (complexI, complexI);
	
	// Now get the magnitude of the results
	Core.split(complexI, planes);                               // planes.get(0) = Re(DFT(I)
																// planes.get(1) = Im(DFT(I))
	Core.magnitude(planes.get(0), planes.get(1), planes.get(0));// planes.get(0) = magnitude
	Mat magI = planes.get(0);	
	
	// Convert to log scale
	Core.log(magI, magI);
	
	// rearrange the quadrants of Fourier image  so that the origin is at the image center
	int cx = magI.cols()/2;
	int cy = magI.rows()/2;
	Mat q0 = new Mat(magI, new Rect(0, 0, cx, cy));   // Top-Left - Create a ROI per quadrant
	Mat q1 = new Mat(magI, new Rect(cx, 0, cx, cy));  // Top-Right
	Mat q2 = new Mat(magI, new Rect(0, cy, cx, cy));  // Bottom-Left
	Mat q3 = new Mat(magI, new Rect(cx, cy, cx, cy)); // Bottom-Right
	Mat tmp = new Mat();               // swap quadrants (Top-Left with Bottom-Right)
	q0.copyTo(tmp);
	q3.copyTo(q0);
	tmp.copyTo(q3);
	q1.copyTo(tmp);                    // swap quadrant (Top-Right with Bottom-Left)
	q2.copyTo(q1);
	tmp.copyTo(q2);
	
	Core.normalize(magI, magI, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1); // Transform the matrix with float values
																		// into a viewable image form (float between


	// Create an empty image in matching format
	BufferedImage fftImage = new BufferedImage(magI.width(), magI.height(), BufferedImage.TYPE_BYTE_GRAY);

	// Get the BufferedImage's backing array and copy the pixels directly into it
	byte[] data = ((DataBufferByte) fftImage.getRaster().getDataBuffer()).getData();
	magI.get(0, 0, data);
	
	// Display fftImage in a new window	
	/*
	EventQueue.invokeLater(new Runnable()
	{
		public void run(){			
			Image_JFrame genericImg_JFrame = new Image_JFrame (fftImage, "FFT of Center of Image");
			genericImg_JFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			genericImg_JFrame.setVisible(true);
		}
	}
	);		
	*/
	
	// Find the location of the peak value in the middle row
	double peak_col_at_row_1 = 0;
	double peak_val = 0.0f;
	int target_row_1 = (optimalDFTRows / 2);
	for (int i=0; i<optimalDFTCols; i++)
	{
		double dft_val = magI.get(target_row_1, i)[0];
		if (i == 0 || dft_val > peak_val)
		{
			peak_val = dft_val;
			peak_col_at_row_1 = i;
		}
	}
	System.out.println ("Location of DFT peak in row " + target_row_1 + ": " + peak_col_at_row_1);
	
	// Find location of the peak value in the halfway point (to avoid the aliasing)
	int target_row_2 = target_row_1 + (optimalDFTRows / 4) - 1;
	double peak_col_at_row_2 = 0;
	peak_val = 0.0f;
	for (int i=0; i<optimalDFTCols; i++)
	{
		double dft_val = magI.get(target_row_2, i)[0];
		if (i == 0 || dft_val > peak_val)
		{
			peak_val = dft_val;
			peak_col_at_row_2 = i;
		}
	}	
	System.out.println ("Location of DFT peak in row " + target_row_2 + ": " + peak_col_at_row_2);	
	
	// Calculate rotation angle based on location of peaks
	double rotAngRad = Math.asin((peak_col_at_row_2 - peak_col_at_row_1)/(target_row_2 - target_row_1));
	double rotAngDeg = -rotAngRad*180.0/Math.PI;
	System.out.println ("Rotation angle is: " + rotAngDeg + " (deg)");
			
	return (float)rotAngDeg;
}

public int getNumColsOrRows ()
{
	int num;
	try
	{
		num = Integer.parseInt(jTextField1.getText());
		if (num < 1)
		{
			jTextField1.setSelectionStart(0);
			jTextField1.setSelectionEnd(jTextField1.getText().length());
			num = 0;
		}
	}
	catch (NumberFormatException nfe)
	{
		num = 0;
		jTextField1.setSelectionStart(0);
		jTextField1.setSelectionEnd(jTextField1.getText().length());
	}
	return num;
}

public int getMaxNumCluesPerColOrRow ()
{
	int num;
	try
	{
		num = Integer.parseInt(jTextField2.getText());
		if (num < 1)
		{
			jTextField2.setSelectionStart(0);
			jTextField2.setSelectionEnd(jTextField2.getText().length());
			num = 0;
		}
	}
	catch (NumberFormatException nfe)
	{
		num = 0;
		jTextField2.setSelectionStart(0);
		jTextField2.setSelectionEnd(jTextField2.getText().length());
	}
	return num;
}

public boolean IsColSelected ()
{
	int index = this.jComboBox1.getSelectedIndex();
	return (index == 0);
}
public boolean IsRowSelected ()
{
	int index = this.jComboBox1.getSelectedIndex();
	return (index == 1);
}

public BufferedImage getSelectionByIndex (int i)
{
	return ic.GetSelectionByIndex(i);
}

private void ProcessSelection ()
{
	if (IsColSelected())
	{
		if (processcol_JFrame == null)
		{
			processcol_JFrame = new ProcessCol_JFrame (this, "Process Columns");
			processcol_JFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		processcol_JFrame.setVisible(true);	
		processcol_JFrame.ResetSettings();
	} else
	{
		if (processrow_JFrame == null)
		{
			processrow_JFrame = new ProcessRow_JFrame (this, "Process Rows");
			processrow_JFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		processrow_JFrame.setVisible(true);
		processrow_JFrame.ResetSettings();
	}
	
}

/**
 * This method is called from within the constructor to initialize the form.
 * WARNING: Do NOT modify this code. The content of this method is always
 * regenerated by the Form Editor.
 */
@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane(ic);
        jButton1 = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        saveJButton = new javax.swing.JButton();
        undoRotateJButton = new javax.swing.JButton();
        manualJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jButton1.setText("Auto Straighten");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jCheckBox1.setText("Lock selection");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Column Clues", "Row Clues" }));

        jLabel1.setText("#");
        jLabel1.setToolTipText("# of columns or rows of clues");

        jTextField1.setText("10");
        jTextField1.setToolTipText("# of sets of columns or rows (of clues) to process");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton2.setText("Process...");
        jButton2.setEnabled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel2.setText("Max # Clues");
        jLabel2.setToolTipText("Max # of clues per col or row");

        jTextField2.setText("0");
        jTextField2.setToolTipText("0 implies no lines to remove between clues");

        jButton3.setText("Contrast+");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("Contrast-");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        saveJButton.setText("Save Clues...");
        saveJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveJButtonActionPerformed(evt);
            }
        });

        undoRotateJButton.setText("Undo Rotate");
        undoRotateJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoRotateJButtonActionPerformed(evt);
            }
        });

        manualJButton.setText("Manual Straight.");
        manualJButton.setToolTipText("Hold SHIFT while click drag to select vertical line first");
        manualJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1))
                    .addComponent(manualJButton)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(undoRotateJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBox1)
                    .addComponent(jButton2)
                    .addComponent(saveJButton)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(manualJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(undoRotateJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                        .addComponent(saveJButton))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		float newRotDeg = AutoCalculateRotation();
		ic.setRotationDeg(newRotDeg);
		ic.repaint();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        ic.setLockSelection (jCheckBox1.isSelected());
		jButton2.setEnabled(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
		int new_num = getNumColsOrRows();
		if (new_num > 0)
			ic.NotifyNewNumColsOrRows (new_num);
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        ProcessSelection();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
	    ic.increaseContrast();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        ic.decreaseContrast();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed

		// Grab row and column clues (we need at least one of them to be available)
		ArrayList<String> myColClues = null;
		ArrayList<String> myRowClues = null;
		if (processcol_JFrame != null) myColClues = processcol_JFrame.GetClues();
		if (processrow_JFrame != null) myRowClues = processrow_JFrame.GetClues();
		
		// abort if we don't have either
		if (myColClues == null && myRowClues == null)
		{
			JOptionPane.showMessageDialog(this, "No clues have been processed", 
					"Save Error", JOptionPane.OK_OPTION);
			return;
		}
		
		// check that we have something for each clue
		if (myColClues != null)
		{
			for (int i=0; i<myColClues.size(); i++)
				if (myColClues.get(i) == null || myColClues.get(i).length() == 0)
				{
					JOptionPane.showMessageDialog(this, 
						"Not all columns have been processed (" + i + ")", "Save Error", JOptionPane.OK_OPTION);
					return;
				}
		}
		if (myRowClues != null)
		{
			for (int i=0; i<myRowClues.size(); i++)
				if (myRowClues.get(i) == null || myRowClues.get(i).length() == 0)
				{
					JOptionPane.showMessageDialog(this, 
						"Not all rows have been processed (" + i + ")", "Save Error", JOptionPane.OK_OPTION);
					return;
				}
		}
		
		SaveClues_JFrame save_JFrame = new SaveClues_JFrame (myColClues, myRowClues, getTitle());
		save_JFrame.setDefaultCloseOperation (JFrame.DO_NOTHING_ON_CLOSE);
		save_JFrame.setVisible(true);
    }//GEN-LAST:event_saveJButtonActionPerformed

    private void undoRotateJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoRotateJButtonActionPerformed
        ic.UndoRotation();
    }//GEN-LAST:event_undoRotateJButtonActionPerformed

    private void manualJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manualJButtonActionPerformed
		if (!ic.getIsLine())
		{
			JOptionPane.showMessageDialog(this, 
				"Draw a line that should be made vertical (hold SHIFT while click-and-drag)", 
				"Draw a Line", JOptionPane.OK_OPTION);
			return;			
		} else
		{
			Point startPt = ic.getStartPt();
			Point endPt = ic.getEndPt();
			
			double rotAngRad = Math.asin((double)(endPt.x - startPt.x)/(double)(endPt.y - startPt.y));
			double rotAngDeg = rotAngRad*180.0/Math.PI;
			System.out.println ("Rotation angle is: " + rotAngDeg + " (deg)");		
			
			ic.setRotationDeg((float)rotAngDeg);
			ic.clearSelection();
			ic.repaint();			
		}
		
    }//GEN-LAST:event_manualJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JButton manualJButton;
    private javax.swing.JButton saveJButton;
    private javax.swing.JButton undoRotateJButton;
    // End of variables declaration//GEN-END:variables
}
