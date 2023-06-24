# Persian NLP

A Java-based repository providing preprocessing, keyword extraction, and summarization functionalities tailored for Persian language text data.

## Introduction to Opennlp Tokenizer

The OpenNlp library provides the opennlp.tools.tokenize package for tokenization. This package offers three different classes for the tokenization process:
- **SimpleTokenizer**: Performs tokenization based on character classes.
- **WhitespaceTokenizer**: Considers whitespace as the separator between tokens.
- **TokenizerMe**: This class uses the maximum entropy method to detect token boundaries. It utilizes a probabilistic model that can be trained. The method calculates the probability at each point in the input string and extracts tokens accordingly.
To use the TokenizerMe class, you first need to load the desired model and assign it to a TokenizerModel. Then, you can perform tokenization using it.

```java
//Loading the Tokenizer model from file
InputStream inputStream = new FileInputStream("en-token.bin"); 
TokenizerModel tokenModel = new TokenizerModel(inputStream); 
	       
//Instantiating the TokenizerME class 
TokenizerME tokenizer = new TokenizerME(tokenModel); 
	       
//Tokenizing the given raw text 
String tokens[] = tokenizer.tokenize(sentence);  
```

## Training the Tokenizer
As mentioned, TokenizerMe can be trained to create a new model. In general, the following three steps are required for training:

• First, the input stream related to the training data file needs to be opened.

• The method TokenizerME.train should be called.

• The trained TokenizerModel should be saved in a file or used directly.

```java
// Opening a training data stream
final String fileName = "data.train";
InputStreamFactory isf = new InputStreamFactory() {
    public InputStream createInputStream() throws IOException {
        return new FileInputStream(fileName);
    }
}

Charset charset = Charset.forName("UTF-8");
ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

// Training model
String language = "fa";
TokenizerFactory tokenFactory = TokenizerFactory.create(null, language, null, true, null);

TrainingParameters params = new TrainingParameters().defaultParams();
params.put(TrainUtil.ITERATIONS_PARAM, "500");

TokenizerModel model = TokenizerME.train(sampleStream, tokenFactory, params);

// Saving model to file
modelOut = new BufferedOutputStream(new FileOutputStream("MODEL_FILE_NAME"));
model.serialize(modelOut);

```


