/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
/**
 *
 * @author Lynne
 */
public class ClickAndDragThread extends Thread implements MouseListener, MouseMotionListener {

	private ImageComponent ic;
	private boolean isShift = false;
	private boolean isCtrl = false;
	private boolean collectingCorners = false;
	private int cornerCount = 0;
	private boolean clickAndDrag = false;

	public ClickAndDragThread (ImageComponent the_ic)
	{
		ic = the_ic;
	}
	
	private void resetCollectingCorners ()
	{
		ic.resetCornerCount ();
		collectingCorners = false;
//		System.out.println ("collecting corners reset");
	}
	private void resetClickAndDrag ()
	{
		ic.setTrackingRectangle (false);
		ic.setStartPt (null, false);
		ic.setEndPt (null);
		clickAndDrag = false;
	}
	
	public void run ()
	{
		resetCollectingCorners();
		resetClickAndDrag();
		while (true) {}
	}
	
	// MouseListener methods
	public void mouseClicked(MouseEvent me)
	{
		/*
		if (ic.isLockSelected()) return;
		isShift = (me.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
		isCtrl = (me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;		
		if (isCtrl && !isShift)
		{
			if (cornerCount == 0) resetClickAndDrag();
			if (cornerCount < 4)
			{
				ic.setCornerPt (me.getPoint(), cornerCount);
				ic.repaint();
				cornerCount++;
				collectingCorners = true;
//				System.out.println ("Added corner # " + cornerCount);
				if (cornerCount == 4) collectingCorners = false;
			}		
		}
		*/
	}
	public void mouseEntered(MouseEvent me)
	{ }
	public void mouseExited(MouseEvent me)
	{}
	public void mousePressed(MouseEvent me)
	{
		if (ic.isLockSelected()) return;
		isShift = (me.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
		isCtrl = (me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;		
		if (!isCtrl)
		{
			resetCollectingCorners();
			resetClickAndDrag();
			ic.setTrackingRectangle (true);
			ic.setStartPt (me.getPoint(), isShift);
			ic.repaint();
			clickAndDrag = true;
		} else
		{
			if (!collectingCorners) 
			{
				resetClickAndDrag();
				resetCollectingCorners();
			}
			ic.setCornerPt(me.getPoint(), cornerCount);
			cornerCount++;
			ic.repaint();
			collectingCorners = true;
		}
	}
	public void mouseReleased(MouseEvent me)
	{
		if (ic.isLockSelected()) return;
		if (clickAndDrag)
		{
			ic.setEndPt (me.getPoint());
			ic.setTrackingRectangle (false);
			ic.repaint();	
			clickAndDrag = false;
		} else if (collectingCorners)
		{
			ic.setCornerPt (me.getPoint(), cornerCount);
			cornerCount++;
			ic.repaint();
			if (cornerCount == 4) collectingCorners = false;
		}
	}	

	// MouseMotionListener method
    public void mouseMoved(MouseEvent e) 
	{
		if (ic.isLockSelected()) return;
		if (clickAndDrag)
		{
			ic.setEndPt (e.getPoint());
			ic.repaint();
		} else if (collectingCorners)
		{
//			System.out.println ("Updating corner " + (cornerCount+1));
			ic.setCornerPt (e.getPoint(), cornerCount);			
			ic.repaint();				
		}
	}	
    public void mouseDragged(MouseEvent e) {
		if (ic.isLockSelected()) return;
        //The user is dragging us, so scroll!
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
		if (clickAndDrag)
		{
			ic.setEndPt (e.getPoint());
			ic.repaint();
		} else if (collectingCorners)
		{
			ic.setCornerPt(e.getPoint(), cornerCount);
			ic.repaint();
		}
		ic.scrollRectToVisible(r);
    }
	
}
