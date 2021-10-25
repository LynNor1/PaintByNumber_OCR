# PaintByNumber_OCR
Java program to scan a Nonogram .tiff and assist the user in extracting the puzzle clues using OCR.  This program was developed on an Intel 2016 MacBook Pro using Java 8 and NetBeans 8.2.

# Installing Tesseract OCR for Mac / Java
Tesseract OCR is used for extracting the puzzle clues from the scanned Nonogram.  I believe I downloaded it from SourceForge here: [SourceForge Tess4J download](https://sourceforge.net/projects/tess4j/) and do not recall having any issues getting my Java program to use it.  The download includes the native libraries needed for the Mac as well as the Java interface.

Once you download the Tess4J package, you'll see the following folders:

- dist (contains the .jar file you need)
- lib (contains the binaries you need for your platform)
- nbproject
- src
- tessdata (contains the English training data you need)
- test

The Tess4J folder is configured as a NetBeans project!  In NetBeans, you can load the Tess4J folder as a project.  Right-click on the project icon and select "Test..." to run the JUnit tests.  If your code is having trouble finding the native DLLs, you need to add `-Dtest-sys-prop.java.library.path=lib/win32-x86-64` for the JUnit tests to run properly.  For your own code, you need to add `-Djava.library.path=lib/win32-x86-64` to the "Run" "VM Options:" in the Project Properties.  (This is **NOT** working for me right now on my PC!  And on my Mac, I didn't specify the library path at all! Eh?)

In order for your code to find the Tesseract training data, also add `-DTESSDATA_PREFIX=/usr/local/Cellar/tesseract/4.1.1/share/tessdata` to your "Run" "VM Options:".  Obviously, you need to point to your particular tessdata folder.

Here is a quick example of how to use Tesseract OCR in Java:

```
package sample_program;

import java.awt.image.BufferedImage;
import java.awt.FileDialog;
import java.awt.Graphics;
import java.io.FilenameFilter;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class Tess4J_Example {
    
    public Tess4J_Example()
    {
        // ---- Have user select a TIFF image ----
        FileDialog fd = new FileDialog((java.awt.Frame) null, "Select a TIFF image for OCR", FileDialog.LOAD);
        fd.setFilenameFilter (new FilenameFilter ()
        {
            public boolean accept (File f, String name)
            {
                String extension = name.substring(name.lastIndexOf("."));
                if (extension == null) return false;
                extension = extension.toLowerCase();
                if (extension.compareTo (".tiff") == 0 || extension.compareTo (".tif") == 0) return true;
                return false;
            }
        });
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        File f = new File(fd.getDirectory(), fd.getFile());
		
        // ---- Read image ----
        BufferedImage color_img = null, grey_img = null;
        try
        { color_img = ImageIO.read(f); } 
        catch (IOException ie)
        {
            System.out.println ("IOException: " + ie.getLocalizedMessage());
            return;
        }        
        if (color_img == null) return;
		
        // ---- Convert to greyscale ----
        grey_img = new BufferedImage(color_img.getWidth(), color_img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);  
        Graphics g = grey_img.getGraphics();  
        g.drawImage(color_img, 0, 0, null);  
        g.dispose();  		
		
        // ---- Set up Tesseract and run the OCR ----
        Tesseract tess = new Tesseract();
        tess.setDatapath("C:\\Users\\newbly\\OneDrive - General Atomics\\Documents\\Tess4J\\tessdata");	
        tess.setTessVariable("tessedit_char_whitelist", "0123456789");
        try
        {
            String ocr_out = tess.doOCR (grey_img);      
            System.out.println ("OCR output: " + ocr_out);
        }
        catch (TesseractException tioe)
        { System.out.println ("TesseractException: " + tioe.getLocalizedMessage()); }
    }
    
    /*======================================*
    * Entry Point                          *
    *======================================*/

    public static void main(String[] args)
    {   
        Tess4J_Example te = new Tess4J_Example();
    }
}
```

# Installing OpenCV for Mac / Java
Getting OpenCV installed on my Intel MacBook Pro was a little more difficult.  You can use homebrew to install OpenCV, but it no longer supports building the Java interfaces.  So I ended up installing OpenCV from source and building it locally on my machine.  I used instructions from here: [OpenCV 4 with Java instructions](https://delabassee.com/OpenCVJava/).
