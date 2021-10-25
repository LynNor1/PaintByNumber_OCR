/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.net.URL;
import javax.sound.sampled.*;
import javax.swing.ImageIcon;
import java.io.IOException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *
 * @author Lynne
 */
public class ProcessCol_JFrame extends javax.swing.JFrame {

private BufferedImage selImage;
private ImageComponent ic;
private Puzzle_JFrame puzzle_JFrame;
private Point startPt;
private Point endPt;
private int num_cols_or_rows = 0;
private int max_num_clues = 0;
private int cur_index = 0;
private ITesseract tess = null;

private ProcessSettings mySettings;

private ArrayList<String> myClues;

/**
 * Creates new form Puzzle_JFrame
 */
//public ProcessCol_JFrame(BufferedImage img, String name) {
public ProcessCol_JFrame(Puzzle_JFrame theFrame, String name, Point start, Point end) {	

//	bimage = img;	
	puzzle_JFrame = theFrame;
	startPt = start;
	endPt = end;
	
	// Initialize the image component with our first col or row to process
	num_cols_or_rows = puzzle_JFrame.getNumColsOrRows();
	max_num_clues = puzzle_JFrame.getMaxNumCluesPerColOrRow();
	
	// Initialize the ArrayList with empty strings
	myClues = new ArrayList<String>(num_cols_or_rows);
	for (int i=0; i<num_cols_or_rows; i++) myClues.add(null);
	
	// Set up new Settings
	mySettings = new ProcessSettings (true, max_num_clues);
	
	// Grab the current col or row to process
	BufferedImage bimage = puzzle_JFrame.getSelectionByIndex(cur_index, startPt, endPt, num_cols_or_rows, false);
	
	// Now *copy* it so we operate on a copy
	selImage = cloneImage (bimage);
	
	// Now apply the settings to our copy
	BufferedImage doctoredImage = mySettings.ApplySettings(selImage, max_num_clues);
	
	// Set up Tesseract
	tess = new Tesseract();
	tess.setDatapath("/usr/local/Cellar/tesseract/4.1.1/share/tessdata");	
	tess.setTessVariable("tessedit_char_whitelist", "0123456789");
	
	// Set up the new Image Component
	ic = new ImageComponent (doctoredImage, null);	
	initComponents();
	
	// Process our first image and add output to the TextArea
	String ocr_output = "";
	try
	{ 
		ocr_output = tess.doOCR (doctoredImage);
		ocr_output = cleanUpExtraCRs (ocr_output);
		myClues.set(cur_index, ocr_output);
	}
	catch (TesseractException te)
	{ ocr_output = te.getLocalizedMessage(); }
	SetText(ocr_output);	
	SetLabel (cur_index, num_cols_or_rows);
		
	setTitle (name);
}

public void ResetSettings ()
{
	// Initialize the image component with our first col or row to process
	num_cols_or_rows = puzzle_JFrame.getNumColsOrRows();
	max_num_clues = puzzle_JFrame.getMaxNumCluesPerColOrRow();
	cur_index = 0;
	// Initialize the ArrayList with empty strings
	myClues = new ArrayList<String>(num_cols_or_rows);
	for (int i=0; i<num_cols_or_rows; i++) myClues.add(null);	
	ProcessCurColumn (false);
}

public ArrayList<String> GetClues ()
{ return myClues; }

private void ProcessCurColumn (boolean recycle)
{
	if (!recycle)
	{
		// Grab the current col or row to process
		BufferedImage bimage = puzzle_JFrame.getSelectionByIndex(cur_index, startPt, endPt, num_cols_or_rows, false);

		// Now *copy* it so we operate on a copy
		selImage = cloneImage (bimage);
	}
	
	// Now copy the selected image so that we can doctor it up with our settings
	BufferedImage doctoredImage = mySettings.ApplySettings(selImage, max_num_clues);
	
	// Set up the new Image Component
	ic.NotifyNewImage (doctoredImage);
	
	// Process our first image and add output to the TextArea
	String ocr_output;
	if (myClues.get(cur_index) == null)
	{
		ocr_output = "";
		try
		{ 
			ocr_output = tess.doOCR (doctoredImage);
			ocr_output = cleanUpExtraCRs (ocr_output);
			myClues.set(cur_index, ocr_output);
		}
		catch (TesseractException te)
		{ ocr_output = te.getLocalizedMessage(); }
	} else
		ocr_output = myClues.get(cur_index);
	
	SetText(ocr_output);	
	SetLabel (cur_index, num_cols_or_rows);	
	
	// Check if we've made it to the end and we have clues for all rows
	if (cur_index == num_cols_or_rows-1 && WeHaveCluesForAllCols())
	{
		puzzle_JFrame.EnableReviewCols();
	}	
}

private boolean WeHaveCluesForAllCols()
{
	for (int i=0; i<num_cols_or_rows; i++)
		if (myClues.get(i) == null || myClues.get(i).isEmpty()) return false;
	return true;
}

private String cleanUpExtraCRs (String text)
{
	String outStr = "";
	String[] tokens = text.split("\n");
	boolean is_first = true;
	for (int i=0; i<tokens.length; i++)
	{
		if (tokens[i].length() > 0)
		{
			if (!is_first) outStr += "\n";
			is_first = false;
			outStr += tokens[i];
		}
	}
	return outStr;
}

private String convertCRsToSpaces (String text)
{
	String outStr = "";
	String[] tokens = text.split("\n");
	boolean is_first = true;
	for (int i=0; i<tokens.length; i++)
	{
		if (tokens[i].length() > 0)
		{
			if (!is_first) outStr += " ";
			is_first = false;
			outStr += tokens[i];
		}
	}
	return outStr;
}

public static BufferedImage cloneImage (BufferedImage img)
{
	int type = img.getType();
	BufferedImage copyOfImage = new BufferedImage(img.getWidth(), img.getHeight(), type);
	Graphics g = copyOfImage.createGraphics();
	g.drawImage(img, 0, 0, null);
	return copyOfImage; //or use it however you want
}

public void SetText (String ocr_text)
{
	jTextArea1.setText(ocr_text);
}

public void SetLabel (int cur_index, int num)
{		
	reportJLabel.setText ("Col " + (cur_index+1) + " of " + num);
}

/**
 * This method is called from within the constructor to initialize the form.
 * WARNING: Do NOT modify this code. The content of this method is always
 * regenerated by the Form Editor.
 */
@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane(ic);
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jComboBox2 = new javax.swing.JComboBox<>();
        ImageIcon icon = new ImageIcon("images/up.png");
        upJButton = new javax.swing.JButton(icon);
        ImageIcon iconDown = new ImageIcon ("images/down.png");
        downJButton = new javax.swing.JButton(iconDown);
        ImageIcon iconLeft = new ImageIcon ("images/left.png");
        leftjButton = new javax.swing.JButton(iconLeft);
        ImageIcon iconRight = new ImageIcon ("images/right.png");
        rightjButton = new javax.swing.JButton(iconRight);
        ImageIcon iconThicker = new ImageIcon ("images/thicker.png");
        thickerjButton = new javax.swing.JButton(iconThicker);
        ImageIcon iconThinner = new ImageIcon ("images/thinner.png");
        thinnerjButton = new javax.swing.JButton(iconThinner);
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        reportJLabel = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextArea1KeyTyped(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextArea1KeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(jTextArea1);

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Top", "Bottom", "Left", "Right", "Separator" }));

        upJButton.setText("");
        upJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upJButtonActionPerformed(evt);
            }
        });

        downJButton.setText("Down");
        downJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downJButtonActionPerformed(evt);
            }
        });

        leftjButton.setText("Left");
        leftjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leftjButtonActionPerformed(evt);
            }
        });

        rightjButton.setText("Right");
        rightjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightjButtonActionPerformed(evt);
            }
        });

        thickerjButton.setText("Thck");
        thickerjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thickerjButtonActionPerformed(evt);
            }
        });

        thinnerjButton.setText("Thin");
        thinnerjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thinnerjButtonActionPerformed(evt);
            }
        });

        jButton7.setText("<");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton8.setText(">");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        reportJLabel.setText("Report");

        jButton1.setText("OCR");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Read");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(reportJLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(upJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(leftjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(thickerjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(thinnerjButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(downJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(rightjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton8))
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton2)))
                        .addGap(0, 24, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(upJButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(leftjButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(thickerjButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(downJButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rightjButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(thinnerjButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reportJLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jButton2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton7)
                            .addComponent(jButton8)))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
		if (cur_index < (num_cols_or_rows - 1))
		{
			cur_index++;
			ProcessCurColumn(false);
		}
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
		if (cur_index > 0)
		{
			cur_index--;
			ProcessCurColumn(false);
		}    }//GEN-LAST:event_jButton7ActionPerformed

    private void upJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upJButtonActionPerformed
		mySettings.UpSelected(jComboBox2.getSelectedIndex());
		ProcessCurColumn (true);
    }//GEN-LAST:event_upJButtonActionPerformed

    private void downJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downJButtonActionPerformed
		mySettings.DownSelected(jComboBox2.getSelectedIndex());
		ProcessCurColumn (true);
    }//GEN-LAST:event_downJButtonActionPerformed

    private void leftjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leftjButtonActionPerformed
		mySettings.LeftSelected(jComboBox2.getSelectedIndex());
		ProcessCurColumn (true);
    }//GEN-LAST:event_leftjButtonActionPerformed

    private void rightjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightjButtonActionPerformed
		mySettings.RightSelected(jComboBox2.getSelectedIndex());
		ProcessCurColumn (true);
    }//GEN-LAST:event_rightjButtonActionPerformed

    private void thickerjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thickerjButtonActionPerformed
		mySettings.ThickerSelected(jComboBox2.getSelectedIndex());
		ProcessCurColumn (true);
    }//GEN-LAST:event_thickerjButtonActionPerformed

    private void thinnerjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thinnerjButtonActionPerformed
		mySettings.ThinnerSelected(jComboBox2.getSelectedIndex());
		ProcessCurColumn (true);
    }//GEN-LAST:event_thinnerjButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		myClues.set(cur_index, null);	// This forces the code to reprocess the image
		ProcessCurColumn (true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextArea1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextArea1KeyTyped

    }//GEN-LAST:event_jTextArea1KeyTyped

    private void jTextArea1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextArea1KeyReleased
        // save any changes to the clues
		String newText = jTextArea1.getText();
		myClues.set(cur_index, newText);
    }//GEN-LAST:event_jTextArea1KeyReleased

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		ClueReaderThread clueReaderThread =  new ClueReaderThread (convertCRsToSpaces(myClues.get(cur_index)));
		clueReaderThread.start();
    }//GEN-LAST:event_jButton2ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton downJButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JButton leftjButton;
    private javax.swing.JLabel reportJLabel;
    private javax.swing.JButton rightjButton;
    private javax.swing.JButton thickerjButton;
    private javax.swing.JButton thinnerjButton;
    private javax.swing.JButton upJButton;
    // End of variables declaration//GEN-END:variables
}
