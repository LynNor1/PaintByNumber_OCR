/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.FileDialog;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Rect;
import paintbynumberpro.PBNPuzzle;
import paintbynumberpro.PuzzleStaticUtilities;

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
private File pbnFile = null;
private PBNPuzzle myPuzzle = null;
private GrabCluesFromPBNJFrame myGrabCluesFromPBNJFrame = null;
private int start_col_clue_from_PBN = -1;
private int start_row_clue_from_PBN = -1;

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

public ImageComponent GetImageComponent()
{ return ic; }

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
public Point GetStartPt ()
{ return ic.getStartPt(); }
public Point GetEndPt ()
{ return ic.getEndPt(); }

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

private void ProcessSelection (ArrayList<String> pbnClues)
{
	Rectangle d = this.ic.GetSelectionRectangle();
	Point startPt = new Point (d.x, d.y);
	Point endPt = new Point (d.x+d.width, d.y+d.height);
	if (IsColSelected())
	{
		if (processcol_JFrame == null)
		{
			processcol_JFrame = new ProcessCol_JFrame (this, "Process Columns", startPt, endPt, pbnClues);
			processcol_JFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		processcol_JFrame.setVisible(true);
		if (!processcol_JFrame.HasClues() || pbnClues != null)
			processcol_JFrame.ResetSettings(pbnClues);
	} else
	{
		if (processrow_JFrame == null)
		{
			processrow_JFrame = new ProcessRow_JFrame (this, "Process Rows", startPt, endPt, pbnClues);
			processrow_JFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		processrow_JFrame.setVisible(true);
		if (!processrow_JFrame.HasClues() || pbnClues != null)
			processrow_JFrame.ResetSettings(pbnClues);
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
        jPanel1 = new javax.swing.JPanel();
        saveJButton = new javax.swing.JButton();
        SavePBNJButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        BrightenJButton = new javax.swing.JButton();
        manualJButton = new javax.swing.JButton();
        MidPointJSlider = new javax.swing.JSlider();
        ContrastPlusJButton = new javax.swing.JButton();
        ScaleJSlider = new javax.swing.JSlider();
        SaveImageJButton = new javax.swing.JButton();
        ReloadJButton = new javax.swing.JButton();
        HomographyJButton = new javax.swing.JButton();
        ScaleJButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        OCRProcessJButton = new javax.swing.JButton();
        LoadPBNJButton = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        jTextField2 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox<>();
        ReviewColsJButton = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        ReviewRowsJButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        saveJButton.setText("Save Clues...");
        saveJButton.setToolTipText("Save clues to .pbn file format");
        saveJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveJButtonActionPerformed(evt);
            }
        });

        SavePBNJButton.setText("Save PBN...");
        SavePBNJButton.setEnabled(false);
        SavePBNJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SavePBNJButtonActionPerformed(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        BrightenJButton.setText("Brighten");
        BrightenJButton.setToolTipText("Brighten pixels above midpoint");
        BrightenJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BrightenJButtonActionPerformed(evt);
            }
        });

        manualJButton.setText("Manual Straight.");
        manualJButton.setToolTipText("Hold SHIFT while click drag to select vertical line first");
        manualJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualJButtonActionPerformed(evt);
            }
        });

        MidPointJSlider.setMajorTickSpacing(64);
        MidPointJSlider.setMaximum(256);
        MidPointJSlider.setPaintLabels(true);
        MidPointJSlider.setPaintTicks(true);
        MidPointJSlider.setToolTipText("Sets pixel value at midpoint between bright and dark");
        MidPointJSlider.setValue(127);

        ContrastPlusJButton.setText("Contrast+");
        ContrastPlusJButton.setToolTipText("Brighten pixels above midpoint and darken those below");
        ContrastPlusJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContrastPlusJButtonActionPerformed(evt);
            }
        });

        ScaleJSlider.setMajorTickSpacing(50);
        ScaleJSlider.setMaximum(200);
        ScaleJSlider.setMinimum(50);
        ScaleJSlider.setPaintLabels(true);
        ScaleJSlider.setPaintTicks(true);
        ScaleJSlider.setToolTipText("Size scaling to apply (percentage)");
        ScaleJSlider.setValue(100);

        SaveImageJButton.setText("Save Image...");
        SaveImageJButton.setToolTipText("Save the modified image to file");
        SaveImageJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveImageJButtonActionPerformed(evt);
            }
        });

        ReloadJButton.setText("Reload Image");
        ReloadJButton.setToolTipText("Reload image from file");
        ReloadJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReloadJButtonActionPerformed(evt);
            }
        });

        HomographyJButton.setText("Homography Txfm");
        HomographyJButton.setToolTipText("CTRL-click the 4 corners of the polygon you need to straighten");
        HomographyJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HomographyJButtonActionPerformed(evt);
            }
        });

        ScaleJButton.setText("Scale Size");
        ScaleJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ScaleJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ScaleJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 224, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(ReloadJButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(manualJButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(HomographyJButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(ContrastPlusJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BrightenJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ScaleJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(MidPointJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SaveImageJButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ReloadJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(HomographyJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ContrastPlusJButton)
                    .addComponent(BrightenJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(MidPointJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScaleJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScaleJSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SaveImageJButton)
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setText("#");
        jLabel1.setToolTipText("# of columns or rows of clues");

        jLabel2.setText("Max # Clues");
        jLabel2.setToolTipText("Max # of clues per col or row");

        OCRProcessJButton.setText("OCR Process...");
        OCRProcessJButton.setToolTipText("Use OCR to extract clue values");
        OCRProcessJButton.setEnabled(false);
        OCRProcessJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OCRProcessJButtonActionPerformed(evt);
            }
        });

        LoadPBNJButton.setText("Compare w/PBN...");
        LoadPBNJButton.setToolTipText("Load a .pbn file to compare with image clues");
        LoadPBNJButton.setEnabled(false);
        LoadPBNJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadPBNJButtonActionPerformed(evt);
            }
        });

        jCheckBox1.setText("Lock selection");
        jCheckBox1.setToolTipText("Select this when you have selected the clues to process (click and drag)");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jTextField2.setText("10");
        jTextField2.setToolTipText("Maximum # of clues per column or row");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Column Clues", "Row Clues" }));
        jComboBox1.setToolTipText("Choose between processing column and row clues");

        ReviewColsJButton.setText("Review Cols...");
        ReviewColsJButton.setToolTipText("Review the column clues that you've already processed");
        ReviewColsJButton.setEnabled(false);
        ReviewColsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReviewColsJButtonActionPerformed(evt);
            }
        });

        jTextField1.setText("10");
        jTextField1.setToolTipText("# of sets of columns or rows (of clues) to process");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
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

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(OCRProcessJButton)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(LoadPBNJButton))
                        .addComponent(ReviewRowsJButton))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jCheckBox1, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                    .addGap(6, 6, 6)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(jLabel2)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(jLabel1)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addComponent(ReviewColsJButton))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(OCRProcessJButton)
                    .addComponent(LoadPBNJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ReviewRowsJButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ReviewColsJButton)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(saveJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(SavePBNJButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(52, 52, 52)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveJButton)
                    .addComponent(SavePBNJButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 704, Short.MAX_VALUE))
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
		OCRProcessJButton.setEnabled(jCheckBox1.isSelected());
		LoadPBNJButton.setEnabled(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
		int new_num = getNumColsOrRows();
		if (new_num > 0)
			ic.NotifyNewNumColsOrRows (new_num);
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void OCRProcessJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OCRProcessJButtonActionPerformed
        ProcessSelection(null);
    }//GEN-LAST:event_OCRProcessJButtonActionPerformed

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

    private void HomographyJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HomographyJButtonActionPerformed
		if (ic.getCornersCollected() != 4)
		{
			JOptionPane.showMessageDialog(this, 
				"Select 4 corners of an area that needs to be straighted out (hold CTRL while clicking)", 
				"Select Four Corners", JOptionPane.OK_OPTION);
			return;			
		} else
		{
			Point[] corners = ic.getCorners();
			
			// Let's calculate an overall rotation based on perceived left edge
			
			// Sort points by .x value from lowest to highest
			LinkedList<Point> cornerListSortByX = new LinkedList();
			cornerListSortByX.add(corners[0]);
			for (int i=1; i<4; i++)
			{
				Point corner = corners[i];
				boolean is_added = false;
				for (Point pt : cornerListSortByX)
				{
					int idx = cornerListSortByX.indexOf(pt);
					if (corner.x < pt.x)
					{
						cornerListSortByX.add(idx, corner);
						is_added = true;
						break;
					}
				}
				if (!is_added) cornerListSortByX.add(corner);
			}
			
			Point startPt;
			Point endPt;
			if (cornerListSortByX.get(0).y < cornerListSortByX.get(1).y)
			{
				startPt = cornerListSortByX.get(0);
				endPt = cornerListSortByX.get(1);
			} else
			{
				startPt = cornerListSortByX.get(1);
				endPt = cornerListSortByX.get(0);
			}
			
			double rotAngRad1 = Math.asin((double)(endPt.x - startPt.x)/(double)(endPt.y - startPt.y));

			if (cornerListSortByX.get(2).y < cornerListSortByX.get(3).y)
			{
				startPt = cornerListSortByX.get(2);
				endPt = cornerListSortByX.get(3);
			} else
			{
				startPt = cornerListSortByX.get(3);
				endPt = cornerListSortByX.get(2);
			}		
			double rotAngRad2 = Math.asin((double)(endPt.x - startPt.x)/(double)(endPt.y - startPt.y));
			
			double rotAngRad = (rotAngRad1 + rotAngRad2)/2.0;
			double rotAngDeg = rotAngRad*180.0/Math.PI;			
			
			System.out.println ("Averaged rotation angle is: " + rotAngDeg + " (deg)");	
			
			// Calculate width of rotated image
			double cos = Math.cos(-rotAngRad);
			double sin = Math.sin(-rotAngRad);
			BufferedImage startImage = ic.getCurrentImage();
			int w = (int) Math.floor(startImage.getWidth() * cos + startImage.getHeight() * sin);
			int h = (int) Math.floor(startImage.getHeight() * cos + startImage.getWidth() * sin);
			int start_w = startImage.getWidth();
			int start_h = startImage.getHeight();		
			
			// Now rotate all four corners
			Point[] rotCornersSortByX = new Point[4];
			for (int i=0; i<4; i++)
			{
				Point corner = cornerListSortByX.get(i);
				double startX = corner.x - start_w/2.0;
				double startY = corner.y - start_h/2.0;
				double newX = startX*Math.cos(-rotAngRad) + startY*Math.sin(-rotAngRad);
				double newY = startY*Math.cos(-rotAngRad) - startX*Math.sin(-rotAngRad);
				newX += w/2.0;
				newY += h/2.0;
				rotCornersSortByX[i] = new Point((int)Math.round(newX), (int)Math.round(newY));
//				System.out.println ("Input corner: " + corner.x + " " + corner.y);
//				System.out.println ("  Rotated   : " + rotCornersSortByX[i].x + " " + rotCornersSortByX[i].y);
			}
			
			// Now calculate the smaller and bigger y values
			double smallX = (cornerListSortByX.get(0).x + cornerListSortByX.get(1).x)/2.0;
			double bigX   = (cornerListSortByX.get(2).x + cornerListSortByX.get(3).x)/2.0;			
			
			// Now sort corners by y
			LinkedList<Point> rotCornerListSortByY = new LinkedList();
			rotCornerListSortByY.add(rotCornersSortByX[0]);
			for (int i=1; i<4; i++)
			{
				Point corner = rotCornersSortByX[i];
				boolean is_added = false;
				for (Point pt : rotCornerListSortByY)
				{
					int idx = rotCornerListSortByY.indexOf(pt);
					if (corner.y < pt.y)
					{
						rotCornerListSortByY.add(idx, corner);
						is_added = true;
						break;
					}
				}
				if (!is_added) rotCornerListSortByY.add(corner);
			}

			// Now calculate the smaller and bigger y values
			double smallY = (rotCornerListSortByY.get(0).y + rotCornerListSortByY.get(1).y)/2.0;
			double bigY   = (rotCornerListSortByY.get(2).y + rotCornerListSortByY.get(3).y)/2.0;
			
			// Establish new corners for the homography transformation
			// based on the rotated list sorted by x
			Point[] outCorners = new Point[4];
			
			// small x - point 0
			double x = smallX;
			double y = smallY;
			if (rotCornersSortByX[0].y > rotCornersSortByX[1].y)
				y = bigY;
			outCorners[0] = new Point ((int)Math.round(x), (int)Math.round(y));
			
			// small x - point 1
			y = smallY;
			if (rotCornersSortByX[1].y > rotCornersSortByX[0].y)
				y = bigY;			
			outCorners[1] = new Point ((int)Math.round(x), (int)Math.round(y));
			
			// big x - point 2
			x = bigX;
			y = smallY;
			if (rotCornersSortByX[2].y > rotCornersSortByX[3].y)
				y = bigY;
			outCorners[2] = new Point ((int)Math.round(x), (int)Math.round(y));
			
			// big x - point 3
			x = bigX;
			y = smallY;
			if (rotCornersSortByX[3].y > rotCornersSortByX[2].y)
				y = bigY;
			outCorners[3] = new Point ((int)Math.round(x), (int)Math.round(y));			
			
			ic.setHomography((float)rotAngDeg, rotCornersSortByX, outCorners);
			ic.repaint();			
		}
    }//GEN-LAST:event_HomographyJButtonActionPerformed

    private void ScaleJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ScaleJButtonActionPerformed
		int scalePercent = ScaleJSlider.getValue();
		ic.scaleImage (scalePercent);
		ic.repaint();
    }//GEN-LAST:event_ScaleJButtonActionPerformed

    private void LoadPBNJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadPBNJButtonActionPerformed
    
		// Figure out what to do if we've already read a .pbn file
		if (pbnFile != null && myPuzzle != null)
		{
			int result = JOptionPane.showConfirmDialog(this, "Do you want to load a new .pbn file?", "Open PBN",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.NO_OPTION) return;
			else pbnFile = null;
		}
		
		// Read a new pbnFile if necessary
		File f = null;
		if (pbnFile == null)
		{
			// Let's get a PBN file to read
			FileDialog fd = new FileDialog((java.awt.Frame) null, "Select a .pbn file to read", FileDialog.LOAD);
			fd.setFilenameFilter (new FilenameFilter ()
			{
				public boolean accept (File f, String name)
				{
					String extension = name.substring(name.lastIndexOf("."));
					if (extension == null) return false;
					if (extension.compareTo (".pbn") == 0) return true;
					return false;
				}
			});
			fd.setVisible(true);
			if (fd.getFile() == null) return;
			f = new File(fd.getDirectory(), fd.getFile());

			// Let's try and read the file
			myPuzzle = PuzzleStaticUtilities.CreatePuzzleFromFile(f);
		}
		
		// if successful, then we'll save the file name!
		if (myPuzzle != null)
		{
			// save the file name
			pbnFile = f;
			
			// okay, now we need to get the clues to extract for comparison with the image
			myGrabCluesFromPBNJFrame = new GrabCluesFromPBNJFrame (this, myPuzzle, this.IsColSelected(),
				this.getNumColsOrRows());
			myGrabCluesFromPBNJFrame.setVisible(true);
			
			// now... we wait form the grabclues JFrame to send us the info we need
			
			// enable the savePBN button
			SavePBNJButton.setEnabled(true);
			
		} else pbnFile = null;
		
    }//GEN-LAST:event_LoadPBNJButtonActionPerformed

    private void SavePBNJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SavePBNJButtonActionPerformed

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
		
		// Update the puzzle with the new clues
		if (myColClues != null)
		{
			int start = this.start_col_clue_from_PBN;
			for (int i=0; i<myColClues.size(); i++)
			{
				String clues = myColClues.get(i).trim();
				clues = SaveClues_JFrame.ReplaceCRs (clues);		
				String[] cluelist = clues.split(" ");
				
				// number of clues
				int numclues = cluelist.length;
				
				// reset the myPuzzle clue info
				int col = start + i;
				myPuzzle.SetCol_NClues (col, numclues);
				
				// set all clue values to new values
				for (int cl=0; cl<numclues; cl++)
				{
					int val = 0;
					try {
						val = Integer.parseInt(cluelist[cl]);
						myPuzzle.SetCol_Clues (col, cl, val);						
					}
					catch (NumberFormatException nfe)
					{
						JOptionPane.showMessageDialog (this, "Serious bug parsing new clues", "Parse Error",
								JOptionPane.OK_OPTION);
						return;
					}
				}
			}
		}
		if (myRowClues != null)
		{
			int start = this.start_row_clue_from_PBN;
			for (int i=0; i<myRowClues.size(); i++)
			{
				String clues = myRowClues.get(i).trim();
				String[] cluelist = clues.split(" ");
				
				// number of clues
				int numclues = cluelist.length;
				
				// reset the myPuzzle clue info
				int row = start + i;
				myPuzzle.SetRow_NClues (row, numclues);
				
				// set all clue values to new values
				for (int cl=0; cl<numclues; cl++)
				{
					int val = 0;
					try {
						val = Integer.parseInt(cluelist[cl]);
						myPuzzle.SetRow_Clues (row, cl, val);						
					}
					catch (NumberFormatException nfe)
					{
						JOptionPane.showMessageDialog (this, "Serious bug parsing new clues", "Parse Error",
								JOptionPane.OK_OPTION);
						return;
					}
				}							
			}
		}
		
		// Okay let's get a new .pbn file to write to
		// Have user select a PBN image
		FileDialog fd = new FileDialog((java.awt.Frame) null, "Choose a .pbn file to write to", FileDialog.SAVE);
		fd.setDirectory (pbnFile.getParent());
		fd.setFile (pbnFile.getName());
		fd.setFilenameFilter (new FilenameFilter ()
		{
			public boolean accept (File f, String name)
			{
				String extension = name.substring(name.lastIndexOf("."));
				if (extension == null) return false;
				extension = extension.toLowerCase();
				if (extension.compareTo (".pbn") == 0) return true;
				return false;
			}
		});
		fd.setVisible(true);
		if (fd.getFile() == null) return;
		File f = new File(fd.getDirectory(), fd.getFile());
		
		// Add .pbn if not already at the end of the file name
		if (!f.getName().endsWith (".pbn"))
		{
			File newF = new File (f.getParent(), f.getName() + ".pbn");
			f = newF;
		}
		
		// now write the new file
		if (PuzzleStaticUtilities.WritePuzzleToFile(myPuzzle, f, true))
			JOptionPane.showMessageDialog(null, "Updated clues written to " + f.getName());
		else
			JOptionPane.showMessageDialog(this, "Error writing updated clues written to " + f.getName(), "File Error", 
					JOptionPane.OK_OPTION);		
    }//GEN-LAST:event_SavePBNJButtonActionPerformed

	public static String[] ShowImageIOInfo () {
//        String[] formats = ImageIO.getReaderFormatNames();
//        for (int i = 0; i < formats.length; ++i) {
//          System.out.println("reader " + formats[i]);
//        }

        String[] names = ImageIO.getWriterFormatNames();
//        for (int i = 0; i < names.length; ++i) {
//          System.out.println("writer " + names[i]);
//        }
        
        return names;
    }        

	
    private void SaveImageJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveImageJButtonActionPerformed

		// Let's get the ImageIO formats we can read
		final String[] formats = ShowImageIOInfo();

		// Okay let's get a new image file to write to
		FileDialog fd = new FileDialog((java.awt.Frame) null, "Choose an image file to write to", FileDialog.SAVE);
		fd.setDirectory (originalFile.getParent());
		fd.setFile (originalFile.getName());
		fd.setFilenameFilter (new FilenameFilter ()
		{
			public boolean accept (File f, String name)
			{
				String extension = name.substring(name.lastIndexOf("."));
				if (extension == null) return false;
				extension = extension.toLowerCase();
                for (int i=0; i<formats.length; i++)
                    if (extension.compareTo ("." + formats[i]) == 0) return true;
				return false;
			}
		});
		fd.setVisible(true);
		if (fd.getFile() == null) return;
		File f = new File(fd.getDirectory(), fd.getFile());
		
		// Get the extension so I know the image format type selected
		String name = f.getName();
		String extension = name.substring(name.lastIndexOf("."));
		if (extension.startsWith(".")) extension = extension.substring(1);
		
		// Grab the BufferedImage from the image component
		BufferedImage theImage = ic.getCurrentImage();
		
		// Now write the image to the selected output file
		try
		{
			ImageIO.write(theImage, extension, f);
			JOptionPane.showMessageDialog (null, "Image saved!");
		}
		catch (IOException ie)
		{
			JOptionPane.showConfirmDialog (null, "Error saving image", "Image Write Error", JOptionPane.OK_OPTION);
		}
        
    }//GEN-LAST:event_SaveImageJButtonActionPerformed

	public void SetStartingColOrRowForComparison (int start, boolean is_col)
	{
		// need to set up ArrayList<String> for clues from myPuzzle starting
		// with row or column start
		ArrayList<String> clueStrings = new ArrayList();
		
		for (int i=0; i<this.getNumColsOrRows(); i++)
		{
			String clues = "";
			int ic = i + start;
			if (is_col)
			{
				int n = myPuzzle.GetCol_NClues(ic);
				for (int c=0; c<n; c++)
				{
					int val = myPuzzle.GetCol_Clues(ic, c);
					clues += Integer.toString(val) + "\n";
				}
				clueStrings.add(clues.trim());
			} else
			{
				int n = myPuzzle.GetRow_NClues(ic);
				for (int c=0; c<n; c++)
				{
					int val = myPuzzle.GetRow_Clues(ic, c);
					clues += Integer.toString(val) + " ";
				}	
				clueStrings.add(clues.trim());
			}
		}
		if (is_col)
			this.start_col_clue_from_PBN = start;
		else
			this.start_row_clue_from_PBN = start;
		
		// kill the GrabClues JFrame
		myGrabCluesFromPBNJFrame.dispose();
		myGrabCluesFromPBNJFrame = null;
		
		// then set up ProcessRow|Col_JFrame with these clues from the .pbn
		ProcessSelection (clueStrings);
	}
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BrightenJButton;
    private javax.swing.JButton ContrastPlusJButton;
    private javax.swing.JButton HomographyJButton;
    private javax.swing.JButton LoadPBNJButton;
    private javax.swing.JSlider MidPointJSlider;
    private javax.swing.JButton OCRProcessJButton;
    private javax.swing.JButton ReloadJButton;
    private javax.swing.JButton ReviewColsJButton;
    private javax.swing.JButton ReviewRowsJButton;
    private javax.swing.JButton SaveImageJButton;
    private javax.swing.JButton SavePBNJButton;
    private javax.swing.JButton ScaleJButton;
    private javax.swing.JSlider ScaleJSlider;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JButton manualJButton;
    private javax.swing.JButton saveJButton;
    // End of variables declaration//GEN-END:variables
}
