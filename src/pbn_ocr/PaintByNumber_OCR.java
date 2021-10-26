/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FilenameFilter;
import java.awt.image.BufferedImage;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Graphics;
import javax.swing.JFrame;
import org.opencv.core.Core;
/**
 *
 * @author Lynne
 */
public class PaintByNumber_OCR {

	
    static BufferedImage img = null;
	static String name = "unknown";
/**
 * @param args the command line arguments
 */
public static void main(String[] args) {

    try {
		
		// Let's try loading in openCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		// Have user select a TIFF image
		FileDialog fd = new FileDialog((java.awt.Frame) null, "Select an image of your puzzle", FileDialog.LOAD);
		fd.setFilenameFilter (new FilenameFilter ()
		{
			public boolean accept (File f, String name)
			{
				String extension = name.substring(name.lastIndexOf("."));
				if (extension == null) return false;
				extension = extension.toLowerCase();
				if (extension.compareTo (".tiff") == 0 ||
					extension.compareTo (".tif") == 0 ||
					extension.compareTo (".jpg")	== 0 ||
					extension.compareTo (".jpeg") == 0 ||
					extension.compareTo (".png") == 0) return true;
				return false;
			}
		});
		fd.setVisible(true);
		if (fd.getFile() == null) return;
		File f = new File(fd.getDirectory(), fd.getFile());
		name = f.getName();
		
		// Read image
		BufferedImage color_img = ImageIO.read(f);
		if (color_img == null) return;
		
		// Convert to greyscale
		img = new BufferedImage(color_img.getWidth(), color_img.getHeight(),  
			BufferedImage.TYPE_BYTE_GRAY);  
		Graphics g = img.getGraphics();  
		g.drawImage(color_img, 0, 0, null);  
		g.dispose();  		
	
		EventQueue.invokeLater(new Runnable()
		{
			public void run(){
				Puzzle_JFrame frame = new Puzzle_JFrame(img, name, f);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setVisible(true);
			}
		}
		);	
		
    } catch (IOException e) {
        e.printStackTrace();
	}
}

}


