/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import javax.swing.*;
import java.awt.Image;
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
import java.util.ArrayList;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;

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
	
	private Mat homography = null;
	
	private boolean trackingRectangle = false;
	private Point startPt = null;
	private Point endPt = null;
	private Color rectColor = Color.RED;
	private Color lineColor = Color.CYAN;
	private ClickAndDragThread selectRectThread = null;
	private boolean lockSelection = false;
	private int numColsOrRows = 0;
	private Point selectedCluesStart = null;
	private Point selectedCluesEnd = null;
	
	private boolean trackingCorners = false;
	private Point[] cornerPts = new Point[4];
	private int cornersCollected = 0;
	private Color cornersColor = Color.MAGENTA;
	
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
				int num_clues = puzzle_JFrame.getMaxNumCluesPerColOrRow();
				if (lockSelection && numColsOrRows > 0 && puzzle_JFrame != null)
				{
					// rearrange Start/End pts so UL/LR
					RearrangeStartEndPts ();

					// Draw rect around each column or row
					int cols = endPt.x - startPt.x;
					int rows = endPt.y - startPt.y;					
					if (puzzle_JFrame.IsColSelected())
					{
						double pixels_per_col = (double)cols/(double)numColsOrRows;
						double pixels_per_clue = (double)rows/(double)num_clues;
						int width = (int)Math.floor(pixels_per_col);
						int height = endPt.y - startPt.y;
						for (int n=0; n<numColsOrRows; n++)
						{
							double start_x = startPt.x + (double)n*pixels_per_col;
							int istart_x = (int)Math.floor(start_x);
							g.drawRect(istart_x, startPt.y, width, height);
						}
						for (int n=0; n<num_clues; n++)
						{
							double start_y = startPt.y + (double)n*pixels_per_clue;
							int istart_y = (int)Math.floor(start_y);
							g.drawLine (startPt.x, istart_y, endPt.x, istart_y);
						}
					} else
					{
						double pixels_per_row = (double)rows/(double)numColsOrRows;
						double pixels_per_clue = (double)cols/(double)num_clues;
						int height = (int)Math.floor(pixels_per_row);
						int width = endPt.x - startPt.x;
						for (int n=0; n<numColsOrRows; n++)
						{
							double start_y = startPt.y + (double)n*pixels_per_row;
							int istart_y = (int)Math.floor(start_y);
							g.drawRect(startPt.x, istart_y, width, height);
						}
						for (int n=0; n<num_clues; n++)
						{
							double start_x = startPt.x + (double)n*pixels_per_clue;
							int istart_x = (int)Math.floor(start_x);
							g.drawLine (istart_x, startPt.y, istart_x, endPt.y);
						}						
					}
					if (selectedCluesStart != null && selectedCluesEnd != null)
					{
						int width  = selectedCluesEnd.x - selectedCluesStart.x;
						int height = selectedCluesEnd.y - selectedCluesStart.y;
						g.setColor (Color.CYAN);
						g.drawRect (selectedCluesStart.x-1, selectedCluesStart.y-1, width+1, height+1);
					}
				} else
					g.drawRect (startPt.x, startPt.y, endPt.x-startPt.x, endPt.y-startPt.y);
			} else
			{
				g.setColor (lineColor);
				g.drawLine (startPt.x, startPt.y, endPt.x, endPt.y);
			}
		} else if (cornersCollected > 1)
		{
			g.setColor (cornersColor);
			for (int i=0; i<cornersCollected-1; i++)
				g.drawLine (cornerPts[i].x, cornerPts[i].y, cornerPts[i+1].x, cornerPts[i+1].y);
			if (cornersCollected >= 4)
				g.drawLine (cornerPts[3].x, cornerPts[3].y, cornerPts[0].x, cornerPts[0].y);
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
	
	public void scaleImage (int scalePercent)
	{
		BufferedImage img = image;		
		if (transformedImage != null)
			img = transformedImage;
		
		int scaledWidth = img.getWidth() * scalePercent / 100;
		int scaledHeight = (int) (img.getHeight() * ( (double) scaledWidth / img.getWidth() ));
		
		/* The following code does NOT work yet but is left in in case
		// I want to add some restrictions of min and max sizes		
		
		int maxHeight = 2500;
		int maxWidth = 2500;
		int minHeight = 200;
		int minWidth = 200;		

		if (scaledHeight > maxHeight) {
			scaledHeight = maxHeight;
			scaledWidth= (int) (img.getWidth() * ( (double) scaledHeight/ img.getHeight() ));

			if (scaledWidth > maxWidth) {
				scaledWidth = maxWidth;
				scaledHeight = maxHeight;
			}
		}
		
		if (scaledHeight < minHeight) {
			scaledHeight = minHeight;
			scaledWidth= (int) (img.getWidth() * ( (double) scaledHeight/ img.getHeight() ));

			if (scaledWidth < minWidth) {
				scaledWidth = minWidth;
				scaledHeight = maxHeight;
			}
		}
		*/
		
		Image resized =  img.getScaledInstance( scaledWidth, scaledHeight, BufferedImage.SCALE_AREA_AVERAGING);
		BufferedImage buffered = new BufferedImage(scaledWidth, scaledHeight, img.getType());
		buffered.getGraphics().drawImage(resized, 0, 0 , this);		
		transformedImage = buffered;
		
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
	
	// input points are assumed to be *after* rotation applied
	// output points are assumed to also be *after* rotation applied
	public void setHomography (float rot_deg, Point[] inPts, Point[] outPts)
	{
		rotationDeg = rot_deg;
		
		ArrayList<org.opencv.core.Point> inPtsList = new ArrayList();
		ArrayList<org.opencv.core.Point> outPtsList = new ArrayList();
		double[] xy = new double[2];
		for (int i=0; i<4; i++)
		{
			xy[0] = inPts[i].x;
			xy[1] = inPts[i].y;
			inPtsList.add(new org.opencv.core.Point(xy));
//			System.out.println ("Input pt " + i + ") " + inPts[i].x + " " + inPts[i].y);
		}
//		System.out.println ("");
		for (int i=0; i<4; i++)
		{
			xy[0] = outPts[i].x;
			xy[1] = outPts[i].y;
			outPtsList.add(new org.opencv.core.Point(xy));
//			System.out.println ("Input pt " + i + ") " + outPts[i].x + " " + outPts[i].y);	
		}
		
		MatOfPoint2f srcPts = new MatOfPoint2f();
		srcPts.fromList(inPtsList);
		MatOfPoint2f destPts = new MatOfPoint2f();
		destPts.fromList(outPtsList);
		homography = Calib3d.findHomography (srcPts, destPts);
				
		// now convert the outPts to a startPt and endPt for a rectangular selection
		// area (and we're going to rely on the fact that the input points were
		// sorted from smallest to biggest X)
		Point sPt = outPts[0];
		int iopposite = 2;
		if (Math.abs(sPt.y - outPts[iopposite].y) < 10)
			iopposite = 3;
		Point ePt = outPts[iopposite];
		if (sPt.y > ePt.y)
		{
			sPt = outPts[1];
			iopposite = 3;
			if (Math.abs(sPt.y - outPts[iopposite].y) < 10)
				iopposite = 2;
			ePt = outPts[iopposite];
		}
		this.resetCornerCount();
		this.setStartPt (sPt, false);
		this.setEndPt (ePt);		
		
		ApplyAllTransforms();
		
	}
	
	private void ApplyAllTransforms()
	{
		BufferedImage startImage = image;
		if (transformedImage != null) startImage = transformedImage;
		
		if (rotationDeg == 0.0f)
		{
			// do nothing
		} else
		{
			// Apply rotation
			double rads = Math.toRadians(rotationDeg);
			double sin = Math.abs(Math.sin(rads));
			double cos = Math.abs(Math.cos(rads));
			int w = (int) Math.floor(startImage.getWidth() * cos + startImage.getHeight() * sin);
			int h = (int) Math.floor(startImage.getHeight() * cos + startImage.getWidth() * sin);
			transformedImage = new BufferedImage(w, h, startImage.getType());
			AffineTransform at = new AffineTransform();
			at.translate(w / 2, h / 2);
			at.rotate(rads,0, 0);
			at.translate(-startImage.getWidth() / 2, -startImage.getHeight() / 2);
			AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			rotateOp.filter(startImage,transformedImage);
			
			startImage = transformedImage;
		}
		
		// Apply homography
		if (homography != null)
		{
			
			byte[] pixels = ((DataBufferByte) startImage.getRaster().getDataBuffer()).getData();
			Mat inputMat = new Mat(startImage.getHeight(), startImage.getWidth(), CvType.CV_8UC1);
			inputMat.put (0, 0, pixels);	
			
			Mat outputMat = new Mat();
			Imgproc.warpPerspective(inputMat, outputMat, homography, inputMat.size());			
			
			// Create an empty image in matching format
			BufferedImage warpedImage = new BufferedImage(outputMat.width(), outputMat.height(), BufferedImage.TYPE_BYTE_GRAY);

			// Get the BufferedImage's backing array and copy the pixels directly into it
			byte[] data = ((DataBufferByte) warpedImage.getRaster().getDataBuffer()).getData();
			outputMat.get(0, 0, data);	
			
			transformedImage = warpedImage;
		}
	}
	
	public BufferedImage getCurrentImage ()
	{
		if (transformedImage != null) return transformedImage;
		else return image;
	}
	
	public void setCornerPt (Point p, int which)
	{
		if (which > 3) return;
		cornerPts[which] = p;
		cornersCollected = which+1;
	}
	public void resetCornerCount ()
	{ cornersCollected = 0; }
	public int getCornersCollected ()
	{ return cornersCollected; }
	public Point[] getCorners ()
	{ return cornerPts; }
	
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
		if (!lockSelection)
		{
			selectedCluesStart = null;
			selectedCluesEnd = null;
		}
		if (lockSelection && puzzle_JFrame != null)
		{
			int num = puzzle_JFrame.getNumColsOrRows();
			NotifyNewNumColsOrRows (num);
		} else
			this.repaint();
	}
	public boolean isLockSelected ()
	{ return lockSelection; }
	
	public void MoveSelectionUp ()
	{
		int step = 1;
		startPt.y -= step;
		endPt.y -= step;
		selectedCluesStart.y -= step;
		selectedCluesEnd.y -= step;
	}
	
	public void MoveSelectionDown ()
	{
		int step = 1;
		startPt.y += step;
		endPt.y += step;
		selectedCluesStart.y += step;
		selectedCluesEnd.y += step;
	}
	public void MoveSelectionLeft ()
	{
		int step = 1;
		startPt.x -= step;
		endPt.x -= step;
		selectedCluesStart.x -= step;
		selectedCluesEnd.x -= step;
	}
	
	public void MoveSelectionRight ()
	{
		int step = 1;
		startPt.x += step;
		endPt.x += step;
		selectedCluesStart.x += step;
		selectedCluesEnd.x += step;
	}
	
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
		selectedCluesStart = new Point (istart_x, istart_y);
		selectedCluesEnd   = new Point (istart_x + width, istart_y + height);
		this.repaint();
		
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