/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;

/**
 *
 * @author Lynne
 */
public class ProcessSettings {

	private int[] StrokeWidths = new int[5];
	private int[] Offsets = new int[5];
	private boolean isColumn = true;
	private int maxNumClues;
	
	// These correspond to the combo box in ProcessCol_JFrame or ProcessRow_JFrame
	private final int TOP = 0;
	private final int BOTTOM = 1;
	private final int LEFT = 2;
	private final int RIGHT = 3;
	private final int SEPARATOR = 4;
	
	public ProcessSettings (Boolean procColumn,	int maxClues)
	{
		isColumn = procColumn;
		maxNumClues = maxClues;
		
		StrokeWidths[TOP] = 2;
		StrokeWidths[BOTTOM] = 2;
		StrokeWidths[LEFT] = 1;
		StrokeWidths[RIGHT] = 1;
		StrokeWidths[SEPARATOR] = 3;
		
		Offsets[TOP] = 0;
		Offsets[BOTTOM] = 0;
		Offsets[LEFT] = 0;
		Offsets[RIGHT] = 0;
		Offsets[SEPARATOR] = -1;
	}
	
	public void UpSelected (int whichone)
	{
		Offsets[whichone] -= 1;
	}
	public void DownSelected (int whichone)
	{
		Offsets[whichone] += 1;
	}
	public void LeftSelected (int whichone)
	{
		Offsets[whichone] -= 1;
	}
	public void RightSelected (int whichone)
	{
		Offsets[whichone] += 1;
	}
	public void ThickerSelected (int whichone)
	{
		StrokeWidths[whichone] += 1;
	}
	public void ThinnerSelected (int whichone)
	{
		if (StrokeWidths[whichone] > 0)
		StrokeWidths[whichone] -= 1;
	}
	public BufferedImage ApplySettings (BufferedImage img, int maxNumClues)
	{
		int width = img.getWidth();
		int height = img.getHeight();
		
		BufferedImage newImg = ProcessCol_JFrame.cloneImage(img);
		
		Graphics g = newImg.getGraphics();
		Graphics2D g2d = (Graphics2D)g;
		
		g2d.setColor(Color.WHITE);
		
		// Draw top edge
		if (StrokeWidths[TOP] > 0)
		{
			g2d.setStroke (new BasicStroke (StrokeWidths[TOP]));
			g2d.drawLine (0, Offsets[TOP], width, Offsets[TOP]);
		}
		if (StrokeWidths[BOTTOM] > 0)
		{
			g2d.setStroke (new BasicStroke (StrokeWidths[BOTTOM]));
			g2d.drawLine (0, height + Offsets[BOTTOM] - 1, width, height + Offsets[BOTTOM] -1);			
		}
		if (StrokeWidths[LEFT] > 0)
		{
			g2d.setStroke (new BasicStroke (StrokeWidths[LEFT]));
			g2d.drawLine (Offsets[LEFT], 0, Offsets[LEFT], height);
		}
		if (StrokeWidths[RIGHT] > 0)
		{
			g2d.setStroke (new BasicStroke (StrokeWidths[RIGHT]));
			g2d.drawLine (width + Offsets[RIGHT]  - 1, 0, width + Offsets[RIGHT] - 1, height);
		}
		
		if (StrokeWidths[SEPARATOR] > 0 && maxNumClues > 0)
		{	
			g2d.setStroke (new BasicStroke (StrokeWidths[SEPARATOR]));
			if (isColumn)
			{
				float pixels_per_clue = (float)height / (float)maxNumClues;
				for (int i=0; i<(maxNumClues-1); i++)
				{
					float y = (float)(i+1)*pixels_per_clue;
					int iy = (int)Math.floor(y);
					g2d.drawLine (0, iy+Offsets[SEPARATOR], width, iy+Offsets[SEPARATOR]);
				}
			} else
			{		
				float pixels_per_clue = (float)width / (float)maxNumClues;	
				for (int i=0; i<(maxNumClues-1); i++)
				{
					float x = (float)(i+1)*pixels_per_clue;
					int ix = (int)Math.floor(x);
					g2d.drawLine (ix+Offsets[SEPARATOR], 0, ix+Offsets[SEPARATOR], height);
				}
				
			}
		}
		
		return newImg;
	}
}
