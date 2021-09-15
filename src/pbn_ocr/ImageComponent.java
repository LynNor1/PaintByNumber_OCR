/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.RescaleOp;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.Color;

/**
 *
 * @author Lynne
 */
class ImageComponent extends JComponent implements Scrollable, MouseMotionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private BufferedImage image = null;
	private BufferedImage transformedImage = null;
	private BufferedImage savedImage = null;
	private int maxUnitIncrement = 1;
	private float rotationDeg = 0.0f;
	
	private boolean trackingRectangle = false;
	private Point startPt = null;
	private Point endPt = null;
	private Color rectColor = Color.RED;
	private Color lineColor = Color.CYAN;
	private ClickAndDragThread selectRectThread = null;
	private boolean lockSelection = false;
	private int numColsOrRows = 0;
	
	private Puzzle_JFrame puzzle_JFrame = null;
	
	private float scaleFactor = 1.0f;
	private float offset = 0.0f;
	private boolean isLine = false;	// If SHIFT key is down while click-and-drag, make a line instead of a rectangle!
	
    public ImageComponent(BufferedImage img, Puzzle_JFrame myFrame){
		
		puzzle_JFrame = myFrame;
		image = img;
		this.setSize (image.getWidth(), image.getHeight());
		this.setPreferredSize (new Dimension(image.getWidth(), image.getHeight()));
		
        //Let the user scroll by dragging to outside the window.
        setAutoscrolls(true); //enable synthetic drag events

		if (puzzle_JFrame != null)
		{
			// Get a Click and Drag listener on another thread
			selectRectThread = new ClickAndDragThread (this);		
			selectRectThread.start();
			addMouseMotionListener (selectRectThread);
			addMouseListener (selectRectThread);
		} else
		{
			addMouseMotionListener (this);
		}
    }
	
	public void UndoRotation ()
	{
		transformedImage = null;
		repaint();
	}
	
	private void RearrangeStartEndPts ()
	{
		if (startPt.x > endPt.x)
		{
			int save_x = startPt.x;
			startPt.x = endPt.x;
			endPt.x = save_x;
		}
		if (startPt.y > endPt.y)
		{
			int save_y = startPt.y;
			startPt.y = endPt.y;
			endPt.y = save_y;
		}
	}
	
    public void paintComponent (Graphics g){
        if(image == null) return;
		if (transformedImage != null)
			g.drawImage(transformedImage, 0, 0, this);
		else
			g.drawImage(image, 0, 0, this);
		if (startPt != null && endPt != null)
		{
			if (!isLine)
			{
				g.setColor (rectColor);
				if (lockSelection && numColsOrRows > 0 && puzzle_JFrame != null)
				{
					// rearrange Start/End pts so UL/LR
					RearrangeStartEndPts ();

					// Draw rect around each column or row
					if (puzzle_JFrame.IsColSelected())
					{
						int cols = endPt.x - startPt.x;
						double pixels_per_col = (double)cols/(double)numColsOrRows;
						int width = (int)Math.floor(pixels_per_col);
						int height = endPt.y - startPt.y;
						for (int n=0; n<numColsOrRows; n++)
						{
							double start_x = startPt.x + (double)n*pixels_per_col;
							int istart_x = (int)Math.floor(start_x);
							g.drawRect(istart_x, startPt.y, width, height);
						}
					} else
					{
						int rows = endPt.y - startPt.y;
						double pixels_per_row = (double)rows/(double)numColsOrRows;
						int height = (int)Math.floor(pixels_per_row);
						int width = endPt.x - startPt.x;
						for (int n=0; n<numColsOrRows; n++)
						{
							double start_y = startPt.y + (double)n*pixels_per_row;
							int istart_y = (int)Math.floor(start_y);
							g.drawRect(startPt.x, istart_y, width, height);
						}
					}
				} else
					g.drawRect (startPt.x, startPt.y, endPt.x-startPt.x, endPt.y-startPt.y);
			} else
			{
				g.setColor (lineColor);
				g.drawLine (startPt.x, startPt.y, endPt.x, endPt.y);
			}
		}
    }
	
	public void setTempImage (BufferedImage img)
	{
		savedImage = image;
		image = img;
		transformedImage = null;
		this.setSize (image.getWidth(), image.getHeight());
		this.setPreferredSize (new Dimension(image.getWidth(), image.getHeight()));
			
	}
	
	public void restoreOldImage ()
	{
		image = savedImage;
		transformedImage = null;
		this.setSize (image.getWidth(), image.getHeight());
		this.setPreferredSize (new Dimension(image.getWidth(), image.getHeight()));		
	}
	
	public void setRotationDeg (float new_rot_deg)
	{
		rotationDeg = new_rot_deg;
		ApplyAllTransforms ();
	}
	
	public void incrementRotationDeg (float incr_deg)
	{
		rotationDeg += incr_deg;
		ApplyAllTransforms ();
	}
	
	public void increaseContrast ()
	{
		scaleFactor += 0.1f;
		if (scaleFactor > 2.0f) scaleFactor = 2.0f;
		applyContrast();
	}
	public void decreaseContrast ()
	{
		scaleFactor -= 0.1f;
		if (scaleFactor < 0.1f) scaleFactor = 0.1f;
		applyContrast();
	}
	
	
	private void applyContrast()
	{
		BufferedImage bimg = image;		
		if (transformedImage != null)
			bimg = transformedImage;

		RescaleOp rescale = new RescaleOp (scaleFactor, offset, null);

		BufferedImage bimgDest = new BufferedImage(bimg.getWidth(), bimg.getHeight(),
			bimg.getType());
		rescale.filter (bimg, bimgDest);
		
		if (transformedImage != null)
			transformedImage = bimgDest;
		else
			image = bimgDest;
		
		this.repaint();
	}
	
	private void ApplyAllTransforms()
	{
		if (rotationDeg == 0.0f)
		{
			transformedImage = null;
		} else
		{
			// Apply rotation
			double rads = Math.toRadians(rotationDeg);
			double sin = Math.abs(Math.sin(rads));
			double cos = Math.abs(Math.cos(rads));
			int w = (int) Math.floor(image.getWidth() * cos + image.getHeight() * sin);
			int h = (int) Math.floor(image.getHeight() * cos + image.getWidth() * sin);
			transformedImage = new BufferedImage(w, h, image.getType());
			AffineTransform at = new AffineTransform();
			at.translate(w / 2, h / 2);
			at.rotate(rads,0, 0);
			at.translate(-image.getWidth() / 2, -image.getHeight() / 2);
			AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			rotateOp.filter(image,transformedImage);			
		}
	}
	
	public void setTrackingRectangle (boolean b)
	{ trackingRectangle = b; }
	public boolean isTrackingRectangle ()
	{ return trackingRectangle; }
	public void setStartPt (Point p, boolean isShift)
	{ 
		startPt = p; 
		isLine = isShift;
	}
	public void setEndPt (Point p)
	{ endPt = p; }
	
	public Point getStartPt ()
	{ return startPt; }
	public Point getEndPt ()
	{ return endPt; }
	public boolean getIsLine()
	{ return isLine; }
	
	public void clearSelection ()
	{
		isLine = false;
		startPt = null;
		endPt = null;
		trackingRectangle = false;
	}
	
	public void setLockSelection (boolean b)
	{ 
		lockSelection = b; 
		if (lockSelection && puzzle_JFrame != null)
		{
			int num = puzzle_JFrame.getNumColsOrRows();
			NotifyNewNumColsOrRows (num);
		} else
			this.repaint();
	}
	public boolean isLockSelected ()
	{ return lockSelection; }
	
	public void NotifyNewNumColsOrRows (int new_num)
	{ 
		numColsOrRows = new_num;
		this.repaint();
	}
	
	public BufferedImage GetSelectionByIndex (int index)
	{
		int istart_x, istart_y, width, height;
		// Draw rect around each column or row
		if (puzzle_JFrame.IsColSelected())
		{
			int cols = endPt.x - startPt.x;
			double pixels_per_col = (double)cols/(double)numColsOrRows;
			width = (int)Math.floor(pixels_per_col);
			height = endPt.y - startPt.y;
			double start_x = startPt.x + (double)index*pixels_per_col;
			istart_x = (int)Math.floor(start_x);
			istart_y = startPt.y;
			istart_x = istart_x + 2;
			width = width - 2;
		} else
		{
			int rows = endPt.y - startPt.y;
			double pixels_per_row = (double)rows/(double)numColsOrRows;
			height = (int)Math.floor(pixels_per_row);
			width = endPt.x - startPt.x;
			double start_y = startPt.y + (double)index*pixels_per_row;
			istart_y = (int)Math.floor(start_y);
			istart_x = startPt.x;
			istart_y = istart_y + 2;
			height = height - 2;
		}
		BufferedImage inputImg = this.image;
		if (this.transformedImage != null) inputImg = this.transformedImage;
		BufferedImage selImg = inputImg.getSubimage (istart_x, istart_y, width, height);
		
		/*
		
		// Clean up edges with white
		Graphics g = selImg.getGraphics();
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.WHITE);
		g2.setStroke (new BasicStroke (1));
		g2.drawRect (0, 0, width, height);
		
		// Clean up lines between clues
		int maxNumClues = puzzle_JFrame.getMaxNumCluesPerColOrRow();
		if (maxNumClues > 0)
		{
			if (puzzle_JFrame.IsColSelected())
			{
				float rows_per_clue = (float)height / (float)maxNumClues;
				g2.setStroke (new BasicStroke (3));				
				for (int c=0; c<maxNumClues; c++)
				{
					int iy = (int)Math.floor(c*rows_per_clue);
					g2.drawLine(0, iy-1, width, iy-1);
				}
			} else
			{				
				float cols_per_clue = (float)width / (float)maxNumClues;
				g2.setStroke (new BasicStroke (3));				
				for (int c=0; c<maxNumClues; c++)
				{
					int ix = (int)Math.floor(c*cols_per_clue);
					g2.drawLine(ix-1, 0, ix-1, height);
				}
			}
		}
		*/
		
		return selImg;
	}
	
	public void NotifyNewImage (BufferedImage newImage)
	{
		image = newImage;
		transformedImage = null;
		this.setSize (image.getWidth(), image.getHeight());
		this.setPreferredSize (new Dimension(image.getWidth(), image.getHeight()));				
		repaint();
	}
	
	//Methods required by the MouseMotionListener interface:
    public void mouseMoved(MouseEvent e) 
	{ }
    public void mouseDragged(MouseEvent e) {
        //The user is dragging us, so scroll!
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);
    }
 
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(320, 480);
        } else {
            return super.getPreferredSize();
        }
    }
 
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
 
    public int getScrollableUnitIncrement(Rectangle visibleRect,
                                          int orientation,
                                          int direction) {
        //Get the current position.
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
        } else {
            currentPosition = visibleRect.y;
        }
 
        //Return the number of pixels between currentPosition
        //and the nearest tick mark in the indicated direction.
        if (direction < 0) {
            int newPosition = currentPosition -
                             (currentPosition / maxUnitIncrement)
                              * maxUnitIncrement;
            return (newPosition == 0) ? maxUnitIncrement : newPosition;
        } else {
            return ((currentPosition / maxUnitIncrement) + 1)
                   * maxUnitIncrement
                   - currentPosition;
        }
    }
 
    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation,
                                           int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width - maxUnitIncrement;
        } else {
            return visibleRect.height - maxUnitIncrement;
        }
    }
 
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }
 
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
 
    public void setMaxUnitIncrement(int pixels) {
        maxUnitIncrement = pixels;
    }

}