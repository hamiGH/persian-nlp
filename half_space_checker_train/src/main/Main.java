package main;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenSampleStream;
import opennlp.tools.tokenize.TokenizerFactory;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.tokenize.PersianHalfSpaceChecker;

import opennlp.model.TrainUtil; 

public class Main {

	
	/**
	 * 
	 */
	public static void trainModel(){
		
		try{
		
		Charset charset = Charset.forName("UTF-8");
		String seperatorChars = Character.toString((char)8204); //Half space
				
		final String fileName = "./data/normalize_data.train";
		InputStreamFactory isf = new InputStreamFactory() {
			
			public InputStream createInputStream() throws IOException {
				// TODO Auto-generated method stub
				return new FileInputStream(fileName);
			}
		};
		
		
		ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
		
		
		ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream, seperatorChars);

		TokenizerModel model;

		try {
			String language = "fa"; 
			TokenizerFactory tokenFactory = TokenizerFactory.create(null, language, null, true, null);

			TrainingParameters params = new TrainingParameters().defaultParams();
			
			params.put(TrainUtil.ITERATIONS_PARAM, "500");
			//params.put(TrainUtil.CUTOFF_PARAM, "10");
			
			model = TokenizerME.train(sampleStream, tokenFactory, params);
		}
		finally {
		  sampleStream.close();
		}


		String sentence = "یک روز گوشت مرغ، روزدیگر گوشت قرمز، یک روز تخم مرغ و روز دیگر برنج و این قصه پر غصه همواره در طول سال به فراخور شرایط به انحای مختلف آرامش قیمتی را  از بازار سلب میکند.";
		
		
		//Instantiating the TokenizerME class
//		TokenizerME tokenizer = new TokenizerME(model);
//		String[] tokens = tokenizer.tokenize(sentence);
		
		PersianHalfSpaceChecker tokenizer = new PersianHalfSpaceChecker(model);
		String tokens[] = tokenizer.tokenizeNormalize(sentence);
		
		//Printing the tokens  
		for (String token : tokens) {
			System.out.println(token);
		}
		
		

		OutputStream modelOut = null;
		try {
		  modelOut = new BufferedOutputStream(new FileOutputStream("half_space_checker_model"));
		  model.serialize(modelOut);
		} finally {
		  if (modelOut != null)
		     modelOut.close();
		}
		
		System.out.println("OK");
	}catch(Exception ex){
		System.out.println(ex.toString());
	}
	
	}
	
	
	public static void testModel(){
		
		try {
			
		    // Loading the Tokenizer model
			InputStream inputStream = null;
			inputStream = new FileInputStream("./models/half_space_checker_model");
			TokenizerModel tokenModel = new TokenizerModel(inputStream);
			tokenModel.getFactory().createArtifactMap().put("useAlphaNumericOptimization", false);
			
			// Instantiating the TokenizerME class
			PersianHalfSpaceChecker tokenizer = new PersianHalfSpaceChecker(tokenModel);
					
			// Loading input test file
			FileReader fileReader = new FileReader("./data/corrupted_data.test");
			LineNumberReader  lnr = new LineNumberReader(fileReader);
			lnr.skip(Long.MAX_VALUE);
			long numOfLines = lnr.getLineNumber()+1;
			lnr.close();
			BufferedReader brIn = new BufferedReader(new FileReader("./data/corrupted_data.test"));
			
			// Opening output file
			BufferedWriter brOut = new BufferedWriter(new FileWriter("./data/output_data.test"));
			
		    long totalTime = 0;
			double lineCounter = 0;
		    String line = brIn.readLine();
		    while (line != null) {
		    	long startTime = System.currentTimeMillis();
		    	
		    	String tokens[] = tokenizer.tokenizeNormalize(line); 
		    	String result = String.join(" ", tokens);
		    	
		    	totalTime += System.currentTimeMillis() - startTime;
//				for (String token : tokens) {
//					brOut.write(token);
//					brOut.write(' ');
//			    }
				brOut.write(result + System.lineSeparator());
		        line = brIn.readLine();
		        
		        lineCounter++;
		        if(lineCounter % 1000 == 0){
		        	double progress = lineCounter / numOfLines * 100;
		        	System.out.println("time: " + String.format("%8.3f" , totalTime / 1000.0) + "s    |\\/|    " +
		        	"progress: " + String.format("%5.3f" , progress) + " %");
		        }
		    }
		    
		    brIn.close();
		    brOut.close();
		    
		}catch (FileNotFoundException excp1) {
			// TODO: handle exception
		}catch (IOException excp2) {
			// TODO: handle exception
		}

	}
	
	
	public static void computeModelAccuracy1(String targetFile, String outFile){
		
		int k = 10;
		String sentence = "";
		try {
			
			BufferedReader brT = new BufferedReader(new FileReader(targetFile));
		    String lineT = brT.readLine();
		    
		    BufferedReader brO = new BufferedReader(new FileReader(outFile));
		    String lineO = brO.readLine();

		    int NumOfWrongWords = 0;
		    while (lineT != null && lineO != null) {
		    	
		    	String targetTokens[] = lineT.split(" ");
		    	String outTokens[] = lineO.split(" ");
		    	
		    	int idxT = 0;
		    	int lastTrueIdx = 0;
		    	for(int idxO = 0 ; idxO < targetTokens.length ; idxO++){
		    		String outToken = outTokens[idxO];
		    		if(outToken.compareTo(targetTokens[idxT])==0){
		    			idxT++;
		    			lastTrueIdx = idxO;
		    		}
//		    		else if(lastTrueIdx == idxO+1){
//		    			idxT++;
//		    			idxO++;
//		    		}
		    		else{
		    			if(idxO == lastTrueIdx+1){
			    			idxT++;
			    			idxO++;
			    		}
		    			
		    			boolean finded = false;
		    			int idx;
			    		for(idx = idxT ; idx < idxT+k ; idx++){
			    			if(outTokens[idxO].compareTo(targetTokens[idx])==0  
			    				&&( outTokens[idxO+1].indexOf(targetTokens[idx+1])==0 
			    				||  targetTokens[idx+1].indexOf(outTokens[idxO+1])==0 )){
			    				finded = true;
			    				break;
				    		}
			    		}
			    		
			    		if(finded){
			    			idxT = idx;
			    			lastTrueIdx = idxO;
			    		}
		    		}
		    	}
		    	
		    	lineT = brT.readLine();
		    	lineO = brO.readLine();
		    }
		    brT.close();
		    brO.close();
		    
		}catch (FileNotFoundException excp1) {
			// TODO: handle exception
		}catch (IOException excp2) {
			// TODO: handle exception
		}
		
	}
	
	
	public static void computeModelAccuracy(String targetFile, String outFile){
		
		try {
			char halfSpace = (char)0x200c;
			
			BufferedReader brT = new BufferedReader(new FileReader(targetFile));
		    String lineT = brT.readLine();
		    
		    BufferedReader brO = new BufferedReader(new FileReader(outFile));
		    String lineOut = brO.readLine();

		    int otherWrongs = 0;
		    int insertWrongs = 0;
		    int transWrongs = 0;
		    int transMissings = 0;
		    int insertMissings = 0;
		    int numOfHalfSpaces = 0;
		    while (lineT != null && lineOut != null) {
		    	
		    	String words[] = lineT.split(" ");
//		    	if(words.length > 1 && words[1].equals("اساس")){
//		    		int hami = 1;
//		    	}
		    	StringBuilder builder = new StringBuilder();
		    	for(String word : words) {
		    		if(!word.trim().isEmpty()){
		    			builder.append(word + " ");
		    		}
		    	}
		    	lineT = builder.toString().trim();
//		    	lineT = String.join(" ", tokens);
//		    	numOfWords += words.length;
		    	
		    	
		    	//just virastyar
		    	words = lineOut.split(" ");
		    	builder = new StringBuilder();
		    	for(String word : words) {
		    		if(!word.trim().isEmpty()){
		    			builder.append(word + " ");
		    		}
		    	}
		    	lineOut = builder.toString().trim();
		    	
		    	
		    	
		    	int idxOut = 0;
		    	int idxTarget = 0;
		    	while(idxOut < lineOut.length() && idxTarget < lineT.length()){
		    		char ch = lineOut.charAt(idxOut);
		    		if(lineOut.charAt(idxOut) == lineT.charAt(idxTarget)){
		    			
		    			if(lineT.charAt(idxTarget) == halfSpace){
		    				numOfHalfSpaces++;
		    			}
		    			idxOut++;
		    			idxTarget++;
		    			
		    		}else if(lineOut.charAt(idxOut) == halfSpace){
		    			
		    			if(Character.isWhitespace(lineT.charAt(idxTarget))){
		    				transWrongs++;
			    			idxTarget++;
		    			}else{
		    				insertWrongs++;
//		    				if(!lineT.substring(idxTarget, idxTarget+2).equals("ها")){
//		    					int hami = 1;
//		    				}
		    			}
		    			idxOut++;
		    			
	    			}else if(lineT.charAt(idxTarget) == halfSpace){
		    			
	    				numOfHalfSpaces++;
		    			if(Character.isWhitespace(lineOut.charAt(idxOut))){
		    				transMissings++;
		    				idxOut++;
		    			}else{
		    				insertMissings++;
		    			}
		    			idxTarget++;
		    			
		    		}else{
		    			if(Character.isWhitespace(lineOut.charAt(idxOut))){
		    				idxOut++;
		    			}else if(Character.isWhitespace(lineT.charAt(idxTarget))){ 
		    				idxTarget++;
		    			}else{
		    				idxOut++;
		    				idxTarget++;
		    			}
		    			otherWrongs++;
		    		}
		    	}
		    	
		    	lineT = brT.readLine();
		    	lineOut = brO.readLine();
		    }
		    brT.close();
		    brO.close();
		    
//		    double errorPercent = numOfWrongs * 100.0 / numOfWords;
//		    System.out.println("Number of words: " + numOfWords);
		    
		    System.out.println("number of half spaces: " + numOfHalfSpaces);
		    System.out.println("insert wrongs: " + insertWrongs);
		    System.out.println("transformation wrongs: " + transWrongs);
		    System.out.println("insert missings: " + insertMissings);
		    System.out.println("transformation missings: " + transMissings);
		    System.out.println("other wrongs: " + otherWrongs);
//		    System.out.println("Percent of error: " + String.format("%4.2f" , errorPercent) + " %");
		    
		}catch (FileNotFoundException excp1) {
			// TODO: handle exception
			System.out.println(excp1.toString());
		}catch (IOException excp2) {
			// TODO: handle exception
			System.out.println(excp2.toString());
		}
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
//		Pattern ptrn = Pattern.compile("^[0-9۰-۹]+|[0-9۰-۹]+[./][0-9۰-۹]+$");
//		Boolean bl = ptrn.matcher("۷.۲").matches();
		
//		trainModel();
		
//		testModel();
		
		System.out.println("=======<My>=============");
		computeModelAccuracy("./data/target_data.test", "./data/output_data.test");
		System.out.println("\n\n=======<Virastyar>=============");
		computeModelAccuracy("./data/target_data.test", "./data/output_data_virastyar.test");
		

	}

}
