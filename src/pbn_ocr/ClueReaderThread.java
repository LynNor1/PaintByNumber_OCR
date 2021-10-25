/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbn_ocr;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.*;
import java.util.ArrayList;
/**
 *
 * @author Lynne
 */
public class ClueReaderThread extends Thread implements LineListener {

	private URL zero, one, two, three, four, five, six, seven, eight, nine;
	private URL ten, eleven, twelve, thirteen, fourteen, fifteen;
	private URL sixteen, seventeen, eighteen, nineteen;
	private URL twenty, thirty, forty, fifty, sixty, seventy, eighty, ninety;
	private URL hundred, pause;
	
	private ArrayList<URL> audioQueue;
	private int cur_index = 0;
	public Clip clip;

	ClueReaderThread (String clues_to_read)
	{
		LoadUpURLs();
		
		SetUpCluesToRead(clues_to_read);
	}
	
	public void SetUpCluesToRead(String clues_to_read)
	{
		String[] clues = clues_to_read.split(" ");
		
		audioQueue = new ArrayList();
		for (int i=0; i<clues.length; i++)
		{
			try
			{
				Integer theNum = Integer.parseInt(clues[i]);
				int hundreds_digit = 0;
				int tens_digit = 0;
				if (theNum == 0)
					audioQueue.add(zero);
				else if (theNum > 999)
				{
					// read each digit
					for (int j=0; j<clues[i].length(); j++)
					{
						if (clues[i].charAt(j) == '0') audioQueue.add(zero);
						else if (clues[i].charAt(j) == '1') audioQueue.add(one);
						else if (clues[i].charAt(j) == '2') audioQueue.add(two);
						else if (clues[i].charAt(j) == '3') audioQueue.add(three);
						else if (clues[i].charAt(j) == '4') audioQueue.add(four);
						else if (clues[i].charAt(j) == '5') audioQueue.add(five);
						else if (clues[i].charAt(j) == '6') audioQueue.add(six);
						else if (clues[i].charAt(j) == '7') audioQueue.add(seven);
						else if (clues[i].charAt(j) == '8') audioQueue.add(eight);
						else if (clues[i].charAt(j) == '9') audioQueue.add(nine);
					}				
				} else if (theNum > 99)
				{
					// get the hundred's digit
					hundreds_digit = (int)Math.floor((float)theNum/100.0);
					audioQueue.add(getURLForDigit(hundreds_digit));
					audioQueue.add(hundred);
					theNum = theNum - hundreds_digit*100;
				}
				if (theNum < 100)
				{
					tens_digit = (int)Math.floor((float)theNum/10.0);
					if (tens_digit == 1)
					{
						if (theNum == 10) audioQueue.add(ten);
						else if (theNum == 11) audioQueue.add(eleven);
						else if (theNum == 12) audioQueue.add(twelve);
						else if (theNum == 13) audioQueue.add(thirteen);
						else if (theNum == 14) audioQueue.add(fourteen);
						else if (theNum == 15) audioQueue.add(fifteen);
						else if (theNum == 16) audioQueue.add(sixteen);
						else if (theNum == 17) audioQueue.add(seventeen);
						else if (theNum == 18) audioQueue.add(eighteen);
						else audioQueue.add(nineteen);
						theNum = 0;
					} else
					{
						if (tens_digit == 2) audioQueue.add(twenty);
						else if (tens_digit == 3) audioQueue.add(thirty);
						else if (tens_digit == 4) audioQueue.add(forty);
						else if (tens_digit == 5) audioQueue.add(fifty);
						else if (tens_digit == 6) audioQueue.add(sixty);
						else if (tens_digit == 7) audioQueue.add(seventy);
						else if (tens_digit == 8) audioQueue.add(eighty);
						else if (tens_digit == 9) audioQueue.add(ninety);
						theNum -= tens_digit*10;
					}
				}
				if (theNum > 0 && theNum < 10)
					audioQueue.add(getURLForDigit(theNum));
				audioQueue.add(pause);
			} catch (NumberFormatException nfe)
			{ }
		}
	}
	
	private URL getURLForDigit (int digit)
	{
		if (digit == 0) return zero;
		else if (digit == 1) return one;
		else if (digit == 2) return two;
		else if (digit == 3) return three;
		else if (digit == 4) return four;
		else if (digit == 5) return five;
		else if (digit == 6) return six;
		else if (digit == 7) return seven;
		else if (digit == 8) return eight;
		else return nine;
	}
	
