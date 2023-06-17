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

import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenSampleStream;
import opennlp.tools.tokenize.TokenizerFactory;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.tokenize.TokenizerME;

import opennlp.model.TrainUtil; 

public class Main {

	
	/**
	 * 
	 */
	
	public static void trainModel(){
		
		try{
		
		Charset charset = Charset.forName("UTF-8");
		
		
		final String fileName = "./data/sample_data.train";
		InputStreamFactory isf = new InputStreamFactory() {
			
			public InputStream createInputStream() throws IOException {
				// TODO Auto-generated method stub
				return new FileInputStream(fileName);
			}
		};
		
		
		ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
		
		
		ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

		TokenizerModel model;

		try {
			String language = "fa"; 
			TokenizerFactory tokenFactory = TokenizerFactory.create(null, language, null, true, null);

			TrainingParameters params = new TrainingParameters().defaultParams();
			
			params.put(TrainUtil.ITERATIONS_PARAM, "500");
			//params.put(TrainUtil.CUTOFF_PARAM, "10");
			
			model = TokenizerME.train(sampleStream, tokenFactory, params);
//		  	model = TokenizerME.train("en", sampleStream, true, TrainingParameters.defaultParams());
		}
		finally {
		  sampleStream.close();
		}

		String sentence = "یکروز گوشتمرغ، روزدیگر گوشتقرمز، یکروز تخم مرغ وروز دیگربرنج و این قصهپر غصه همواره در طو ل سال به فراخور شرایط به انحایمختلف آرامشقیمتی را  از بازار سلب می‌کند.";
		//String sentence = "طبیعتاً سیاستگذاری رشد ۱۱.۶ با ۷.۲ متفاوت است";
		
		//Instantiating the TokenizerME class
//		TokenizerME tokenizer = new TokenizerME(model);
//		String[] tokens = tokenizer.tokenize(sentence);
		
		TokenizerME tokenizer = new TokenizerME(model);
		String tokens[] = tokenizer.tokenizeNormalize(sentence);
		
		//Printing the tokens  
		for (String token : tokens) {
			System.out.println(token);
		}
		
		

		OutputStream modelOut = null;
		try {
		  modelOut = new BufferedOutputStream(new FileOutputStream("model/tokenizer_model"));
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
			inputStream = new FileInputStream("model/tokenizer_model");
			TokenizerModel tokenModel = new TokenizerModel(inputStream);
//			tokenModel.getFactory().createArtifactMap().put("useAlphaNumericOptimization", false);
			
			// Instantiating the TokenizerME class
			TokenizerME tokenizer = new TokenizerME(tokenModel);
					
			// Loading input test file
			FileReader fileReader = new FileReader("./data/corruptedData.test");
			LineNumberReader  lnr = new LineNumberReader(fileReader);
			lnr.skip(Long.MAX_VALUE);
			long numOfLines = lnr.getLineNumber()+1;
			lnr.close();
			BufferedReader brIn = new BufferedReader(new FileReader("./data/corruptedData.test"));
			
			// Opening output file
			BufferedWriter brOut = new BufferedWriter(new FileWriter("./data/outputData.test"));
			
		    
			double lineCounter = 0;
		    String line = brIn.readLine();
		    while (line != null) {
		    	String tokens[] = tokenizer.tokenizeNormalize(line); 
				for (String token : tokens) {
					brOut.write(token);
					brOut.write(' ');
			    }
				brOut.write(System.lineSeparator());
		        line = brIn.readLine();
		        
		        lineCounter++;
		        if(lineCounter % 1000 == 0){
		        	double progress = lineCounter / numOfLines * 100;
		        	System.out.println("progress: " + String.format("%5.3f" , progress) + " %");
		        }
		    }
		    
		    brIn.close();
		    brOut.close();
		    
		}catch (FileNotFoundException excp1) {
			// TODO: handle exception
			System.out.println(excp1.toString());
			System.out.println(excp1.getStackTrace());
		}catch (IOException excp2) {
			// TODO: handle exception
			System.out.println(excp2.toString());
			System.out.println(excp2.getStackTrace());
		}

	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		trainModel();
		
		testModel();


	}

}
