# PaintByNumber_OCR
Java program to scan a Nonogram .tiff and assist the user in extracting the puzzle clues using OCR.  This program was developed on an Intel 2016 MacBook Pro using Java 8 and NetBeans 8.2.

# Installing Tesseract OCR for Mac / Java
Tesseract OCR is used for extracting the puzzle clues from the scanned Nonogram.  It seems that I have the native library installation via homebrew and the Java interface from Tess4J.  I believe I downloaded Tess4J from SourceForge here: [SourceForge Tess4J download](https://sourceforge.net/projects/tess4j/).  And my homebrew version is 4.1.0.  There are newer versions of Tesseract available now (v5) and I'm not sure if Tess4J will work with these newer versions.

## Tess4J and NetBeans
Once you download the Tess4J package, you'll see the following folders:

- dist (contains the .jar file you need)
- lib (contains the Windows .dlls you would need)
- nbproject
- src
- tessdata (contains the English training data you need, though I used the training data installed by homebrew)
- test

The Tess4J folder is configured as a NetBeans project.  In NetBeans, you can load the Tess4J folder as a project.  In theory, you should be able to run the JUnit tests by right-clicking on the project icon and select "Test...".  However, I could not get this to run on either my Mac or my PC, even when I added `-Dtest-sys-prop.java.library.path=lib/win32-x86-64` and `-Djava.library.path=lib/win32-x86-64` to the "Run" "VM Options:" in the Project Properties.  (Example is for the PC).  I also tried using the full path to the library folder and that also did not work.

However on my Mac, I was able to get Tess4J and Tesseract to work without having to set up the java.library.path on the JVM command line.  Somehow by using homebrew on my Mac, my system can automatically find the libtesseract.dylib without my needing to set the VM Options.  However, it cannot find the .dylib when I try to run the JUnit tests on my Mac.  I may have to do something similar on my PC to get this to work (i.e. install Tesseract separately and set the system path to find it automatically).

## Tesseract Example in Java / NetBeans
Here is a quick example of how to use Tesseract OCR in Java.  You can create this sample program within the Tess4J Netbeans project and see if it will run.

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
        final String[] formats = ShowImageIOInfo();        
	    
        // ---- Have user select a TIFF image ----
        FileDialog fd = new FileDialog((java.awt.Frame) null, "Select a TIFF image for OCR", FileDialog.LOAD);
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
        tess.setDatapath("/usr/local/Cellar/tesseract/4.1.1/share/tessdata");	
        tess.setTessVariable("tessedit_char_whitelist", "0123456789");
        try
        {
            String ocr_out = tess.doOCR (grey_img);      
            System.out.println ("OCR output: " + ocr_out);
        }
        catch (TesseractException tioe)
        { System.out.println ("TesseractException: " + tioe.getLocalizedMessage()); }
    }
    
    public String[] ShowImageIOInfo () {
        String[] formats = ImageIO.getReaderFormatNames();
        for (int i = 0; i < formats.length; ++i) {
          System.out.println("reader " + formats[i]);
        }

        String[] names = ImageIO.getWriterFormatNames();
        for (int i = 0; i < names.length; ++i) {
          System.out.println("writer " + names[i]);
        }
        
        return formats;
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
Getting OpenCV installed on my Intel MacBook Pro was a little more difficult.  You can use homebrew to install OpenCV, but it no longer supports building the Java interfaces.  So I ended up installing OpenCV from source and building it locally on my machine.  I used instructions from here: [OpenCV 4 with Java instructions](https://delabassee.com/OpenCVJava/) by David Delabassée.

I already had XCode and Java 8 installed on my Mac so I started with creating a landing space for OpenCV and downloading the source from GitHub:

```
mkdir workspace;cd workspace
git clone https://github.com/opencv/opencv.git
mkdir build
```

I think used ccmake to create the make file.

```
ccmake -S opencv/ -B build/
```

Following the directions, you hit "C" to scan the environment.  You can navigate through the various options, turning them on and off.  Then press "T" to enter Advanced Mode.  Advanced Mode was necessary to be able to configure some of the more obscure settings for Java.  Be sure to set the Java environment variables noted on the webpage.

David Delabassée has a list of suggested modules to omit for a faster build and for just getting started.  I did try building the image codecs, but was not able to get it to actually build, having run into some kind of problem with needing/building gdal.  Because I'm relying on java.awt.ImageIO for reading image files, I did not need these codecs anyway.

When you have finished setting up your options, press "G" to generate the Makefile.

Once the Makefile is generated, you can build OpenCV using:

```
cmake --build build
```

Presumably all goes well and you now have OpenCV native libraries and the Java Native Interface to work with.  The .jar file should be in the build/bin folder and the native library files should be in build/lib.

## OpenCV and NetBeans
Now that OpenCV and it's Java bindings have been built, you need to add the `opencv-453.jar` library to the list of Libraries for the project.  And you add `-Djava.library.path=/Users/Lynne/Documents/OpenCV/build/lib` to the "Run" "VM Options:" Project Preferences.

## OpenCV Example in Java / NetBeans
The following code was used to compute the FFT of a greyscale image.