	public void run()
	{
		try
		{
			clip = AudioSystem.getClip();
			clip.addLineListener (this);

			// Open audio clip and load samples from the audio input stream.
			clip.open(AudioSystem.getAudioInputStream(audioQueue.get(cur_index)));
			clip.start();	
		}
		catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		catch (IOException ie) {
			ie.printStackTrace();
		}
		catch (UnsupportedAudioFileException uafe) {
			uafe.printStackTrace();
		}
	}
	
	public void update (LineEvent le)
	{
		LineEvent.Type type  = le.getType();
		if (type == LineEvent.Type.STOP)
		{
//			System.out.println ("STOP");
			clip.close();
			cur_index += 1;
			if (cur_index < audioQueue.size())
			{
				try
				{
//					System.out.println ("Trying to play clip " + cur_index);					
					clip = AudioSystem.getClip();
					clip.addLineListener(this);
					clip.open(AudioSystem.getAudioInputStream(audioQueue.get(cur_index)));
					clip.start();
				}
				catch (LineUnavailableException e) {
					System.out.println (e.getLocalizedMessage());
				}		
				catch (IOException ie) {
					System.out.println (ie.getLocalizedMessage());
				}	
				catch (UnsupportedAudioFileException uafe) {
					System.out.println (uafe.getLocalizedMessage());
				}
			} else
			{
				clip.close();
//				System.out.println ("Finished audio clips");
			}
		} else if (type == LineEvent.Type.CLOSE)
			;
//			System.out.println ("CLOSE");
		else if (type == LineEvent.Type.OPEN)
			;
//			System.out.println ("OPEN");
		else if (type == LineEvent.Type.START)
			;
//			System.out.println ("START");
	}
	  
	private void LoadUpURLs ()
	{
		 zero = this.getClass().getClassLoader().getResource("audio/zero.wav");		 
         one = this.getClass().getClassLoader().getResource("audio/one.wav");		 
		 two = this.getClass().getClassLoader().getResource("audio/two.wav");
		 three = this.getClass().getClassLoader().getResource("audio/three.wav");
		 four = this.getClass().getClassLoader().getResource("audio/four.wav");
		 five = this.getClass().getClassLoader().getResource("audio/five.wav");
		 six = this.getClass().getClassLoader().getResource("audio/six.wav");
		 seven = this.getClass().getClassLoader().getResource("audio/seven.wav");
		 eight = this.getClass().getClassLoader().getResource("audio/eight.wav");
		 nine = this.getClass().getClassLoader().getResource("audio/nine.wav");
		 pause = this.getClass().getClassLoader().getResource("audio/pause.wav");
		 
		 ten = this.getClass().getClassLoader().getResource("audio/ten.wav");		 
         eleven = this.getClass().getClassLoader().getResource("audio/eleven.wav");		 
		 twelve = this.getClass().getClassLoader().getResource("audio/twelve.wav");
		 thirteen = this.getClass().getClassLoader().getResource("audio/thirteen.wav");
		 fourteen = this.getClass().getClassLoader().getResource("audio/fourteen.wav");
		 fifteen = this.getClass().getClassLoader().getResource("audio/fifteen.wav");
		 sixteen = this.getClass().getClassLoader().getResource("audio/sixteen.wav");
		 seventeen = this.getClass().getClassLoader().getResource("audio/seventeen.wav");
		 eighteen = this.getClass().getClassLoader().getResource("audio/eighteen.wav");
		 nineteen = this.getClass().getClassLoader().getResource("audio/nineteen.wav");
		 
		 twenty = this.getClass().getClassLoader().getResource("audio/twenty.wav");
		 thirty = this.getClass().getClassLoader().getResource("audio/thirty.wav");
		 forty = this.getClass().getClassLoader().getResource("audio/forty.wav");
		 fifty = this.getClass().getClassLoader().getResource("audio/fifty.wav");
		 sixty = this.getClass().getClassLoader().getResource("audio/sixty.wav");
		 seventy = this.getClass().getClassLoader().getResource("audio/seventy.wav");
		 eighty = this.getClass().getClassLoader().getResource("audio/eighty.wav");
		 ninety = this.getClass().getClassLoader().getResource("audio/ninety.wav");
		 
		 hundred = this.getClass().getClassLoader().getResource("audio/hundred.wav");
		 
	}
	
}
