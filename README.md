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

### Training the Tokenizer
As mentioned, TokenizerMe can be trained to create a new model. In general, the following three steps are required for training:

- First, the input stream related to the training data file needs to be opened.

- The method TokenizerME.train should be called.

- The trained TokenizerModel should be saved in a file or used directly.

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

// Training the model
String language = "fa";
TokenizerFactory tokenFactory = TokenizerFactory.create(null, language, null, true, null);

TrainingParameters params = new TrainingParameters().defaultParams();
params.put(TrainUtil.ITERATIONS_PARAM, "500");

TokenizerModel model = TokenizerME.train(sampleStream, tokenFactory, params);

// Saving model to a file
modelOut = new BufferedOutputStream(new FileOutputStream("MODEL_FILE_NAME"));
model.serialize(modelOut);

```

The format of the training data file is such that each line should contain only one sentence. Tokens should be separated from each other using whitespace or the tag "<SPLIT>". An example of the training data is shown below.

```
Pierre Vinken<SPLIT>, 61 years old<SPLIT>, will join the board as a nonexecutive director Nov. 29<SPLIT>.
Mr. Vinken is chairman of Elsevier N.V.<SPLIT>, the Dutch publishing group<SPLIT>.
Rudolph Agnew<SPLIT>, 55 years old and former chairman of Consolidated Gold Fields PLC<SPLIT>, was named a nonexecutive director of this British industrial conglomerate<SPLIT>.
```

In general, it can be said that the TokenizerMe.train method involves several steps:
- Creating events from the data,
- Indexing them,
- Training the model.

### Format of the event file
In the event creation step, each sentence is converted into multiple events. The formation of these events is as follows: first, the desired sentence is broken down into tokens based on whitespace. Then, each token is considered separately, and events are formed accordingly. Specifically, each character within a token is taken as a delimiter point. If the delimiter point contains the "<SPLIT>" tag, the corresponding event is labeled as class T. Otherwise, it is labeled as class F. For example, the events for the given sentence are specified as follows.

<p align="center">
  <a href="https://github.com/hamiGH/persian-nlp/blob/feature_update_readme/images/training_data.png" target="_blank"><img src="https://github.com/hamiGH/persian-nlp/blob/feature_update_readme/images/training_data.png"></a>
</p>

<img src="https://github.com/hamiGH/persian-nlp/blob/feature_update_readme/images/training_data.png" alt="Image" width="500" height="150">



<!-- | Format   | Sentence | -->
<!-- |----------|--------| -->
<!-- | simple   | «خبر:«بخریم یا نخریم؟   | -->
<!-- | training  | «<SPLIT>؟<SPLIT>بخریم یا نخریم<SPLIT>»<SPLIT>:<SPLIT>  | خبر -->



