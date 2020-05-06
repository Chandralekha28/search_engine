package com.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GermanSynonymsUtil{
	ArrayList<ArrayList<String>> germanDictionary;
	
	public GermanSynonymsUtil(){
		this.germanDictionary = loadGermanSynonymDictionary();
	}
	
	public ArrayList<ArrayList<String>> loadGermanSynonymDictionary(){
		ArrayList<ArrayList<String>> germanSynonyms2 = new ArrayList<ArrayList<String>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader("Germanopenthesaurus.txt"));
			String line = reader.readLine();
			while (line != null) {
				String[] split = line.split(";");
				ArrayList<String> itemsList = new ArrayList<String>();
				for(int i=0 ; i< split.length ;i++){
					Matcher m = Pattern.compile("\\(.*?\\)").matcher(split[i]);
					split[i]=split[i].trim().toLowerCase();
					if(m.find()){
						String temp = processStringWithBracket(split[i]);
						if(!temp.contains(" ")){
							split[i]=temp;
							itemsList.add(split[i]);
						}
					}
					else if(!split[i].contains(" "))
						itemsList.add(split[i]);
				}
				germanSynonyms2.add(itemsList);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return germanSynonyms2;
	}
	
	private String processStringWithBracket(String str){
		String temp = str.replaceAll("\\(.*?\\)","").trim();
		return temp;
	}
	
	public Set<String> findGermanSynonyms(String query){
		Set<String> queryTokens= new HashSet<String>();
		for(int i=0;i< germanDictionary.size(); i++){
			ArrayList<String> temp = new ArrayList<String>();
		    temp = germanDictionary.get(i);
		    if(temp.contains(query)){
		    	queryTokens.addAll(temp);
		    }
		}
		if(queryTokens.size()==0)
			queryTokens.add(query);
		System.out.println("Synonyms "+queryTokens);
		return queryTokens;
	}
}

