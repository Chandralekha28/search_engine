package com.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class SpellCheckUtil{
	
	private HashMap <String,Double> wordFreqEng= new HashMap<String,Double>();
	private HashMap <String,Double> wordFreqGer= new HashMap<String,Double>();
	 public SpellCheckUtil() {
		 loadDictionaryEnglish();
		 loadDictionaryGerman();
	 }

	public ArrayList<String> spellCheck(String query)
	{
		query= query.toLowerCase();
		
		String splitQuery [] = query.split("\\s+");
		
		for(int i=0; i<splitQuery.length; i++){
			
			TreeMap<Integer, String> corrections = new TreeMap<Integer, String>();
			for(String word: wordFreqEng.keySet()){
				int dist;
				LevenshteinDistance ld = new LevenshteinDistance();
				if(splitQuery[i].startsWith("\"")){
					dist=ld.apply(word,splitQuery[i].replaceAll("^\\\"|\\\"$",""));
				}
				else{
					dist=ld.apply(word,splitQuery[i] );
				}
				
				if(dist==1 | dist==2){
					if(corrections.containsKey(dist)){
						String oldvalue = corrections.get(dist);
						if(wordFreqEng.get(oldvalue)< wordFreqEng.get(word))
							corrections.put(dist, word);
					}
					else
						corrections.put(dist, word);
				}
				
			}
			
			if(!corrections.isEmpty()){
				if(splitQuery[i].startsWith("\"")){
					splitQuery[i]="\""+corrections.pollFirstEntry().getValue().toLowerCase()+"\"";
				}
			
				else
					splitQuery[i]=corrections.pollFirstEntry().getValue().toLowerCase();
			}	
		}
		System.out.println(String.join(" ", splitQuery));
		ArrayList<String> b= new ArrayList<String>();
		for(int i=0;i<splitQuery.length;i++)
			b.add(splitQuery[i]);
		return b;
	}
	
	private void loadDictionaryEnglish(){
		File dictEng= new File("english_words.txt");
		try{
			Scanner sceng = new Scanner(dictEng).useDelimiter("\n");
			while (sceng.hasNextLine()){ 
				String line = sceng.nextLine();
				String[] split = line.split("\\s+");
				wordFreqEng.put(split[0].toLowerCase(), Double.parseDouble(split[1]));
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}

	private void loadDictionaryGerman() {
		File dictEng= new File("german_words.txt");
		try{
			Scanner sceng = new Scanner(dictEng).useDelimiter("\n");
			while (sceng.hasNextLine()){ 
				String line = sceng.nextLine();
				String[] split = line.split("\\s+");
				wordFreqGer.put(split[0].toLowerCase(), Double.parseDouble(split[1]));
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}

	public ArrayList<String> spellCheckGerman(String query) {
		query= query.toLowerCase();
		
		String splitQuery [] = query.split("\\s+");
		
		for(int i=0; i<splitQuery.length; i++){
			
			TreeMap<Integer, String> corrections = new TreeMap<Integer, String>();
			for(String word: wordFreqGer.keySet()){
				int dist;
				LevenshteinDistance ld = new LevenshteinDistance();
				if(splitQuery[i].startsWith("\"")){
					dist=ld.apply(word,splitQuery[i].replaceAll("^\\\"|\\\"$",""));
				}
				else{
					dist=ld.apply(word,splitQuery[i] );
				}
				
				if(dist==1 | dist==2){
					if(corrections.containsKey(dist)){
						String oldvalue = corrections.get(dist);
						if(wordFreqGer.get(oldvalue)< wordFreqGer.get(word))
							corrections.put(dist, word);
					}
					else
						corrections.put(dist, word);
				}
				
			}
			
			if(!corrections.isEmpty()){
				if(splitQuery[i].startsWith("\"")){
					splitQuery[i]="\""+corrections.pollFirstEntry().getValue().toLowerCase()+"\"";
				}
			
				else
					splitQuery[i]=corrections.pollFirstEntry().getValue().toLowerCase();
			}	
		}
		System.out.println(String.join(" ", splitQuery));
		ArrayList<String> b= new ArrayList<String>();
		for(int i=0;i<splitQuery.length;i++)
			b.add(splitQuery[i]);
		return b;
	}
}
