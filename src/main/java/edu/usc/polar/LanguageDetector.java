package edu.usc.polar;

import org.apache.tika.language.LanguageIdentifier;

/**
 * Class that performs language detection of a file based
 * on its contents
 * 
 * @author shriram
 *
 */
public class LanguageDetector {

	public static String detectLanguage(String content) {
		// TODO Auto-generated method stub
		LanguageIdentifier identifier = new LanguageIdentifier(content);
		String language = identifier.getLanguage();
		//System.out.println("Language of the given content is : " + language);
		return language;
	}
}
