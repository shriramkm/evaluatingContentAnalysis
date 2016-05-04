package edu.usc.polar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Class that invokes the Grobid Quanitites on files in the dataset
 * to retrieve measurements present in the file
 * 
 * @author shriram
 *
 */
public class GrobidQuantities {
	private static PostMethod method = new PostMethod("http://localhost:8080/processQuantityText");
	private static HttpClient httpclient = new HttpClient();
	private static Tika tika = new Tika();
	public static String getGrobidQuantitiesResponse(String content, String mimetype) throws HttpException, IOException {
		ArrayList<String> splitContent = new ArrayList<String>();

		// Grobid quantities works fine with strings lesser than 3000 char limit
		// Hence the input string is split into substrings of 2990 char limit
		// and sent to the server
		int j = 0;
		if (content.length() > 2990) {
			for (int i = 0; i < content.length(); i += 2990) {
				splitContent.add(content.substring(i, ((i + 2990) > content.length()) ? content.length() : i + 2990));
			}
		} else {
			splitContent.add(content);
		}
		String prefix = "";
		StringBuilder result = new StringBuilder();
		for (String splitCont : splitContent) {
			HttpClientParams params = new HttpClientParams();
			params.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
			try{
				httpclient.setParams(params);
				NameValuePair nameValuePair = new NameValuePair("text", splitCont);
				NameValuePair[] nvPairArray = { nameValuePair };
				method.setQueryString(nvPairArray);
	
				httpclient.executeMethod(method);
				InputStream stream = method.getResponseBodyAsStream();
				List<String> str = IOUtils.readLines(stream);
				result.append(prefix);
				prefix = ",";
				result.append("{\"" + mimetype + "\":" + str + "}");
				System.out.println("{\"" + mimetype + "\":" + str + "}");
			}
			catch(Exception e){
				//e.printStackTrace();
				System.out.println(e.getMessage());

			}
			finally{
				method.releaseConnection();
			}
			//System.out.println(result);
			// WRITE result TO FILE
		}
		return result.toString();
	}

	public static void main(String[] arg) throws SAXException, TikaException {
		//File inputFile = new File(
		//		"/Users/nikitharathnakar/Documents/application-pdf/FC48EBF8838CD97E20D4D566886B0DE72EE203419164F18D61D4052F00CEB936");
		List<String> inputFilePaths = EvaluationUtils.getFilePaths();
		StringBuilder resultJSON = new StringBuilder();
		String prefix = "";
		for(String inputFilePath : inputFilePaths){
			File inputFile = new File(inputFilePath);
			Tika tika = new Tika();
			try {
				System.out.println(inputFilePath);
				String mimetype = tika.detect(inputFile);
				AutoDetectParser parser = new AutoDetectParser();
				ContentHandler handler = new BodyContentHandler(-1);
				Metadata metadata = new Metadata();
				FileInputStream inputstream = new FileInputStream(inputFile);
				ParseContext context = new ParseContext();

				// parsing the file
				parser.parse(inputstream, handler, metadata, context);
				String handlerArr = handler.toString();
				resultJSON.append(prefix);
				prefix = ",";
				resultJSON.append(getGrobidQuantitiesResponse(handlerArr, mimetype));
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println(e.getMessage());

			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println(e.getMessage());
			}
		}
		try{
			System.out.println(resultJSON.toString());
			//JSONObject jsonObj = new JSONObject(resultJSON.toString());
			EvaluationUtils.writeJSONToFile(resultJSON.toString(), "/home/shriram/Desktop/grobidquantities.json");
		}
		catch(Exception e){
			//e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}