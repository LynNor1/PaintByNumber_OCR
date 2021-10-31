/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Rect;

/**
 *
 * @author Lynne
 */
public class Puzzle_JFrame extends javax.swing.JFrame {

private BufferedImage bimage;
private ImageComponent ic;
private ProcessCol_JFrame processcol_JFrame = null;
private ProcessRow_JFrame processrow_JFrame = null;
private File originalFile;

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
public Puzzle_JFrame(BufferedImage img, String name, File f) {

	originalFile = f;
	bimage = img;	
	ic = new ImageComponent (bimage, this);	
	initComponents();
	
	setTitle (name);
}

private void ReloadOriginalFile ()
{
	try
	{
		// Read image
		BufferedImage color_img = ImageIO.read(originalFile);
		if (color_img == null) return;

		// Convert to greyscale
		BufferedImage greyImg = new BufferedImage(color_img.getWidth(), color_img.getHeight(),  
			BufferedImage.TYPE_BYTE_GRAY);  
		Graphics g = greyImg.getGraphics();  
		g.drawImage(color_img, 0, 0, null);  
		g.dispose();
		
		// update the ImageComponent with the new image
		ic.NotifyNewImage (greyImg);
		
	} catch (IOException ie)
	{
		System.out.println ("IOException: " + ie.getLocalizedMessage());
	}
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
	if (num < 0) num = 10;
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

public BufferedImage getSelectionByIndex (int i, Point start, Point end, int num, boolean is_row)
{
	return ic.GetSelectionByIndex(i, start, end, num, is_row);
}

public void EnableReviewRows ()
{
	ReviewRowsJButton.setEnabled(true);
}

public void EnableReviewCols ()
{
	ReviewColsJButton.setEnabled(true);
}

public int GetMidPoint ()
{
	return MidPointJSlider.getValue();
}

private void ProcessSelection ()
{
	Rectangle d = this.ic.GetSelectionRectangle();
	Point startPt = new Point (d.x, d.y);
	Point endPt = new Point (d.x+d.width, d.y+d.height);
	if (IsColSelected())
	{
		if (processcol_JFrame == null)
		{
			processcol_JFrame = new ProcessCol_JFrame (this, "Process Columns", startPt, endPt);
			processcol_JFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		processcol_JFrame.setVisible(true);	
		processcol_JFrame.ResetSettings();
	} else
	{
		if (processrow_JFrame == null)
		{
			processrow_JFrame = new ProcessRow_JFrame (this, "Process Rows", startPt, endPt);
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
        ReloadJButton = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        ProcessJButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        ContrastPlusJButton = new javax.swing.JButton();
        BrightenJButton = new javax.swing.JButton();
        saveJButton = new javax.swing.JButton();
        undoRotateJButton = new javax.swing.JButton();
        manualJButton = new javax.swing.JButton();
        ReviewRowsJButton = new javax.swing.JButton();
        ReviewColsJButton = new javax.swing.JButton();
        MidPointJSlider = new javax.swing.JSlider();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        ReloadJButton.setText("Reload Image");
        ReloadJButton.setToolTipText("Reload image from file");
        ReloadJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReloadJButtonActionPerformed(evt);
            }
        });

        jCheckBox1.setText("Lock selection");
        jCheckBox1.setToolTipText("Select this when you have selected the clues to process (click and drag)");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Column Clues", "Row Clues" }));
        jComboBox1.setToolTipText("Choose between processing column and row clues");

        jLabel1.setText("#");
        jLabel1.setToolTipText("# of columns or rows of clues");

        jTextField1.setText("10");
        jTextField1.setToolTipText("# of sets of columns or rows (of clues) to process");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        ProcessJButton.setText("OCR Process...");
        ProcessJButton.setToolTipText("Use OCR to extract clue values");
        ProcessJButton.setEnabled(false);
        ProcessJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ProcessJButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Max # Clues");
        jLabel2.setToolTipText("Max # of clues per col or row");

        jTextField2.setText("10");
        jTextField2.setToolTipText("Maximum # of clues per column or row");

        ContrastPlusJButton.setText("Contrast+");
        ContrastPlusJButton.setToolTipText("Brighten pixels above midpoint and darken those below");
        ContrastPlusJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContrastPlusJButtonActionPerformed(evt);
            }
        });

        BrightenJButton.setText("Brighten");
        BrightenJButton.setToolTipText("Brighten pixels above midpoint");
        BrightenJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BrightenJButtonActionPerformed(evt);
            }
        });

        saveJButton.setText("Save Clues...");
        saveJButton.setToolTipText("Save clues to .pbn file format");
        saveJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveJButtonActionPerformed(evt);
            }
        });

        undoRotateJButton.setText("Undo Rotate");
        undoRotateJButton.setToolTipText("Undo the applied rotation");
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

        ReviewRowsJButton.setText("Review Rows...");
        ReviewRowsJButton.setToolTipText("Review the row clues that you've already processed");
        ReviewRowsJButton.setEnabled(false);
        ReviewRowsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReviewRowsJButtonActionPerformed(evt);
            }
        });

        ReviewColsJButton.setText("Review Cols...");
        ReviewColsJButton.setToolTipText("Review the column clues that you've already processed");
        ReviewColsJButton.setEnabled(false);
        ReviewColsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReviewColsJButtonActionPerformed(evt);
            }
        });

        MidPointJSlider.setMaximum(255);
        MidPointJSlider.setToolTipText("Sets pixel value at midpoint between bright and dark");
        MidPointJSlider.setValue(127);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField2))
                    .addComponent(ReloadJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(undoRotateJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ContrastPlusJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jCheckBox1)
                    .addComponent(ProcessJButton)
                    .addComponent(saveJButton)
                    .addComponent(ReviewRowsJButton)
                    .addComponent(ReviewColsJButton)
                    .addComponent(MidPointJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(BrightenJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1))
                    .addComponent(jComboBox1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(manualJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(ReloadJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(manualJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(undoRotateJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ContrastPlusJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BrightenJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(MidPointJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                        .addComponent(ProcessJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ReviewRowsJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ReviewColsJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(saveJButton))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ReloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReloadJButtonActionPerformed
/*
		float newRotDeg = AutoCalculateRotation();
		ic.setRotationDeg(newRotDeg);
		ic.repaint();
*/
		ReloadOriginalFile();
    }//GEN-LAST:event_ReloadJButtonActionPerformed

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        ic.setLockSelection (jCheckBox1.isSelected());
		ProcessJButton.setEnabled(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
		int new_num = getNumColsOrRows();
		if (new_num > 0)
			ic.NotifyNewNumColsOrRows (new_num);
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void ProcessJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ProcessJButtonActionPerformed
        ProcessSelection();
    }//GEN-LAST:event_ProcessJButtonActionPerformed

    private void ContrastPlusJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContrastPlusJButtonActionPerformed
	    ic.increaseContrast();
    }//GEN-LAST:event_ContrastPlusJButtonActionPerformed

    private void BrightenJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BrightenJButtonActionPerformed
        ic.brighten();
    }//GEN-LAST:event_BrightenJButtonActionPerformed

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

    private void ReviewRowsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReviewRowsJButtonActionPerformed
		if (processrow_JFrame != null) processrow_JFrame.setVisible(true);
    }//GEN-LAST:event_ReviewRowsJButtonActionPerformed

    private void ReviewColsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReviewColsJButtonActionPerformed
		if (processcol_JFrame != null) processcol_JFrame.setVisible(true);
    }//GEN-LAST:event_ReviewColsJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BrightenJButton;
    private javax.swing.JButton ContrastPlusJButton;
    private javax.swing.JSlider MidPointJSlider;
    private javax.swing.JButton ProcessJButton;
    private javax.swing.JButton ReloadJButton;
    private javax.swing.JButton ReviewColsJButton;
    private javax.swing.JButton ReviewRowsJButton;
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
