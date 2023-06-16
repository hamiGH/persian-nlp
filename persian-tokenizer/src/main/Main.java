package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import preprocess.Preprocessor;

public class Main {

	public static void main(String[] args) {
		try {
			BufferedReader brInput = new BufferedReader(new FileReader("resources/data/input.txt"));
	        String text = "";
			String line1 = null;
			while ((line1 = brInput.readLine()) != null) {
	            line1 = line1.trim();
	            if (!line1.isEmpty()) {
	            	text += "\n " + line1;
	            }
	        }
			brInput.close();
			
			String[] tokens = Preprocessor.INSTANCE.run(text);
			for (String token : tokens) {
				System.out.println(token);
			}
			
			
		}catch (IOException ioe) {
			System.out.println(ioe.toString());
		}
		
	}
}
