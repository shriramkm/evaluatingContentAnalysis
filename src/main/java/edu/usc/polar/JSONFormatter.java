package edu.usc.polar;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class that formats the output of the LanguageDetector.java's 
 * in such a way that it can be consumed by the D3 visualization
 * 
 * @author shriram
 *
 */
public class JSONFormatter {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		File inputFile = new File("/home/shriram/Desktop/CSCI 599/Assignment 3/language.json");
		String source = "";
		try {
			source = FileUtils.readFileToString(inputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONObject jsonObj = new JSONObject(source);
		JSONArray outputJSON = new JSONArray();
		if(null != jsonObj){
			Iterator keys = jsonObj.keys();
			while(keys.hasNext()){
				JSONObject outputJsonObj = new JSONObject();
				String language = (String)keys.next();
				int noOfFiles = (int)Double.parseDouble((String)jsonObj.get(language));
				outputJsonObj.put("label",language);
				outputJsonObj.put("value", noOfFiles);
				outputJSON.put(outputJsonObj);
			}
		}
		System.out.println(outputJSON);
	}
}
