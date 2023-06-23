package opennlp.tools.tokenize;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sun.org.apache.xpath.internal.operations.Bool;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.tokenize.lang.Factory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;


public class PersianHalfSpaceChecker extends AbstractTokenizer{

	/**
	   * Constant indicates a token split.
	   */
	  public static final String SPLIT = "T";

	  /**
	   * Constant indicates no token split.
	   */
	  public static final String NO_SPLIT = "F";

	  /**
	   * Alpha-Numeric Pattern
	   * @deprecated As of release 1.5.2, replaced by {@link Factory#getAlphanumeric(String)}
	   */
	  @Deprecated
	  public static final Pattern alphaNumeric = Pattern.compile(Factory.DEFAULT_ALPHANUMERIC);

	  private final Pattern alphanumeric;

	  /**
	   * The maximum entropy model to use to evaluate contexts.
	   */
	  private MaxentModel model;

	  /**
	   * The context generator.
	   */
	  private final TokenContextGenerator cg;

	  /**
	   * Optimization flag to skip alpha numeric tokens for further
	   * tokenization
	   */
	  private boolean useAlphaNumericOptimization;

	  /**
	   * List of probabilities for each token returned from a call to
	   * <code>tokenize</code> or <code>tokenizePos</code>.
	   */
	  private List<Double> tokProbs;

	  private List<Span> newTokens;

	  public PersianHalfSpaceChecker(TokenizerModel model) {
	    TokenizerFactory factory = model.getFactory();
	    this.alphanumeric = factory.getAlphaNumericPattern();
	    this.cg = factory.getContextGenerator();
	    this.model = model.getMaxentModel();
	    this.useAlphaNumericOptimization = factory.isUseAlphaNumericOptmization();

	    newTokens = new ArrayList<>();
	    tokProbs = new ArrayList<>(50);
	  }

	  /**
	   * @deprecated use {@link TokenizerFactory} to extend the Tokenizer
	   *             functionality
	   */
	  public PersianHalfSpaceChecker(TokenizerModel model, Factory factory) {
	    String languageCode = model.getLanguage();

	    this.alphanumeric = factory.getAlphanumeric(languageCode);
	    this.cg = factory.createTokenContextGenerator(languageCode,
	        getAbbreviations(model.getAbbreviations()));

	    this.model = model.getMaxentModel();
	    useAlphaNumericOptimization = model.useAlphaNumericOptimization();

	    newTokens = new ArrayList<>();
	    tokProbs = new ArrayList<>(50);
	  }

	  private static Set<String> getAbbreviations(Dictionary abbreviations) {
	    if (abbreviations == null) {
	      return Collections.emptySet();
	    }
	    return abbreviations.asStringSet();
	  }

	  /**
	   * Returns the probabilities associated with the most recent
	   * calls to {@link PersianHalfSpaceChecker#tokenize(String)} or {@link PersianHalfSpaceChecker#tokenizePos(String)}.
	   *
	   * @return probability for each token returned for the most recent
	   *     call to tokenize.  If not applicable an empty array is returned.
	   */
	  public double[] getTokenProbabilities() {
	    double[] tokProbArray = new double[tokProbs.size()];
	    for (int i = 0; i < tokProbArray.length; i++) {
	      tokProbArray[i] = tokProbs.get(i);
	    }
	    return tokProbArray;
	  }

	  /**
	   * Tokenizes the string.
	   *
	   * @param d  The string to be tokenized.
	   *
	   * @return   A span array containing individual tokens as elements.
	   */
	  public Span[] tokenizePos(String d) {
	    Span[] tokens = WhitespaceTokenizer.INSTANCE.tokenizePos(d);
	    newTokens.clear();
	    tokProbs.clear();
	    for (Span s : tokens) {
	      String tok = d.substring(s.getStart(), s.getEnd());
	      // Can't tokenize single characters
	      if (tok.length() < 2) {
	        newTokens.add(s);
	        tokProbs.add(1d);
	      } else if (useAlphaNumericOptimization() && alphanumeric.matcher(tok).matches()) {
	        newTokens.add(s);
	        tokProbs.add(1d);
	      } else {
	        int start = s.getStart();
	        int end = s.getEnd();
	        final int origStart = s.getStart();
	        double tokenProb = 1.0;
	        for (int j = origStart + 1; j < end; j++) {
	          double[] probs =
	              model.eval(cg.getContext(tok, j - origStart));
	          String best = model.getBestOutcome(probs);
	          tokenProb *= probs[model.getIndex(best)];
	          if (best.equals(PersianHalfSpaceChecker.SPLIT)) {
	            newTokens.add(new Span(start, j));
	            tokProbs.add(tokenProb);
	            start = j;
	            tokenProb = 1.0;
	          }
	        }
	        newTokens.add(new Span(start, end));
	        tokProbs.add(tokenProb);
	      }
	    }

	    Span[] spans = new Span[newTokens.size()];
	    newTokens.toArray(spans);
	    return spans;
	  }

	  
	  
