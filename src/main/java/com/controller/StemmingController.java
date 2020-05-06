package com.controller;

import java.util.ArrayList;

import com.service.StemmingService;

public class StemmingController {

	public ArrayList<String> stem(String content) {
		char[] w = new char[501];
	    StemmingService stemmingService = new StemmingService();
		ArrayList<String> terms = new ArrayList<String>();
		int z=0;
	    for (int i = 0; i < 1; i++) {
			while(z<content.length())

	           {  int ch = content.charAt(z++);
	           	
	              if (Character.isLetter((char) ch))
	              {
	                 int j = 0;
	                 while(true)
	                 {  ch = Character.toLowerCase((char) ch);
	                    w[j] = (char) ch;
	                    if (j < 500) j++;
	                    ch = content.charAt(z++);
	                    if (!Character.isLetter((char) ch))
	                    {
	                       for (int c = 0; c < j; c++) stemmingService.add(w[c]);

	                       stemmingService.stem();
	                       {  String u;
	                          u = stemmingService.toString();
	                          terms.add(u);
	                       }
	                       break;
	                    }
	                 }
	              }
	              if (ch < 0) break;
	           }
	         return terms;
		}
	    return terms;
	}
}
