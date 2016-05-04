package edu.usc.polar;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.ner.nltk.NLTKNERecogniser;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

/**
 * Class that traverses each file in the dataset and extracts 
 * entities and keeps track of the count of these entities.
 * The class also generates a JSON which contains this information
 * 
 * @author shriram
 *
 */
public class CompositeNERAgreementParser {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		List<String> filePaths = EvaluationUtils.getFilePaths();
		Map<String,Integer> nltkEntities = new HashMap<String,Integer>();
		Map<String,Integer> coreNLPEntities = new HashMap<String,Integer>();
		Map<String,Integer> openNLPEntities = new HashMap<String,Integer>();
		String contents = "";
		for(String filePath : filePaths){
			Tika tika = new Tika();
			try {
				contents = tika.parseToString(new File(filePath));
			} catch (TikaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}

			//NLTK
			NLTKNERecogniser recog = new NLTKNERecogniser();
			Map<String,Set<String>> ner = recog.recognise(contents);
			Set<String> keys = ner.keySet();
			for(String key : keys){
				Set<String> entries = ner.get(key);
				for(String entry : entries){
					updateMap(nltkEntities, coreNLPEntities, openNLPEntities, entry);
				}
			}

			//Stanford CoreNLP
			Properties p = new Properties();
			p.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(p);

			Annotation document = new Annotation(contents);
			pipeline.annotate(document);

			for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
				for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

					String word = token.get(CoreAnnotations.TextAnnotation.class);
					String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
					updateMap(coreNLPEntities, openNLPEntities, nltkEntities, word);
				}
			}

			//OpenNLP
			// Load the model file downloaded from OpenNLP
			// http://opennlp.sourceforge.net/models-1.5/en-ner-person.bin
			TokenNameFinderModel model1 = new TokenNameFinderModel(new File(
					"/home/shriram/opennlp/models/en-ner-person.bin"));
			TokenNameFinderModel model2 = new TokenNameFinderModel(new File(
					"/home/shriram/opennlp/models/en-ner-location.bin"));
			TokenNameFinderModel model3 = new TokenNameFinderModel(new File(
					"/home/shriram/opennlp/models/en-ner-money.bin"));
			TokenNameFinderModel model4 = new TokenNameFinderModel(new File(
					"/home/shriram/opennlp/models/en-ner-organization"));
			TokenNameFinderModel model5 = new TokenNameFinderModel(new File(
					"/home/shriram/opennlp/models/en-ner-percentage.bin"));
			TokenNameFinderModel model6 = new TokenNameFinderModel(new File(
					"/home/shriram/opennlp/models/en-ner-time.bin"));

			// Create a NameFinder using the model
			NameFinderME finder1 = new NameFinderME(model1);
			NameFinderME finder2 = new NameFinderME(model2);
			NameFinderME finder3 = new NameFinderME(model3);
			NameFinderME finder4 = new NameFinderME(model4);
			NameFinderME finder5 = new NameFinderME(model5);
			NameFinderME finder6 = new NameFinderME(model6);

			Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
			String sentences[] = {contents};
			if(contents.contains("\n")){
				sentences = contents.split("\n");
			}
			for (String sentence : sentences) {

				// Split the sentence into tokens
				String[] tokens = tokenizer.tokenize(sentence);

				// Find the names in the tokens and return Span objects
				Span[] nameSpans = finder1.find(tokens);
				Span[] locationSpans = finder2.find(tokens);
				Span[] moneySpans = finder3.find(tokens);
				Span[] organizationSpans = finder4.find(tokens);
				Span[] percentageSpans = finder5.find(tokens);
				Span[] timeSpans = finder6.find(tokens);
				Span[][] spanArr = {nameSpans,locationSpans,moneySpans,organizationSpans,percentageSpans,timeSpans};
				for(Span[] s: spanArr){
					for(Span s1 : s){
						updateMap(openNLPEntities, nltkEntities, coreNLPEntities, s1.toString());
					}
				}
			}
		}

		JSONObject jsonObj = new JSONObject();
		String outputFilePath = "/home/shriram/Desktop/CompositeNERAgreement.json";
		updateJSONs(jsonObj,nltkEntities,"nltk");
		updateJSONs(jsonObj,coreNLPEntities,"coreNLP");
		updateJSONs(jsonObj,openNLPEntities,"openNLP");

		JSONArray entitiesObj = new JSONArray();
		JSONArray nltkObj = new JSONArray();
		JSONArray coreNLPObj = new JSONArray();
		JSONArray openNLPObj = new JSONArray();

		Set keys = jsonObj.keySet();
		for(Object key : keys){
			JSONObject obj = (JSONObject) jsonObj.get((String)key);
			entitiesObj.put((String)key);
			nltkObj.put((Integer)obj.get("nltk"));
			coreNLPObj.put((Integer)obj.get("coreNLP"));
			openNLPObj.put((Integer)obj.get("openNLP"));
		}

		jsonObj = new JSONObject();
		jsonObj.put("labels", entitiesObj);
		JSONArray seriesObj = new JSONArray();
		JSONObject libraryObj1 = new JSONObject();
		JSONObject libraryObj2 = new JSONObject();
		JSONObject libraryObj3 = new JSONObject();
		libraryObj1.put("name", "nltk");
		libraryObj1.put("value", nltkObj);
		libraryObj2.put("name", "coreNLP");
		libraryObj2.put("value", coreNLPObj);
		libraryObj3.put("name", "openNLP");
		libraryObj3.put("value", openNLPObj);
		seriesObj.put(libraryObj1);
		seriesObj.put(libraryObj2);
		seriesObj.put(libraryObj3);
		jsonObj.put("series", seriesObj);

		EvaluationUtils.writeJSONToFile(jsonObj, outputFilePath);
	}

	public static JSONObject updateJSONs(JSONObject jsonObj, Map<String,Integer> map, String library){
		Set<String> keys = map.keySet();
		for(String key : keys){
			if(jsonObj.has(key)){
				JSONObject obj = (JSONObject) jsonObj.get(key);
				obj.put(library,map.get(key));
				jsonObj.put(key, obj);
			}
			else{
				JSONObject obj = new JSONObject();
				obj.put(library,map.get(key));
				jsonObj.put(key, obj);
			}
		}
		return jsonObj;
	}

	public static Map<String,Integer> updateMap(Map<String,Integer> inputMap, Map<String,Integer> inputMap1, Map<String,Integer> inputMap2, String key){
		if(inputMap.containsKey(key)){
			inputMap.put(key, inputMap.get(key)+1);
		}
		else{
			inputMap.put(key, 1);
		}
		if(!inputMap1.containsKey(key)){
			inputMap1.put(key, 0);
		}
		if(!inputMap2.containsKey(key)){
			inputMap2.put(key, 0);
		}
		return inputMap;
	}
}