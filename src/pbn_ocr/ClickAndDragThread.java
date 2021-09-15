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

	public ClickAndDragThread (ImageComponent the_ic)
	{
		ic = the_ic;
	}
	
	public void run ()
	{
		while (true) {}
	}
	
	// MouseListener methods
	public void mouseClicked(MouseEvent me)
	{}
	public void mouseEntered(MouseEvent me)
	{ }
	public void mouseExited(MouseEvent me)
	{}
	public void mousePressed(MouseEvent me)
	{
		isShift = (me.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
		if (!ic.isLockSelected())
		{
			ic.setTrackingRectangle (true);
			ic.setStartPt (me.getPoint(), isShift);
			ic.repaint();
		}
	}
	public void mouseReleased(MouseEvent me)
	{
		if (!ic.isLockSelected())
		{
			ic.setEndPt (me.getPoint());
			ic.setTrackingRectangle (false);
			ic.repaint();	
		}
	}	

	// MouseMotionListener method
    public void mouseMoved(MouseEvent e) 
	{
		if (!ic.isLockSelected() && ic.isTrackingRectangle())
		{
			ic.setEndPt (e.getPoint());
			ic.repaint();
		}
	}	
    public void mouseDragged(MouseEvent e) {
        //The user is dragging us, so scroll!
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
		if (!ic.isLockSelected() && ic.isTrackingRectangle())
		{
			ic.setEndPt (e.getPoint());
			ic.repaint();
		}
		ic.scrollRectToVisible(r);
    }
	
}
