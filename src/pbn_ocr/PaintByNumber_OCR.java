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
	static String TessDataLocationDefault = "/usr/local/Cellar/tesseract/4.1.1/share/tessdata";
	static String TessDataPath;
/**
 * @param args the command line arguments
 */
public static void main(String[] args) {

    try {
		
		TessDataPath = System.getProperty("tessdatapath", TessDataLocationDefault);
		
		// Let's get the ImageIO formats we can read
		final String[] formats = ShowImageIOInfo();
		
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
                for (int i=0; i<formats.length; i++)
                    if (extension.compareTo ("." + formats[i]) == 0) return true;
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

   public static String[] ShowImageIOInfo () {
        String[] formats = ImageIO.getReaderFormatNames();
//        for (int i = 0; i < formats.length; ++i) {
//          System.out.println("reader " + formats[i]);
//        }

//        String[] names = ImageIO.getWriterFormatNames();
//        for (int i = 0; i < names.length; ++i) {
//          System.out.println("writer " + names[i]);
//        }
        
        return formats;
    }        

}


