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

The Tess4J folder is configured as a NetBeans project!  In NetBeans, you can load the Tess4J folder as a project.  Right-click on the project icon and select "Test..." to run the JUnit tests.  If your code is having trouble finding the native DLLs, you need to add -Dtest-sys-prop.java.library.path=lib/win32-x86-64 for the JUnit tests to run properly.  For your own code, you need to add -Djava.library.path=lib/win32-x86-64 to the "Run" "VM Options:" in the Project Properties.  (This is **NOT** working for me right now on my PC!)

Here is a quick example of how to use Tesseract OCR in Java:

`code example`

# Installing OpenCV for Mac / Java
Getting OpenCV installed on my Intel MacBook Pro was a little more difficult.  You can use homebrew to install OpenCV, but it no longer supports building the Java interfaces.  So I ended up installing OpenCV from source and building it locally on my machine.  I used instructions from here: [OpenCV 4 with Java instructions](https://delabassee.com/OpenCVJava/).
