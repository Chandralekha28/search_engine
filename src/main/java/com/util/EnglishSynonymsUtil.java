package com.util;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class EnglishSynonymsUtil {
	public Set<String> findEnglishSynonyms(String query){
		Set<String> synonyms = new HashSet<>();
		try{
			File fout = new File("dict/"); 
			IDictionary dict = new Dictionary (fout);
			dict.open();

			IIndexWord idxWord = dict.getIndexWord (query, POS. NOUN );
			synonyms.addAll(dictIterator(idxWord,dict));
			  
			idxWord = dict.getIndexWord (query, POS. VERB );
			synonyms.addAll(dictIterator(idxWord,dict));
			  
			idxWord = dict.getIndexWord (query, POS. ADVERB );
			synonyms.addAll(dictIterator(idxWord,dict));
			  
			idxWord = dict.getIndexWord (query, POS. ADJECTIVE );
			synonyms.addAll(dictIterator(idxWord,dict));
			  
			if(synonyms.size()==0)
				synonyms.add(query);
			System.out.println("Synonyms "+synonyms);
			return synonyms;  
		} catch (Exception e){
			e.printStackTrace();
		}
		return synonyms;
	}
	
	public Set<String> dictIterator( IIndexWord idxWord,IDictionary dict){
		 Set<String> synonyms = new HashSet<>();
		 if(idxWord!=null){
			 Iterator<IWordID> wordID = idxWord.getWordIDs ().iterator(); 
			  while(wordID.hasNext()){
				  IWordID h = wordID.next();
				  IWord word = dict . getWord (h);
				  ISynset synsetNoun = word . getSynset ();
				  for( IWord w : synsetNoun . getWords ()){
					  if(!w.getLemma().contains("_"))
						  synonyms.add(w.getLemma());
				  }	  
			  }
		 }
		 return synonyms;
	}
}
