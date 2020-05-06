package com.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageUtil {

	public String checkForLanguage(ArrayList<String> germanWords, ArrayList<String> englishWords,String pageContents) {
		Pattern p = Pattern.compile("[a-zA-Z]+"); 
		Matcher m1 = p.matcher(pageContents); 
		ArrayList<String> allTerms = new ArrayList<String>();
		while (m1.find()) { 
			allTerms.add( (m1.group()).toLowerCase());
		} 
		int engCount =0, germanCount = 0;
		for(String term: allTerms) {
			if(!germanWords.contains(term)){
				germanCount+= Math.log(1/57699127.0);
			}
			if(!englishWords.contains(term)){
				engCount+= Math.log(1/46387276.0);
			}
		}
		//System.out.println(engCount +" count "+germanCount);
		if(engCount>germanCount)
			return "en";
		else
			return "de";
	}

}
