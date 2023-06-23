package preprocess;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import jhazm.Normalizer;

import opennlp.tools.tokenize.PersianHalfSpaceChecker;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class Preprocessor {
	
	public TokenizerME tokenizer;
	public PersianHalfSpaceChecker halfSpaceChecker;
	
    /**
     * Use this static reference to retrieve an instance of the
     * {@link Preprocessor}.
     */
    public static final Preprocessor INSTANCE = new Preprocessor();
	
	public Preprocessor(){
		
		try{
			
			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			System.out.println("Current relative path is: " + s);
			
		    // Loading the TokenizerME model
			InputStream inputStream = new FileInputStream("resources/models/tokenizer_model");
			TokenizerModel tokenizerModel = new TokenizerModel(inputStream);
			// Instantiating the TokenizerME class
			tokenizer = new TokenizerME(tokenizerModel);
			
			
			// Loading the PersianHalfSpaceChecker model
			inputStream = new FileInputStream("resources/models/half_space_checker_model");
			TokenizerModel halfSpaceModel = new TokenizerModel(inputStream);
			// Instantiating the PersianHalfSpaceChecker class
			halfSpaceChecker = new PersianHalfSpaceChecker(halfSpaceModel);
			
			
		} catch (IOException ioexcp){
			System.out.println("loading the text preprocessor model is failed.");
			ioexcp.printStackTrace();
		}
	}
	
	
	public String[] run(String text){
		
		text = Normalizer.i().run(text);
		
		String[] tokens = tokenizer.split(text);
		
		String nomalizedText = halfSpaceChecker.normalize(tokens);
		
		tokens = tokenizer.join(nomalizedText);
		
		return tokens;
		
	}
	

}
