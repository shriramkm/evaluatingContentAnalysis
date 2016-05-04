/**
 * 
 */
package edu.usc.polar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;

import com.google.gson.JsonObject;

/**
 * Helper method that contains methods to update the maps
 * corresponding to file size diversity, language diversity,
 * mimetype diversity and parser chain hierarchies. It also
 * contains a method to traverse the dataset and return a list
 * of files to be processed, a method to write a JSON object
 * onto a file on the disk and a method to convert a map
 * to a JSON
 * 
 * @author shriram
 *
 */
public class EvaluationUtils {
	private static Map<String,Double> languageDiversityMap;
	private static Map<String,Double> sizeDiversityMap;
	private static Map<String,Double> numberDiversityMap;
	private static Map<String,String> parsersMap;
	private static Map<String,String> parsersAltMap;
	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		Tika tika = new Tika();
		languageDiversityMap = new HashMap<String,Double>();
		sizeDiversityMap = new HashMap<String,Double>();
		numberDiversityMap = new HashMap<String,Double>();
		parsersMap = new HashMap<String,String>();
		parsersAltMap = new HashMap<String,String>();
		List<String> filePaths = getFilePaths();
		InputStream is = null;
		for(String path : filePaths){
			System.out.println(path);
			File file = new File(path);
			try{
				if(null != is){
					is.close();
					is = null;
				}
				is = new FileInputStream(file);
				//Metadata metadata = new Metadata();
				Metadata altMetadata = new Metadata();
				String filecontent = "";
				String mimetype = tika.detect(file);
				/*try{
					tika.parse(file, metadata);
				}
				catch(Exception e){
					System.out.println("Tika could not parse "+path);
				}
				catch(Error e){
					System.out.println("Error - Tika could not parse "+path);
				}*/
				try{
					AutoDetectParser adparser = new AutoDetectParser();
					ContentHandler handler = new BodyContentHandler(-1);
					adparser.parse(is, handler, altMetadata);
					//filecontent = tika.parseToString(file);
					filecontent = handler.toString();
					is.close();
					is = null;
				}
				catch(Exception e){
					System.out.println("Tika could not parse to string "+path);
					e.printStackTrace();
				}
				catch(Error e){
					System.out.println("Error : Tika could not parse to string "+path);
				}
				/*List<String> parsers = new ArrayList<String>();
					if(!parsersMap.containsKey(mimetype)){
						if(null != metadata.get("X-Parsed-By")){
							if(metadata.isMultiValued("X-Parsed-By")){
								parsers = Arrays.asList(metadata.getValues("X-Parsed-By"));
							}
							else{
								parsers.add(metadata.get("X-Parsed-By"));
							}
						}
						if(parsers.size()>0){
							parsersMap.put(mimetype, parsers.toString());
						}
					}
					else{
						if(parsersMap.get(mimetype).contains("org.apache.tika.parser.EmptyParser")){
							if(parsers.size()>0){
								parsersMap.put(mimetype, parsers.toString());
							}
						}
					}*/
				//parsersMap = updateMetadataMap(metadata,parsersMap,mimetype);
				//parsersAltMap = updateMetadataMap(altMetadata,parsersAltMap,mimetype);
				String language = LanguageDetector.detectLanguage(filecontent);
				languageDiversityMap = updateMap(languageDiversityMap,language,1.0);
				numberDiversityMap = updateMap(numberDiversityMap,mimetype,1.0);
				sizeDiversityMap = updateMap(sizeDiversityMap,mimetype,(double)file.length());
			}
			catch(Exception e){
				System.out.println("Exception in updating maps of "+path);
				e.printStackTrace();
			}
			catch(Error e){
				System.out.println("Error - Tika could not parse "+path);
				e.printStackTrace();
			}
			finally{
				if(null != is){
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		try{
			JSONObject jsonObj = new JSONObject();
			Set<String> keys = parsersMap.keySet();
			for(String key : keys){
				jsonObj.put(key, parsersMap.get(key));
			}
			writeJSONToFile(jsonObj, "/home/shriram/Desktop/parsers.json");
			/*jsonObj = new JSONObject();
			keys = parsersAltMap.keySet();
			for(String key : keys){
				jsonObj.put(key, parsersAltMap.get(key));
			}
			writeJSONToFile(jsonObj, "/home/shriram/Desktop/altparsers.json");*/
		}
		catch(Exception e){
			System.out.println("Exception in creating parsers JSON");
		}
		try{
			convertMapToJSON(languageDiversityMap,"/home/shriram/Desktop/language.json");
		}
		catch(Exception e){
			System.out.println("Exception in creating language diversity JSON");
		}
		try{
			convertMapToJSON(numberDiversityMap,"/home/shriram/Desktop/number.json");
		}
		catch(Exception e){
			System.out.println("Exception in creating number diversity JSON");
		}
		try{
			convertMapToJSON(sizeDiversityMap,"/home/shriram/Desktop/size.json");
		}
		catch(Exception e){
			System.out.println("Exception in creating size diversity JSON");
		}
	}

	public static Map<String,String> updateMetadataMap(Metadata metadata, Map<String,String> parsersMap, String mimetype){
		List<String> parsers = new ArrayList<String>();
		if(!parsersMap.containsKey(mimetype)){
			if(null != metadata.get("X-Parsed-By")){
				if(metadata.isMultiValued("X-Parsed-By")){
					parsers = Arrays.asList(metadata.getValues("X-Parsed-By"));
				}
				else{
					parsers.add(metadata.get("X-Parsed-By"));
				}
			}
			if(parsers.size()>0){
				parsersMap.put(mimetype, parsers.toString());
			}
		}
		else{
			if(parsersMap.get(mimetype).contains("org.apache.tika.parser.EmptyParser")){
				if(parsers.size()>0){
					parsersMap.put(mimetype, parsers.toString());
				}
			}
		}
		return parsersMap;
	}

	public static void convertMapToJSON(Map<String,Double> map,String outputFilePath) throws IOException{
		JSONObject jsonObj = new JSONObject();
		Set<String> keys = map.keySet();
		for(String key : keys){
			jsonObj.put(key, String.format("%f",map.get(key)));
		}
		writeJSONToFile(jsonObj, outputFilePath);
	}

	public static Map<String,Double> updateMap(Map<String,Double> originalMap, String key, Double increaseBy){
		Double count = 0.0;
		if(originalMap.containsKey(key)){
			count = originalMap.get(key);
		}
		originalMap.put(key, count+increaseBy);
		return originalMap;
	}

	/**
	 * 
	 * @return
	 */
	public static List<String> getFilePaths(){

		Tika tika = new Tika();
		List<String> paths = new ArrayList<String>();

		//Directory that points to our dataset
		File dir = new File("/home/shriram/Documents/572-team1");
		//File dir = new File("/home/shriram/Desktop/CSCI 599/Assignment3_Sample_Dataset");
		//Do not consider the intermediate results as input
		IOFileFilter[] filters = {
				new NotFileFilter(new SuffixFileFilter("_1.json")),
				new NotFileFilter(new SuffixFileFilter("_2.json")),
				new NotFileFilter(new SuffixFileFilter("_3.json")),
				new NotFileFilter(new SuffixFileFilter("_4.json")),
				new NotFileFilter(new SuffixFileFilter("_5.json")),
				new NotFileFilter(new SuffixFileFilter("_6.json")),
				new NotFileFilter(new SuffixFileFilter("_solr.json"))};
		IOFileFilter finalFilter = new RegexFileFilter("^(.*?)"); 
		for(IOFileFilter filter : filters){
			finalFilter = new AndFileFilter(finalFilter,filter);
		}
		Collection files = FileUtils.listFiles(
				dir, 
				finalFilter,
				DirectoryFileFilter.DIRECTORY
				);
		Iterator it = files.iterator();
		while(it.hasNext()){
			File file = (File)it.next();
			try{
				//String mimetype = tika.detect(file);
				//System.out.println(file.getPath());
				paths.add(file.getAbsolutePath());
			}
			catch(Exception e){
				System.out.println("Exception while getting file paths!");
				e.printStackTrace();
			}
		}
		return paths;
	}

	/**
	 * This method writes the jsonObj JSON Object as a string
	 * to the file at the outputFilePath path.
	 * 
	 * @param jsonObj
	 * @param outputFilePath
	 * @throws IOException
	 */
	
	
	public static void writeJSONToFile(JSONObject jsonObj, String outputFilePath) throws IOException
	{
		FileWriter file = new FileWriter(outputFilePath);
		file.write(jsonObj.toString());
		file.close();
	}
	
	public static void writeJSONToFile(String jsonObj, String outputFilePath) throws IOException
	{
		FileWriter file = new FileWriter(outputFilePath);
		file.write(jsonObj.toString());
		file.close();
	}
}
