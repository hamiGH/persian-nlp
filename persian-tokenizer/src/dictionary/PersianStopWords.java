/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package dictionary;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Several sets of English stop words.
 *
 * @author Haifeng Li
 */
public enum PersianStopWords implements StopWords {

    /**
     * Default stop words list.
     */
		
    DEFAULT("resources/dictionary/stop-words_fa.txt");
    /**
     * A very long list of stop words.
     */
//    COMPREHENSIVE("/smile/nlp/dictionary/stop-words_en_more.txt"),
    /**
     * The stop words list used by Google.
     */
//    GOOGLE("/smile/nlp/dictionary/stop-words_en_google.txt"),
    /**
     * The stop words list used by MySQL FullText feature.
     */
//    MYSQL("/smile/nlp/dictionary/stop-words_en_mysql.txt");
    
    /**
     * A set of stop words.
     */
    public HashSet<String> dict;

    
	
    
    /**
     * Constructor.
     */
    private PersianStopWords(String resource) {
        dict = new HashSet<>();

//        try (BufferedReader input = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(resource)))) {
        try (BufferedReader input = new BufferedReader(new FileReader(resource))){
        	
            String line = null;
            while ((line = input.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    dict.add(line);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean contains(String word) {
        return dict.contains(word);
    }

    @Override
    public int size() {
        return dict.size();
    }

    @Override
    public Iterator<String> iterator() {
        return dict.iterator();
    }
}
