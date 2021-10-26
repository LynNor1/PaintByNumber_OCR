/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.event.*;
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
//	private BufferedImage savedImage = null;
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
//		savedImage = image;
		image = img;
		transformedImage = null;
		this.setSize (image.getWidth(), image.getHeight());
		this.setPreferredSize (new Dimension(image.getWidth(), image.getHeight()));
			
	}
	
	// This doesn't work because savedImage and image point to the same object.
	public void restoreOldImage ()
	{
//		image = savedImage;
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
		applyContrast(true);
	}
	public void brighten ()
	{
		applyContrast(false);
	}	
	
	// if do_contrast, then brighten values > 127 and darken values < 127
	// if !do_contrast, then brighten values > 127 only
	private void applyContrast(boolean do_contrast)
	{				
		BufferedImage bimg = image;		
		if (transformedImage != null)
			bimg = transformedImage;
		
		int modification = 10;
		
		byte[] pixels = ((DataBufferByte) bimg.getRaster().getDataBuffer()).getData();	
		
		int mid_point = puzzle_JFrame.GetMidPoint();
		
		for (int i=0; i<pixels.length; i++)
		{
			int np = Byte.toUnsignedInt(pixels[i]);
			if (np > mid_point)
			{
				np += modification;
				if (np > 255) np = 255;
			} else if (do_contrast)
			{
				np -= modification;
				if (np < 0) np = 0;		
			}
			pixels[i] = (byte)np;
		}
		
		if (transformedImage != null)
			transformedImage = bimg;
		else
			image = bimg;		
		
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
	
	public Rectangle GetSelectionRectangle ()
	{
		int smallX = startPt.x < endPt.x ? startPt.x : endPt.x;
		int smallY = startPt.y < endPt.y ? startPt.y : endPt.y;
		int width = endPt.x - startPt.x;
		int height = endPt.y - startPt.y;
		if (width < 0) width = -width;
		if (height < 0) height = -height;
		Rectangle r = new Rectangle (smallX, smallY, width, height);
		return r;
	}
	
	public BufferedImage GetSelectionByIndex (int index, Point start, Point end, int num, boolean is_row)
	{
		int istart_x, istart_y, width, height;
		// Draw rect around each column or row
		if (!is_row)
		{
			int cols = end.x - start.x;
			double pixels_per_col = (double)cols/(double)num;
			width = (int)Math.floor(pixels_per_col);
			height = end.y - start.y;
			double start_x = start.x + (double)index*pixels_per_col;
			istart_x = (int)Math.floor(start_x);
			istart_y = start.y;
			istart_x = istart_x + 2;
			width = width - 2;
		} else
		{
			int rows = end.y - start.y;
			double pixels_per_row = (double)rows/(double)num;
			height = (int)Math.floor(pixels_per_row);
			width = end.x - start.x;
			double start_y = start.y + (double)index*pixels_per_row;
			istart_y = (int)Math.floor(start_y);
			istart_x = start.x;
			istart_y = istart_y + 2;
			height = height - 2;
		}
		BufferedImage inputImg = this.image;
		if (this.transformedImage != null) inputImg = this.transformedImage;
		BufferedImage selImg = inputImg.getSubimage (istart_x, istart_y, width, height);
		
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