	  /**
	   * Tokenizes the string.
	   * @param d  The string to be tokenized.
	   * @return   A string array containing individual tokens as elements.
	   */
	  
	  public String[] tokenizeNormalize(String d) {
		  	String seperatorChars = Character.toString((char)8204);
		  	List<String> tokList = new ArrayList<>();
		    String newD = d;
		    int shift = 0;
		  	
		    String[] toks = WhitespaceTokenizer.INSTANCE.tokenize(d);
		    
//		    String[] toks;
//			try {
//				InputStream inputStream = new FileInputStream("en-token.bin");
//							
//				//Instantiating the TokenizerME class
//				TokenizerModel tokenModel = new TokenizerModel(inputStream);
//				tokenModel.getFactory().createArtifactMap().put("useAlphaNumericOptimization", false);
//				
//				TokenizerME tokenizer = new TokenizerME(tokenModel);
//				toks = tokenizer.tokenize(d);
//				
//			} catch (IOException e) {
//				toks = WhitespaceTokenizer.INSTANCE.tokenize(d);
//			}

			
		    for(int idx = 0; idx < toks.length; idx++){
		    	
				// Can't tokenize single characters
				if (toks[idx].length() < 2) {  
					tokProbs.add(1d);
				    continue;
				} else if (useAlphaNumericOptimization() && alphanumeric.matcher(toks[idx]).matches()) {
					tokProbs.add(1d);
					continue;
				} else {
					double tokenProb = 1.0;
					for(int i = 1; i < toks[idx].length() ; i++){
						double[] probs = model.eval(cg.getContext(toks[idx], i));
					    String best = model.getBestOutcome(probs);
					    tokenProb *= probs[model.getIndex(best)];
					    if (best.equals(TokenizerME.SPLIT)) {
					    	toks[idx] = new StringBuilder(toks[idx]).insert(i, seperatorChars).toString();
					    	i++;
					    }
					}
					tokProbs.add(tokenProb);
				}
		    }
		    
		    if(toks.length < 2){
		    	if(toks.length > 0){
		    		tokList.add(toks[0]);
		    	}
		    }else{   	
		    	
		        double[] probs = model.eval(cg.getContext(toks[0]+toks[1], toks[0].length()));
		        String prevBest = model.getBestOutcome(probs);
		        double prevTokenProb = probs[model.getIndex(prevBest)];
		        
		        for(int idx = 1; idx < toks.length - 1; idx++){
		        	
		        	probs = model.eval(cg.getContext(toks[idx] + toks[idx + 1], toks[idx].length()));
			        String best = model.getBestOutcome(probs);
			        double tokenProb = probs[model.getIndex(best)];
			        
			        if (prevBest.equals(PersianHalfSpaceChecker.SPLIT) 
			        		&& (!best.equals(PersianHalfSpaceChecker.SPLIT) || prevTokenProb > tokenProb)) {
			        	toks[idx] = toks[idx-1] + seperatorChars + toks[idx];
			        	
			        	probs = model.eval(cg.getContext(toks[idx]+toks[idx+1], toks[idx].length()));
				        prevBest = model.getBestOutcome(probs);
				        prevTokenProb = probs[model.getIndex(prevBest)];
				        
			        }else{
			        	tokList.add(toks[idx-1]);
			        	prevBest = best;
			        	prevTokenProb = tokenProb;
			        }
			        
		        }
		        
		        if(prevBest.equals(PersianHalfSpaceChecker.SPLIT)){
		        	tokList.add(toks[toks.length-2] + seperatorChars+ toks[toks.length-1]);
		        }else{
		        	tokList.add(toks[toks.length-2]);
		        	tokList.add(toks[toks.length-1]);
		        }
		    }
		    String[] toks1 = new String[tokList.size()];
		    tokList.toArray(toks1);
		    return toks1;
		    
		    
		    
//		    
//		    Span[] tokens = WhitespaceTokenizer.INSTANCE.tokenizePos(d);
//		    newTokens.clear();
//		    tokProbs.clear();
//		    for (Span s : tokens) {
//		      String tok = d.substring(s.getStart(), s.getEnd());
//		      // Can't tokenize single characters
//		      if (tok.length() < 2) {
//		        newTokens.add(s);
//		        tokProbs.add(1d);
//		      } else if (useAlphaNumericOptimization() && alphanumeric.matcher(tok).matches()) {
//		        newTokens.add(s);
//		        tokProbs.add(1d);
//		      } else {
//		        int start = s.getStart();
//		        int end = s.getEnd();
//		        final int origStart = s.getStart();
//		        double tokenProb = 1.0;
//		        for (int j = origStart + 1; j < end; j++) {
//		          double[] probs =
//		              model.eval(cg.getContext(tok, j - origStart));
//		          String best = model.getBestOutcome(probs);
//		          tokenProb *= probs[model.getIndex(best)];
//		          if (best.equals(TokenizerME.SPLIT)) {
//		            newTokens.add(new Span(start, j));
//		            tokProbs.add(tokenProb);
//		            start = j;
//		            tokenProb = 1.0;
//		          }
//		        }
//		        newTokens.add(new Span(start, end));
//		        tokProbs.add(tokenProb);
//		      }
//		    }
//
//		    
//		    if(newTokens.size() < 2){
//		    	if(newTokens.size() > 0){
//		    		tokList.add(d.substring(newTokens.get(0).getStart(), newTokens.get(0).getEnd()));
//		    	}
//		    }
//		    else{
//		    	int prevStartJoin = newTokens.get(0).getStart();
//		    	int prevEndJoin = newTokens.get(0+1).getEnd();
//		    	int midJoin = newTokens.get(0).getEnd() - newTokens.get(0).getStart();
//		    	
//		    	String prevJoinedTok = d.substring(prevStartJoin, prevEndJoin).replaceAll(" ", "");
//		        double[] probs = model.eval(cg.getContext(prevJoinedTok, midJoin));
//		        String prevBest = model.getBestOutcome(probs);
//		        double prevTokenProb = probs[model.getIndex(prevBest)];
//		        
//			    for(int idx = 1 ; idx < newTokens.size()-1 ; idx++){
//			    	int startJoin = newTokens.get(idx).getStart();
//			    	int endJoin = newTokens.get(idx+1).getEnd();
//			    	midJoin = d.substring(startJoin, newTokens.get(idx).getEnd()).replaceAll(" ", "").length();//newTokens.get(idx).getEnd() - newTokens.get(idx).getStart(); 
//			    	
//			    	String joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
//			        probs = model.eval(cg.getContext(joinedTok, midJoin));
//			        String best = model.getBestOutcome(probs);
//			        double tokenProb = probs[model.getIndex(best)];
//			        
//			        int prevIdx = idx-1; 
//			        if (prevBest.equals(TokenizerNormalizer.NO_SPLIT) 
//		        		&& (!best.equals(TokenizerNormalizer.NO_SPLIT) || prevTokenProb > tokenProb)) {
//			        	
//			        	newTokens.set(prevIdx, new Span(prevStartJoin, prevEndJoin));
//			        	tokProbs.set(prevIdx, prevTokenProb);
//			            newTokens.remove(prevIdx+1);
//			            tokProbs.remove(prevIdx+1);
//			            //tokList.add(prevJoinedTok);
//			            
//			            //recalculate for new token
//			            startJoin = newTokens.get(prevIdx).getStart();
//				    	endJoin = newTokens.get(prevIdx+1).getEnd();
//				    	midJoin =  d.substring(startJoin, newTokens.get(prevIdx).getEnd()).replaceAll(" ", "").length(); //newTokens.get(prevIdx).getEnd() - newTokens.get(prevIdx).getStart();
//				    	
//				    	joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
//				        probs = model.eval(cg.getContext(joinedTok, midJoin));
//				        best = model.getBestOutcome(probs);
//				        tokenProb = probs[model.getIndex(best)];
//				        
//			            idx--;
//			        }
//			        else{
//			        	tokList.add(d.substring(newTokens.get(prevIdx).getStart(), newTokens.get(prevIdx).getEnd()).replaceAll(" ", ""));
//	//		        	if(idx == newTokens.size()-2){
//	//		        		tokList.add(d.substring(newTokens.get(idx+1).getStart(), newTokens.get(idx+1).getEnd()));
//	//		        	}
//			        }
//			        prevStartJoin = startJoin;
//			        prevEndJoin = endJoin;
//			        prevJoinedTok = joinedTok;
//			        prevTokenProb = tokenProb;
//			        prevBest = best;
//			    }
//			    
//			    int prevIdx = newTokens.size()-2; 
//			    if (prevBest.equals(TokenizerNormalizer.NO_SPLIT)) {
//			        	newTokens.set(prevIdx, new Span(prevStartJoin, prevEndJoin));
//			        	tokProbs.set(prevIdx, prevTokenProb);
//			            newTokens.remove(prevIdx+1);
//			            tokProbs.remove(prevIdx+1);
//			            tokList.add(prevJoinedTok);
//			    }
//			    else{
//		        	tokList.add(d.substring(newTokens.get(prevIdx).getStart(), newTokens.get(prevIdx).getEnd()).replaceAll(" ", ""));
//		        	tokList.add(d.substring(newTokens.get(prevIdx+1).getStart(), newTokens.get(prevIdx+1).getEnd()).replaceAll(" ", ""));
//			    }
//		    }
//		    String[] toks = new String[tokList.size()];
//		    tokList.toArray(toks);
//		    return toks;
			    
		  }

		  
	  
	  
//	  public String[] tokenizeNormalize(String d) {
//	  	List<String> tokList = new ArrayList<>();
//	  
//	    Span[] tokens = WhitespaceTokenizer.INSTANCE.tokenizePos(d);
//	    newTokens.clear();
//	    tokProbs.clear();
//	    for (Span s : tokens) {
//	      String tok = d.substring(s.getStart(), s.getEnd());
//	      // Can't tokenize single characters
//	      if (tok.length() < 2) {
//	        newTokens.add(s);
//	        tokProbs.add(1d);
//	      } else if (useAlphaNumericOptimization() && alphanumeric.matcher(tok).matches()) {
//	        newTokens.add(s);
//	        tokProbs.add(1d);
//	      } else {
//	        int start = s.getStart();
//	        int end = s.getEnd();
//	        final int origStart = s.getStart();
//	        double tokenProb = 1.0;
//	        for (int j = origStart + 1; j < end; j++) {
//	          double[] probs =
//	              model.eval(cg.getContext(tok, j - origStart));
//	          String best = model.getBestOutcome(probs);
//	          tokenProb *= probs[model.getIndex(best)];
//	          if (best.equals(TokenizerME.SPLIT)) {
//	            newTokens.add(new Span(start, j));
//	            tokProbs.add(tokenProb);
//	            start = j;
//	            tokenProb = 1.0;
//	          }
//	        }
//	        newTokens.add(new Span(start, end));
//	        tokProbs.add(tokenProb);
//	      }
//	    }
//
////	    Span[] spans = new Span[newTokens.size()];
////	    newTokens.toArray(spans);
////	    return spans;
//	    for(int idx = 0 ; idx < newTokens.size()-1 ; idx++){
//	    	int startJoin = newTokens.get(idx).getStart();
//	    	int endJoin = newTokens.get(idx+1).getEnd();
//	    	int midJoin = newTokens.get(idx).getEnd() - newTokens.get(idx).getStart();
//	    	
//	    	String joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
//	        double[] probs =
//		              model.eval(cg.getContext(joinedTok, midJoin));
//	        String best = model.getBestOutcome(probs);
//	        double tokenProb = probs[model.getIndex(best)];
//	        if (best.equals(TokenizerNormalizer.NO_SPLIT)) {
//	        	newTokens.set(idx, new Span(startJoin, endJoin));
//	        	tokProbs.set(idx, tokenProb);
//	            newTokens.remove(idx+1);
//	            tokProbs.remove(idx+1);
//	            tokList.add(joinedTok);
//	        }
//	        else{
//	        	tokList.add(d.substring(newTokens.get(idx).getStart(), newTokens.get(idx).getEnd()));
//	        	if(idx == newTokens.size()-2){
//	        		tokList.add(d.substring(newTokens.get(idx+1).getStart(), newTokens.get(idx+1).getEnd()));
//	        	}
//	        }
//	    }
//	    
//	    String[] toks = new String[tokList.size()];
//	    tokList.toArray(toks);
//	    return toks;
//		    
//	  }

	  
//	  public String[] tokenizeNormalize(String d) {
//		  	List<String> tokList = new ArrayList<>();
//		  
//		    Span[] tokens = WhitespaceTokenizer.INSTANCE.tokenizePos(d);
//		    newTokens.clear();
//		    tokProbs.clear();
//		    for(int sidx = 0 ; sidx < tokens.length ; sidx++){
//		      Span s = tokens[sidx];
//		    //for (Span s : tokens) {
//		      
//		      String tok = d.substring(s.getStart(), s.getEnd());
//		      // Can't tokenize single characters
//		      /*if (tok.length() < 2) {
//		        newTokens.add(s);
//		        tokProbs.add(1d);
//		      } else*/ 
//		      if (useAlphaNumericOptimization() && alphanumeric.matcher(tok).matches()) {
//		        newTokens.add(s);
//		        tokProbs.add(1d);
//		        tokList.add(d.substring(s.getStart(), s.getEnd()));
//		      } else {
//		        int start = s.getStart();
//		        int end = s.getEnd();
//		        double tokenProb = 1.0;
//		        
//		        if (tok.length() > 1) {
//			        final int origStart = s.getStart();
//			        for (int j = origStart + 1; j < end; j++) {
//			          double[] probs =
//			              model.eval(cg.getContext(tok, j - origStart));
//			          String best = model.getBestOutcome(probs);
//			          tokenProb *= probs[model.getIndex(best)];
//			          if (best.equals(TokenizerNormalizer.SPLIT)) {
//			            newTokens.add(new Span(start, j));
//			            tokProbs.add(tokenProb);
//			            tokList.add(d.substring(start, j));
//			            start = j;
//			            tokenProb = 1.0;
//			          }
//			        }
//		        }
//		        
//		        boolean joined = false;
//		        if(sidx+1 < tokens.length){
//		        	String joinedTok = d.substring(start, tokens[sidx+1].getEnd()).replaceAll(" ", "");
//			        double[] probs =
//				              model.eval(cg.getContext(joinedTok, s.getEnd()));
//			        String best = model.getBestOutcome(probs);
//			        double tokenProbJ = probs[model.getIndex(best)];
//			        if (best.equals(TokenizerNormalizer.NO_SPLIT)) {
//			            newTokens.add(new Span(start, tokens[sidx+1].getEnd()));
//			            tokProbs.add(tokenProbJ);
//			            tokList.add(joinedTok);
//			            sidx++;
//			            joined = true;
//			        }
//		        }
//		        
//		        if(!joined){
//			        newTokens.add(new Span(start, end));
//			        tokProbs.add(tokenProb);
//			        tokList.add(d.substring(start, end));
//		        }
//		      }
//		    }
//
//		    Span[] spans = new Span[newTokens.size()];
//		    newTokens.toArray(spans);
//		    //return spans;
//		    String[] toks = new String[tokList.size()];
//		    tokList.toArray(toks);
//		    return toks;
//		    
//		  }

	  
	  
	  
	  /**
	   * Trains a model for the {@link PersianHalfSpaceChecker}.
	   *
	   * @param samples
	   *          the samples used for the training.
	   * @param factory
	   *          a {@link TokenizerFactory} to get resources from
	   * @param mlParams
	   *          the machine learning train parameters
	   * @return the trained {@link TokenizerModel}
	   * @throws IOException
	   *           it throws an {@link IOException} if an {@link IOException} is
	   *           thrown during IO operations on a temp file which is created
	   *           during training. Or if reading from the {@link ObjectStream}
	   *           fails.
	   */
	  public static TokenizerModel train(ObjectStream<TokenSample> samples, TokenizerFactory factory,
	      TrainingParameters mlParams) throws IOException {

	    Map<String, String> manifestInfoEntries = new HashMap<>();

	    ObjectStream<Event> eventStream = new TokSpanEventStream(samples,
	        factory.isUseAlphaNumericOptmization(),
	        factory.getAlphaNumericPattern(), factory.getContextGenerator());

	    EventTrainer trainer = TrainerFactory.getEventTrainer(
	        mlParams, manifestInfoEntries);

	    MaxentModel maxentModel = trainer.train(eventStream);

	    return new TokenizerModel(maxentModel, manifestInfoEntries, factory);
	  }

	  /**
	   * Returns the value of the alpha-numeric optimization flag.
	   *
	   * @return true if the tokenizer should use alpha-numeric optimization, false otherwise.
	   */
	  public boolean useAlphaNumericOptimization() {
	    return useAlphaNumericOptimization;
	  }

}
