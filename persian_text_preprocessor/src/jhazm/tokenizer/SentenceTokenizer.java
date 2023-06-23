package jhazm.tokenizer;

import jhazm.utility.RegexPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Mojtaba Khallash
 */
public class SentenceTokenizer {
    public static SentenceTokenizer instance;
    private final RegexPattern pattern;
    
    public static final SentenceTokenizer INSTANCE = new SentenceTokenizer();
    
    public SentenceTokenizer() {
//        this.pattern = new RegexPattern("([!\\.\\?⸮؟]+)[ \\n]+", "$1\n\n");
    	this.pattern = new RegexPattern("(([!\\.\\?⸮؟]+)[ \\n]+)|([\\n]+)", "$1\n\n");
    }

    public static SentenceTokenizer i() {
        if (instance != null) return instance;
        instance = new SentenceTokenizer();
        return instance;
    }

    public List<String> tokenize(String text) {
        text = this.pattern.apply(text);
        List<String> sentences = Arrays.asList(text.split("\n\n"));
        List<String> sentences2 = new ArrayList<String>();
        for (String sentence : sentences) {
//            sentence = sentence.replace("\n", " ").trim();
        	if(!sentence.trim().isEmpty()){
        		sentences2.add(sentence.trim());
        	}
        }
        return sentences2;
    }
}