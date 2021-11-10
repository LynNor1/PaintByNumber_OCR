/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *
 * @author Lynne
 */
public class ProcessRow_JFrame extends javax.swing.JFrame implements KeyListener {

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
public ProcessRow_JFrame(Puzzle_JFrame theFrame, String name, Point start, Point end, 
		ArrayList<String> startingClues) {	

//	bimage = img;	
	puzzle_JFrame = theFrame;
	
	startPt = start;
	endPt = end;
	
	// Initialize the image component with our first col or row to process
	num_cols_or_rows = puzzle_JFrame.getNumColsOrRows();
	max_num_clues = puzzle_JFrame.getMaxNumCluesPerColOrRow();
	
	// Initialize the ArrayList with empty strings
	if (startingClues == null)
	{
		myClues = new ArrayList<String>(num_cols_or_rows);
		for (int i=0; i<num_cols_or_rows; i++) myClues.add(null);
	} else
		myClues = startingClues;
	
	// Set up new Settings
	mySettings = new ProcessSettings (false, max_num_clues);
	
	// Grab the current col or row to process
	BufferedImage bimage = puzzle_JFrame.getSelectionByIndex(cur_index, startPt, endPt, num_cols_or_rows, true);
	
	// Now *copy* it so we operate on a copy
	selImage = cloneImage (bimage);
	
	// Now apply the settings to our copy
	BufferedImage doctoredImage = mySettings.ApplySettings(selImage, max_num_clues);
	
	// Set up Tesseract
	tess = new Tesseract();
	tess.setDatapath(PaintByNumber_OCR.TessDataPath);	
	tess.setTessVariable("tessedit_char_whitelist", "0123456789");
	
	// Set up the new Image Component
	ic = new ImageComponent (doctoredImage, null);	
	initComponents();
	
	float pixels_per_clue = doctoredImage.getWidth() / max_num_clues;
	int ipixels_per_clue = (int)Math.floor(pixels_per_clue);	
	float startx = 0.f;
	
	// Process our first image and add output to the TextArea
	if (startingClues == null)
	{
		String ocr_output = "";
		try
		{ 
			if (ProcessLineJCheckBox.isSelected())
			{
				ocr_output = tess.doOCR (doctoredImage);
				ocr_output = AddSpaces(ocr_output.trim());
			} else
			{
				ocr_output = "";
				for (int i=0; i<max_num_clues; i++)
				{
					BufferedImage clueImage = doctoredImage.getSubimage ((int)Math.floor(startx), 0, ipixels_per_clue, doctoredImage.getHeight());
					startx += pixels_per_clue;
					String ocr_out = tess.doOCR (clueImage);
					ocr_output = ocr_output + " " + ocr_out;
				}
			}
			ocr_output = ocr_output.trim();
			myClues.set(cur_index, ocr_output);		
		}
		catch (TesseractException te)
		{ ocr_output = te.getLocalizedMessage(); }
		SetText(ocr_output);	
	} else
		SetText (startingClues.get(0));
	SetLabel (cur_index, num_cols_or_rows);
		
	setTitle (name);
	
	jTextArea1.addKeyListener(this);
	
	if (startingClues != null)
		puzzle_JFrame.EnableReviewRows();
}

public boolean HasClues ()
{ return myClues != null; }

public void ResetSettings (ArrayList<String> pbnClues)
{
	// Initialize the image component with our first col or row to process
	num_cols_or_rows = puzzle_JFrame.getNumColsOrRows();
	max_num_clues = puzzle_JFrame.getMaxNumCluesPerColOrRow();
	cur_index = 0;
	// Initialize the ArrayList with empty strings
	if (pbnClues == null)
	{
		myClues = new ArrayList<String>(num_cols_or_rows);
		for (int i=0; i<num_cols_or_rows; i++) myClues.add(null);	
	} else
		myClues = pbnClues;
	ProcessCurRow (false);
}

public ArrayList<String> GetClues ()
{ return myClues; }

private void ProcessCurRow (boolean recycle)
{
	if (!recycle)
	{
		// Grab the current col or row to process
		BufferedImage bimage = puzzle_JFrame.getSelectionByIndex(cur_index, startPt, endPt, num_cols_or_rows, true);

		// Now *copy* it so we operate on a copy
		selImage = cloneImage (bimage);
	}
	
	// Now copy the selected image so that we can doctor it up with our settings
	BufferedImage doctoredImage = mySettings.ApplySettings(selImage, max_num_clues);
	
	// Set up the new Image Component
	ic.NotifyNewImage (doctoredImage);
	
	// Calculate # of columns per clue
	float pixels_per_clue = doctoredImage.getWidth() / max_num_clues;
	int ipixels_per_clue = (int)Math.round(pixels_per_clue);	
	float startx = 0.f;	
	
	// Process our first image and add output to the TextArea
	String ocr_output = "";
	if (myClues.get(cur_index) == null)
	{	
		try
		{
			if (ProcessLineJCheckBox.isSelected())
			{
				ocr_output = tess.doOCR (doctoredImage);
				ocr_output = AddSpaces(ocr_output.trim());
			} else
			{
				ocr_output = "";
				for (int i=0; i<max_num_clues; i++)
				{
					BufferedImage clueImage = doctoredImage.getSubimage ((int)Math.floor(startx), 0, ipixels_per_clue, doctoredImage.getHeight());
					startx += pixels_per_clue;
					String ocr_out = tess.doOCR (clueImage);
					if (ocr_out.length() > 0)
						ocr_output = ocr_output + " " + ocr_out.trim();
				}
			}
			ocr_output = ocr_output.trim();
			myClues.set(cur_index, ocr_output);
		}
		catch (TesseractException te)
		{ ocr_output = te.getLocalizedMessage(); }
	} else
		ocr_output = myClues.get(cur_index);
	SetText(ocr_output);	
	SetLabel (cur_index, num_cols_or_rows);	
	
	// Check if we've made it to the end and we have clues for all rows
	if (cur_index == num_cols_or_rows-1 && WeHaveCluesForAllRows())
	{
		puzzle_JFrame.EnableReviewRows();
	}
}

private boolean WeHaveCluesForAllRows()
{
	for (int i=0; i<num_cols_or_rows; i++)
		if (myClues.get(i) == null || myClues.get(i).isEmpty()) return false;
	return true;
}

private String AddSpaces (String str)
{
	String newStr = "";
	for (int i=0; i<str.length(); i++)
	{
		newStr = newStr + str.substring(i,i+1) + " ";
	}
	return newStr.trim();
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
	jTextArea1.setText(RemoveCRs(ocr_text));
	jTextArea1.requestFocus();
	jTextArea1.setCaretPosition(0);
}

public void SetLabel (int cur_index, int num)
{		
	reportJLabel.setText ("Col " + (cur_index+1) + " of " + num);
}

public void keyPressed (KeyEvent ke)
{
	// Want to intercept a CR so that we call the "Next" button instead
	if (ke.getKeyCode() == KeyEvent.VK_ENTER)
	{
		ke.consume();
		if (cur_index < (num_cols_or_rows-1))
		{
			cur_index++;
			ProcessCurRow(false);
		}
	}
}
public void keyReleased (KeyEvent ke)
{}
public void keyTyped (KeyEvent ke)
{}

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
        jPanel1 = new javax.swing.JPanel();
        ImageIcon iconDown = new ImageIcon ("images/down.png");
        downJButton = new javax.swing.JButton(iconDown);
        ProcessLineJCheckBox = new javax.swing.JCheckBox();
        ImageIcon icon = new ImageIcon("images/up.png");
        upJButton = new javax.swing.JButton(icon);
        jButton1 = new javax.swing.JButton();
        ImageIcon iconLeft = new ImageIcon ("images/left.png");
        leftjButton = new javax.swing.JButton(iconLeft);
        ImageIcon iconRight = new ImageIcon ("images/right.png");
        rightjButton = new javax.swing.JButton(iconRight);
        jButton8 = new javax.swing.JButton();
        ImageIcon iconThinner = new ImageIcon ("images/thinner.png");
        thinnerjButton = new javax.swing.JButton(iconThinner);
        jButton7 = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox<>();
        ImageIcon iconThicker = new ImageIcon ("images/thicker.png");
        thickerjButton = new javax.swing.JButton(iconThicker);
        NudgeUpJButton = new javax.swing.JButton(icon);
        reportJLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        NudgeDownJButton = new javax.swing.JButton(iconDown);
        jButton2 = new javax.swing.JButton();
        NudgeLeftJButton = new javax.swing.JButton(iconLeft);
        NudgeRightJButton = new javax.swing.JButton(iconRight);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextArea1KeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextArea1KeyReleased(evt);
            }
        });
        jScrollPane2.setViewportView(jTextArea1);

        downJButton.setText("Down");
        downJButton.setToolTipText("Move white-out line down");
        downJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downJButtonActionPerformed(evt);
            }
        });

        ProcessLineJCheckBox.setSelected(true);
        ProcessLineJCheckBox.setText("Process line as a whole");
        ProcessLineJCheckBox.setToolTipText("Process line as one instead of by squares");

        upJButton.setText("Up");
        upJButton.setToolTipText("Move white-out line up");
        upJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upJButtonActionPerformed(evt);
            }
        });

        jButton1.setText("OCR");
        jButton1.setToolTipText("Use OCR to identify clues");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        leftjButton.setText("Left");
        leftjButton.setToolTipText("Move white-out line to the left");
        leftjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leftjButtonActionPerformed(evt);
            }
        });

        rightjButton.setText("Right");
        rightjButton.setToolTipText("Move white-out line to the right");
        rightjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightjButtonActionPerformed(evt);
            }
        });

        jButton8.setText(">");
        jButton8.setToolTipText("Move to next row");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        thinnerjButton.setText("Thin");
        thinnerjButton.setToolTipText("Make white-out line thinner");
        thinnerjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thinnerjButtonActionPerformed(evt);
            }
        });

        jButton7.setText("<");
        jButton7.setToolTipText("Move to previous row");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Top", "Bottom", "Left", "Right", "Separator" }));
        jComboBox2.setToolTipText("Choose white-out element to modify");

        thickerjButton.setText("Thck");
        thickerjButton.setToolTipText("Make white-out line thicker");
        thickerjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                thickerjButtonActionPerformed(evt);
            }
        });

        NudgeUpJButton.setText("Up");
        NudgeUpJButton.setToolTipText("Move selection box up");
        NudgeUpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NudgeUpJButtonActionPerformed(evt);
            }
        });

        reportJLabel.setText("Report");

        jLabel1.setText("Nudge");

        NudgeDownJButton.setText("Down");
        NudgeDownJButton.setToolTipText("Move selection box down");
        NudgeDownJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NudgeDownJButtonActionPerformed(evt);
            }
        });

        jButton2.setText("Read");
        jButton2.setToolTipText("Read clues out loud");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        NudgeLeftJButton.setText("Left");
        NudgeLeftJButton.setToolTipText("Nudge selection box to the left");
        NudgeLeftJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NudgeLeftJButtonActionPerformed(evt);
            }
        });

        NudgeRightJButton.setText("Right");
        NudgeRightJButton.setToolTipText("Nudge selection box to the right");
        NudgeRightJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NudgeRightJButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(reportJLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(NudgeUpJButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(NudgeDownJButton))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(NudgeLeftJButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(NudgeRightJButton))))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(upJButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(leftjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(thickerjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(thinnerjButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(downJButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(rightjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton8))
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addComponent(ProcessLineJCheckBox))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(upJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(leftjButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(thickerjButton))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(downJButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rightjButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(thinnerjButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(reportJLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(NudgeUpJButton)
                    .addComponent(NudgeDownJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NudgeLeftJButton)
                    .addComponent(NudgeRightJButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                .addComponent(ProcessLineJCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton7)
                    .addComponent(jButton8))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE))
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
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
		if (cur_index < (num_cols_or_rows - 1))
		{
			cur_index++;
			ProcessCurRow(false);
		}
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
		if (cur_index > 0)
		{
			cur_index--;
			ProcessCurRow(false);
		}    }//GEN-LAST:event_jButton7ActionPerformed

    private void upJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upJButtonActionPerformed
		mySettings.UpSelected(jComboBox2.getSelectedIndex());
		ProcessCurRow (true);
    }//GEN-LAST:event_upJButtonActionPerformed

    private void downJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downJButtonActionPerformed
		mySettings.DownSelected(jComboBox2.getSelectedIndex());
		ProcessCurRow (true);
    }//GEN-LAST:event_downJButtonActionPerformed

    private void leftjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leftjButtonActionPerformed
		mySettings.LeftSelected(jComboBox2.getSelectedIndex());
		ProcessCurRow (true);
    }//GEN-LAST:event_leftjButtonActionPerformed

    private void rightjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightjButtonActionPerformed
		mySettings.RightSelected(jComboBox2.getSelectedIndex());
		ProcessCurRow (true);
    }//GEN-LAST:event_rightjButtonActionPerformed

    private void thickerjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thickerjButtonActionPerformed
		mySettings.ThickerSelected(jComboBox2.getSelectedIndex());
		ProcessCurRow (true);
    }//GEN-LAST:event_thickerjButtonActionPerformed

    private void thinnerjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_thinnerjButtonActionPerformed
		mySettings.ThinnerSelected(jComboBox2.getSelectedIndex());
		ProcessCurRow (true);
    }//GEN-LAST:event_thinnerjButtonActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		myClues.set(cur_index, null);	// This forces the code to reprocess the image
        ProcessCurRow (true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextArea1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextArea1KeyReleased
        // save any changes to the clues
		String newText = RemoveCRs(jTextArea1.getText());
		myClues.set(cur_index, newText);
    }//GEN-LAST:event_jTextArea1KeyReleased

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		ClueReaderThread clueReaderThread =  new ClueReaderThread (myClues.get(cur_index));
		clueReaderThread.start();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void NudgeUpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NudgeUpJButtonActionPerformed
        puzzle_JFrame.GetImageComponent().MoveSelectionUp();
		ic.repaint();
		startPt = puzzle_JFrame.GetStartPt();
		endPt   = puzzle_JFrame.GetEndPt();
		ProcessCurRow(false);
    }//GEN-LAST:event_NudgeUpJButtonActionPerformed

    private void NudgeDownJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NudgeDownJButtonActionPerformed
        puzzle_JFrame.GetImageComponent().MoveSelectionDown();
		ic.repaint();
		startPt = puzzle_JFrame.GetStartPt();
		endPt   = puzzle_JFrame.GetEndPt();		
		ProcessCurRow(false);
    }//GEN-LAST:event_NudgeDownJButtonActionPerformed

    private void jTextArea1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextArea1KeyPressed
        // save any changes to the clues
		String newText = RemoveCRs(jTextArea1.getText());
		myClues.set(cur_index, newText);
    }//GEN-LAST:event_jTextArea1KeyPressed

    private void NudgeLeftJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NudgeLeftJButtonActionPerformed
        puzzle_JFrame.GetImageComponent().MoveSelectionLeft();
		ic.repaint();
		startPt = puzzle_JFrame.GetStartPt();
		endPt   = puzzle_JFrame.GetEndPt();		
		ProcessCurRow(false);
    }//GEN-LAST:event_NudgeLeftJButtonActionPerformed

    private void NudgeRightJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NudgeRightJButtonActionPerformed
        puzzle_JFrame.GetImageComponent().MoveSelectionRight();
		ic.repaint();
		startPt = puzzle_JFrame.GetStartPt();
		endPt   = puzzle_JFrame.GetEndPt();		
		ProcessCurRow(false);
    }//GEN-LAST:event_NudgeRightJButtonActionPerformed

	private String RemoveCRs (String text)
	{
		String nextText = text.replace("\n", " ");
		return nextText.trim();
	}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton NudgeDownJButton;
    private javax.swing.JButton NudgeLeftJButton;
    private javax.swing.JButton NudgeRightJButton;
    private javax.swing.JButton NudgeUpJButton;
    private javax.swing.JCheckBox ProcessLineJCheckBox;
    private javax.swing.JButton downJButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
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
