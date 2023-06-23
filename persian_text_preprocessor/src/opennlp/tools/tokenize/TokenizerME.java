package opennlp.tools.tokenize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.w3c.dom.views.AbstractView;

import com.sun.org.apache.xpath.internal.operations.Bool;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.model.Event;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.tokenize.lang.Factory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;


public class TokenizerME extends AbstractTokenizer{

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

	  public TokenizerME(TokenizerModel model) {
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
	  public TokenizerME(TokenizerModel model, Factory factory) {
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
	   * calls to {@link TokenizerME#tokenize(String)} or {@link TokenizerME#tokenizePos(String)}.
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
	          if (best.equals(TokenizerME.SPLIT)) {
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
		  	List<String> tokList = new ArrayList<>();
		  
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
		          if (best.equals(TokenizerME.SPLIT)) {
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

		    
		    if(newTokens.size() < 2){
		    	if(newTokens.size() > 0){
		    		tokList.add(d.substring(newTokens.get(0).getStart(), newTokens.get(0).getEnd()));
		    	}
		    }
		    else{
		    	int prevStartJoin = newTokens.get(0).getStart();
		    	int prevEndJoin = newTokens.get(0+1).getEnd();
		    	int midJoin = newTokens.get(0).getEnd() - newTokens.get(0).getStart();
		    	
		    	String prevJoinedTok = d.substring(prevStartJoin, prevEndJoin).replaceAll(" ", "");
		        double[] probs = model.eval(cg.getContext(prevJoinedTok, midJoin));
		        String prevBest = model.getBestOutcome(probs);
		        double prevTokenProb = probs[model.getIndex(prevBest)];
		        
			    for(int idx = 1 ; idx < newTokens.size()-1 ; idx++){
			    	int startJoin = newTokens.get(idx).getStart();
			    	int endJoin = newTokens.get(idx+1).getEnd();
			    	midJoin = d.substring(startJoin, newTokens.get(idx).getEnd()).replaceAll(" ", "").length();//newTokens.get(idx).getEnd() - newTokens.get(idx).getStart(); 
			    	
			    	String joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
			        probs = model.eval(cg.getContext(joinedTok, midJoin));
			        String best = model.getBestOutcome(probs);
			        double tokenProb = probs[model.getIndex(best)];
			        
			        int prevIdx = idx-1; 
			        if (prevBest.equals(TokenizerME.NO_SPLIT) 
		        		&& (!best.equals(TokenizerME.NO_SPLIT) || prevTokenProb > tokenProb)) {
			        	
			        	newTokens.set(prevIdx, new Span(prevStartJoin, prevEndJoin));
			        	tokProbs.set(prevIdx, prevTokenProb);
			            newTokens.remove(prevIdx+1);
			            tokProbs.remove(prevIdx+1);
			            //tokList.add(prevJoinedTok);
			            
			            //recalculate for new token
			            startJoin = newTokens.get(prevIdx).getStart();
				    	endJoin = newTokens.get(prevIdx+1).getEnd();
				    	midJoin =  d.substring(startJoin, newTokens.get(prevIdx).getEnd()).replaceAll(" ", "").length(); //newTokens.get(prevIdx).getEnd() - newTokens.get(prevIdx).getStart();
				    	
				    	joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
				        probs = model.eval(cg.getContext(joinedTok, midJoin));
				        best = model.getBestOutcome(probs);
				        tokenProb = probs[model.getIndex(best)];
				        
			            idx--;
			        }
			        else{
			        	tokList.add(d.substring(newTokens.get(prevIdx).getStart(), newTokens.get(prevIdx).getEnd()).replaceAll(" ", ""));
	//		        	if(idx == newTokens.size()-2){
	//		        		tokList.add(d.substring(newTokens.get(idx+1).getStart(), newTokens.get(idx+1).getEnd()));
	//		        	}
			        }
			        prevStartJoin = startJoin;
			        prevEndJoin = endJoin;
			        prevJoinedTok = joinedTok;
			        prevTokenProb = tokenProb;
			        prevBest = best;
			    }
			    
			    int prevIdx = newTokens.size()-2; 
			    if (prevBest.equals(TokenizerME.NO_SPLIT)) {
			        	newTokens.set(prevIdx, new Span(prevStartJoin, prevEndJoin));
			        	tokProbs.set(prevIdx, prevTokenProb);
			            newTokens.remove(prevIdx+1);
			            tokProbs.remove(prevIdx+1);
			            tokList.add(prevJoinedTok);
			    }
			    else{
		        	tokList.add(d.substring(newTokens.get(prevIdx).getStart(), newTokens.get(prevIdx).getEnd()).replaceAll(" ", ""));
		        	tokList.add(d.substring(newTokens.get(prevIdx+1).getStart(), newTokens.get(prevIdx+1).getEnd()).replaceAll(" ", ""));
			    }
		    }
		    String[] toks = new String[tokList.size()];
		    tokList.toArray(toks);
		    return toks;
			    
		  }

		  
	  
	  public String[] split(String d){
		  	List<String> tokList = new ArrayList<>();
			  
		    Span[] tokens = WhitespaceTokenizer.INSTANCE.tokenizePos(d);
		    newTokens.clear();
		    tokProbs.clear();
		    for (Span s : tokens) {
		      String tok = d.substring(s.getStart(), s.getEnd());
		      // Can't tokenize single characters
		      if (tok.length() < 2) {
		        newTokens.add(s);
		        tokProbs.add(1d);
		        tokList.add(tok);
		      } else if (useAlphaNumericOptimization() && alphanumeric.matcher(tok).matches()) {
		        newTokens.add(s);
		        tokProbs.add(1d);
		        tokList.add(tok);
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
		          if (best.equals(TokenizerME.SPLIT)) {
		            newTokens.add(new Span(start, j));
		            tokProbs.add(tokenProb);
		            tokList.add(d.substring(start, j));
		            start = j;
		            tokenProb = 1.0;
		          }
		        }
		        newTokens.add(new Span(start, end));
		        tokProbs.add(tokenProb);
		        tokList.add(d.substring(start, end));
		      }
		    }
		    
		    String[] toks = new String[tokList.size()];
		    tokList.toArray(toks);
		    return toks;
	  }

	  
	  public String[] join(String d){
	      newTokens.clear();
	      tokProbs.clear();
		  List<String> tokList = new ArrayList<>();
		  Span[] tokens = WhitespaceTokenizer.INSTANCE.tokenizePos(d);
//		  newTokens = Arrays.asList(tokens);
		  for(Span token : tokens){
			  newTokens.add(token);
		  }
		  
		  if(newTokens.size() < 2){
	    	if(newTokens.size() > 0){
	    		tokList.add(d.substring(newTokens.get(0).getStart(), newTokens.get(0).getEnd()));
	    	}
	      }
	      else{
	    	int prevStartJoin = newTokens.get(0).getStart();
	    	int prevEndJoin = newTokens.get(0+1).getEnd();
	    	int midJoin = newTokens.get(0).getEnd() - newTokens.get(0).getStart();
	    	
	    	String prevJoinedTok = d.substring(prevStartJoin, prevEndJoin).replaceAll(" ", "");
	        double[] probs = model.eval(cg.getContext(prevJoinedTok, midJoin));
	        String prevBest = model.getBestOutcome(probs);
	        double prevTokenProb = probs[model.getIndex(prevBest)];
	        
		    for(int idx = 1 ; idx < newTokens.size()-1 ; idx++){
		    	int startJoin = newTokens.get(idx).getStart();
		    	int endJoin = newTokens.get(idx+1).getEnd();
		    	midJoin = d.substring(startJoin, newTokens.get(idx).getEnd()).replaceAll(" ", "").length();//newTokens.get(idx).getEnd() - newTokens.get(idx).getStart(); 
		    	
		    	String joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
		        probs = model.eval(cg.getContext(joinedTok, midJoin));
		        String best = model.getBestOutcome(probs);
		        double tokenProb = probs[model.getIndex(best)];
		        
		        int prevIdx = idx-1; 
		        if (prevBest.equals(TokenizerME.NO_SPLIT) 
	        		&& (!best.equals(TokenizerME.NO_SPLIT) || prevTokenProb > tokenProb)) {
		        	
		        	newTokens.set(prevIdx, new Span(prevStartJoin, prevEndJoin));
		        	//tokProbs.set(prevIdx, prevTokenProb);
		            newTokens.remove(prevIdx+1);
		            //tokProbs.remove(prevIdx+1);
		            //tokList.add(prevJoinedTok);
		            
		            //recalculate for new token
		            startJoin = newTokens.get(prevIdx).getStart();
			    	endJoin = newTokens.get(prevIdx+1).getEnd();
			    	midJoin =  d.substring(startJoin, newTokens.get(prevIdx).getEnd()).replaceAll(" ", "").length(); //newTokens.get(prevIdx).getEnd() - newTokens.get(prevIdx).getStart();
			    	
			    	joinedTok = d.substring(startJoin, endJoin).replaceAll(" ", "");
			        probs = model.eval(cg.getContext(joinedTok, midJoin));
			        best = model.getBestOutcome(probs);
			        tokenProb = probs[model.getIndex(best)];
			        
		            idx--;
		        }
		        else{
		        	tokList.add(d.substring(newTokens.get(prevIdx).getStart(), newTokens.get(prevIdx).getEnd()).replaceAll(" ", ""));

		        }
		        prevStartJoin = startJoin;
		        prevEndJoin = endJoin;
		        prevJoinedTok = joinedTok;
		        prevTokenProb = tokenProb;
		        prevBest = best;
		    }
		    
		    int prevIdx = newTokens.size()-2; 
		    if (prevBest.equals(TokenizerME.NO_SPLIT)) {
		        	newTokens.set(prevIdx, new Span(prevStartJoin, prevEndJoin));
//		        	tokProbs.set(prevIdx, prevTokenProb);
		            newTokens.remove(prevIdx+1);
//		            tokProbs.remove(prevIdx+1);
		            tokList.add(prevJoinedTok);
		    }
		    else{
	        	tokList.add(d.substring(newTokens.get(prevIdx).getStart(), newTokens.get(prevIdx).getEnd()).replaceAll(" ", ""));
	        	tokList.add(d.substring(newTokens.get(prevIdx+1).getStart(), newTokens.get(prevIdx+1).getEnd()).replaceAll(" ", ""));
		    }
	    }
	    String[] toks = new String[tokList.size()];
	    tokList.toArray(toks);
	    return toks;
		  
	  }
	  
	  
	  
	  /**
	   * Trains a model for the {@link TokenizerNormalizer}.
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
