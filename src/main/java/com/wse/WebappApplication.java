package com.wse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.controller.DBController;
import com.controller.StemmingController;
import com.entity.AdData;
import com.google.common.util.concurrent.RateLimiter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.PdfWriter;
import com.util.EnglishSynonymsUtil;
import com.util.GermanSynonymsUtil;
import com.util.IndexerUtil;
import com.util.LanguageUtil;
import com.util.MetaSearchUtil;
import com.util.PDFCreator;
import com.util.PageRankUtil;
import com.util.RequestFilter;
import com.util.SpellCheckUtil;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
 
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@SpringBootApplication
@RestController
@Controller
@EnableScheduling 
@Component
public class WebappApplication {
	
	int maxSitesToCrawl = 4;
	int maxDepthToCrawl = 1;
	boolean leaveDomain = true;
	
	static int maxSitesCrawled = 0;
	static int maxDepth = 0;
	boolean crawling = false;
	
	int max =0;
	
	IndexerUtil indexerUtil = new IndexerUtil();
	
	LanguageUtil languageUtil = new LanguageUtil();
	
	ArrayList<ArrayList<String>> arrayOfQueues = new ArrayList<ArrayList<String>>();

	DBController dbController = new DBController();
	
	ArrayList<String> stopwords = new ArrayList<String>();

	ArrayList<String> germanWords = new ArrayList<String>();
	
	ArrayList<String> englishWords = new ArrayList<String>();
	
	ArrayList<String> urlList = new ArrayList<String>();
	
	ArrayList<Thread> threads = new ArrayList<Thread>();
	
	RequestFilter requestFilter = new RequestFilter();
	
	PageRankUtil pageRankUtil = new PageRankUtil();
	
	SpellCheckUtil spellCheckUtil = new SpellCheckUtil();

	EnglishSynonymsUtil englishSynonymsUtil = new EnglishSynonymsUtil();
	
	GermanSynonymsUtil germanSynonymsUtil = new GermanSynonymsUtil();
	
	MetaSearchUtil metaSearch = new MetaSearchUtil();
	
	PDFCreator pdfCreator = new PDFCreator();
	
	public static void main(String[] args) {
		SpringApplication.run(WebappApplication.class, args);
	}
	
	@RequestMapping("/metaSearch")
	public String metaSearch(String query, String host,int topk) {
		ArrayList<String> words = new ArrayList<String>();
		Map<String,Double> finaldocScore = new HashMap<String,Double>();
		Pattern p = Pattern.compile("[a-zA-Z]+"); 
		Matcher m1 = p.matcher(query);
		while (m1.find()) { 
			words.add(m1.group().toLowerCase());
		} 
		String [] hosts = host.split(",");
		Map<String,Boolean> aa = new HashMap<String,Boolean>();
		ArrayList<Boolean> a = new ArrayList<Boolean>();
		for(String word:words) {
			boolean hostTermExists = dbController.hostTermExists(hosts,word);
			aa.put(word, hostTermExists);
		}
		Map<String,Boolean> aaTrue = new HashMap<String,Boolean>();
		for(String word:aa.keySet()) {
			if(aa.get(word)== true) {
				aaTrue.put(word, aa.get(word));
			}
		}

		Map<String,Double> hostScore = new HashMap<String,Double>();
		if(aaTrue.size()>0) {
			Map<String,Double> hostScoreNew = new HashMap<String,Double>();
			hostScore = dbController.checkScore(hosts, aaTrue.keySet());
			hostScore = hostScore.entrySet()
		        .stream()
		        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
		        .limit(topk)
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
		        		-> e1, LinkedHashMap::new));
			for(String b:hostScore.keySet()) {
				System.out.println(b+":: "+hostScore.get(b));
			}

			finaldocScore = showMeta(hostScore,query);
			
		}else {
			//ping all
			ArrayList<JSONObject> jsonArray = new ArrayList<JSONObject>();
			Map<String,Double> docScore = new HashMap<String,Double>();
			double avg_cw = 0;
			for(int i=0;i<hosts.length;i++) {
				try {
					if(query.contains(" ")) {
						query = query.replace(" ", "%20");
					}
					URL obj ;
					if(hosts[i].contains("isproj-vm07.informatik.uni-kl.de")) {
						obj = new URL("http://"+hosts[i]+":8080/json?query="+query+"&score=1");
					}else
						obj  = new URL("http://"+hosts[i]+":8080/is-project/json?query="+query+"&score=1");
					System.out.println(obj);
					//URL obj = new URL(GET_URL);
					HttpURLConnection con = (HttpURLConnection) obj.openConnection();
					con.setRequestMethod("GET");
					int responseCode = con.getResponseCode();
					System.out.println("GET Response Code :: " + responseCode);
					if (responseCode == HttpURLConnection.HTTP_OK) {
						BufferedReader in = new BufferedReader(new InputStreamReader(
								con.getInputStream()));
						String inputLine;
						StringBuffer response = new StringBuffer();

						while ((inputLine = in.readLine()) != null) {
							response.append(inputLine);
						}
						in.close();
						System.out.println(response.toString());
						JSONObject json = new JSONObject(response.toString());
						json.put("host", hosts[i]);
						jsonArray.add(json);
						int cw = json.getInt("cw");
						avg_cw = avg_cw + cw;
						JSONArray stat = json.getJSONArray("stat");
						JSONArray resultList = json.getJSONArray("resultList");
						for(Object j : stat)
							dbController.insertHostAndTerm(hosts[i],((JSONObject) j).getString("term"),((JSONObject) j).getInt("df"),cw,resultList);
						for(Object j : resultList) {
							String doc = ((JSONObject) j).getString("url");
							double score = ((JSONObject) j).getDouble("score");
							docScore.put(hosts[i]+">"+doc, score);
						}
					} else {
						System.out.println("GET request not worked");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			avg_cw = avg_cw / hosts.length;
			
			metaSearch.calculateT(jsonArray, avg_cw);
			metaSearch.calculateI(hosts.length,jsonArray);
			Set<String> set = new HashSet<String>(words);
			hostScore = dbController.checkScore(hosts, set);
			for(String hostDoc: docScore.keySet()) {
				String [] h = hostDoc.split(">");
				String host1 = h[0];
				String doc = h[1];
				double colScore = hostScore.get(host1);
				double i = dbController.getIFromMeta(host1);
				double rmax = 0.4+(0.6*i);
				double ri = (colScore-0.4)/(rmax-0.4);
				double docScor = docScore.get(hostDoc);
				double dDash = (docScor + (0.4*docScor*ri) )/1.4;
				docScore.replace(hostDoc, dDash);
				System.out.println(colScore+" "+i+" jjj "+rmax+" "+ri+" "+docScor+" "+dDash);
			}
			docScore = docScore.entrySet()
			        .stream()
			        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
			        .limit(10)
			        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
			        		-> e1, LinkedHashMap::new));
			
			for(String d:docScore.keySet()) {
				System.out.println(d+" hhs "+docScore.get(d));
			}
			finaldocScore = docScore;
		}
		JSONArray ar = new JSONArray();
		for(String h:finaldocScore.keySet()) {
			JSONObject jjj = new JSONObject();
			String[] split = h.split(">");
			jjj.put("host", split[0]);
			jjj.put("url", split[1]);
			ar.put(jjj);
		}
		JSONObject jj = new JSONObject ();
		jj.put("resultList", ar);
		return jj.toString();
	}
	
	public Map<String,Double> showMeta(Map<String,Double> hostScore,String query) {
		ArrayList<JSONObject> jsonArray = new ArrayList<JSONObject>();
		float avg_cw = 0;
		Map<String,Double> docScore = new HashMap<String,Double>();
		for(String host:hostScore.keySet()) {
			try {
				if(query.contains(" ")) {
					query = query.replace(" ", "%20");
				}
				URL obj ;
				if(host.contains("isproj-vm07.informatik.uni-kl.de")) {
					obj = new URL("http://"+host+":8080/json?query="+query+"&score=1");
				}else
					obj  = new URL("http://"+host+":8080/is-project/json?query="+query+"&score=1");
				System.out.println(obj);
				//URL obj = new URL(GET_URL);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				int responseCode = con.getResponseCode();
				System.out.println("GET Response Code :: " + responseCode);
				if (responseCode == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(new InputStreamReader(
							con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					System.out.println(response.toString());
					JSONObject json = new JSONObject(response.toString());
					json.put("host", host);
					jsonArray.add(json);
					JSONArray resultList = json.getJSONArray("resultList");
					for(Object j : resultList) {
						String doc = ((JSONObject) j).getString("url");
						double score = ((JSONObject) j).getDouble("score");
						docScore.put(host+">"+doc, score);
					}
						
				} else {
					System.out.println("GET request not worked");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for(String hostDoc: docScore.keySet()) {
			String [] h = hostDoc.split(">");
			String host = h[0];
			String doc = h[1];
			double colScore = hostScore.get(host);
			double i = dbController.getIFromMeta(host);
			double rmax = 0.4+(0.6*i);
			double ri = (colScore-0.4)/(rmax-0.4);
			double docScor = docScore.get(hostDoc);
			double dDash = (docScor + (0.4*docScor*ri) )/1.4;
			docScore.replace(hostDoc, dDash);
		}
		docScore = docScore.entrySet()
		        .stream()
		        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
		        .limit(10)
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
		        		-> e1, LinkedHashMap::new));
		
		for(String d:docScore.keySet()) {
			System.out.println(d+" hh "+docScore.get(d));
		}

		return docScore;
	}
	
	@RequestMapping("/reduceCost")
	public void reduceCost (String url) {
		dbController.reduceCost(url);
	}
	
	@RequestMapping("/getAds")
	public String getAds (String query) {
		ArrayList<AdData> adDataArray = dbController.getAllAds();
		Map<AdData,Integer> res = new HashMap<AdData,Integer>();
		for(AdData ad:adDataArray) {
			String[] ngrams = ad.getnGrams().split(",");
			for(int i=0;i<ngrams.length;i++) {
				if(query.contains(ngrams[i])) {
					ad.setScore(ad.getScore()+1);
					if(res.containsKey(ad))
						res.replace(ad,ad.getScore());
					else
						res.put(ad,ad.getScore());
				}
				
			}
		}
		res.entrySet()
        .stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .limit(4)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
        		-> e1, LinkedHashMap::new));
		
		for(AdData a:res.keySet()) {
			System.out.println("score : "+res.get(a));
		}
		
		JSONArray array = new JSONArray();
		for(AdData ad:res.keySet()) {
			JSONObject json = new JSONObject();
			if(!ad.getImageUrl().contains("http"))
				json.put("image_url", "http://"+ad.getImageUrl());
			else
				json.put("image_url", ad.getImageUrl());
			if(!ad.getUrl().contains("http"))
				json.put("url", "http://"+ad.getUrl());
			else
				json.put("url", ad.getUrl());
			json.put("text", ad.getText());
			array.put(json);
		}
		return array+"";
	}
	
	@RequestMapping("/postAd")
	public void postAd (String nGrams,String url,String imageUrl, String text, float budget) {
		dbController.insertAd(nGrams, url, imageUrl, text, budget);
	}

	@RequestMapping("/getImages")
	public String getImages (String query, @RequestParam(required=false ,defaultValue = "en") String language) {
		String link = "";
		if(query.contains("site:")) {
			
			Pattern p1 = Pattern.compile(
			        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
			                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
			                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
			        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL); 
			Matcher m=p1.matcher(query);
			while (m.find()) {
			    int matchStart = m.start(1);
			    int matchEnd = m.end();
			    link = query.substring(matchStart, matchEnd);
			}
			query = query.replace(link, " ");
			query = query.replace("site:", " ");
		}
		
		ArrayList<String> words = new ArrayList<String>();
		Pattern p = Pattern.compile("[a-zA-Z]+"); 
		Matcher m1 = p.matcher(query);
		while (m1.find()) { 
			words.add(m1.group().toLowerCase());
		} 
		
		JSONObject r = new JSONObject(search(query, "D", 10, 3, language,null));
		JSONArray resultArray = new JSONArray(r.get("resultList")+"");
		Map<Integer,Float>  res = new HashMap<Integer,Float>();
		Map<String,Float>  newres = new HashMap<String,Float>();
		for(int i =0;i<resultArray.length();i++) {
			JSONObject json = new JSONObject(resultArray.get(i)+"");
			int docid = (int) json.get("docid");
			String tags = dbController.getPageContentWithTags(docid);
			res =indexerUtil.getNeighbouringText(tags, words);
			for(Integer d:res.keySet())
				newres.put(docid+"-"+d, res.get(d));
		}
		newres = newres.entrySet()
		        .stream()
		        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
		        		-> e1, LinkedHashMap::new));
		JSONArray imgRes = new JSONArray();int h=0;
		for(String key:newres.keySet()) {
			String[] split = key.split("-");
			int docid = Integer.parseInt(split[0]);
			int position = Integer.parseInt(split[1]);
			JSONObject json = new JSONObject();
			String im = dbController.getImageByDocidPosition(docid,position);
			if(!im.contains("http"))
				im = "http://"+im;

			System.out.println(key+" -- "+newres.get(key)+"  ---- "+im);
			json.put("url",im);
			if(h<10)
			 imgRes.put(json);
			h++;
		}
		
		return imgRes.toString();
	}
	
	@RequestMapping("/shingles")
	public void testMinHashing () {
		dbController.createShinglesTable();
		Map<Integer,String> docidContent = dbController.getPageContentForAllDocs();
		createShingles(docidContent,4);
		dbController.createJaccardTable();
		dbController.calculateJaccardScore();
		dbController.createJaccardUDF();
		ArrayList<Integer> docs = dbController.getJaccardScore(0,0.000009);
		for(Integer s:docs)
			System.out.println(s);
		dbController.createMinNShinglesUDF();
		dbController.createCompareShinglesUDF();
		ArrayList<Integer> limits = new ArrayList<Integer>();
		limits.add(1);
		limits.add(4);
		limits.add(16);
		limits.add(32);
		ArrayList<Integer> distinctDocs = dbController.getDistinctDocsForShingles();
		ArrayList<Float> errors = new ArrayList<Float>();
		ArrayList<Float> medians = new ArrayList<Float>();
		ArrayList<Double> averages = new ArrayList<Double>();
		ArrayList<Float> q1s = new ArrayList<Float>();
		ArrayList<Float> q3s = new ArrayList<Float>();
		for(Integer limit:limits) {
			for(int i=0;i<distinctDocs.size();i++) {
				for(int j=0;j<distinctDocs.size();j++) {
					if(distinctDocs.get(i)!=distinctDocs.get(j) &&  distinctDocs.get(i)<distinctDocs.get(j)){
						float minHashValue = dbController.compareNShingles(distinctDocs.get(i), distinctDocs.get(j), limit);
						//if(minHashValue>0) {
							float jaccardScore = 0;
							jaccardScore = (float) dbController.getJaccardScoreForPair(distinctDocs.get(i), distinctDocs.get(j));
							float error = Math.abs(jaccardScore - minHashValue);
							System.out.println("Error : "+error+" :: "+distinctDocs.get(i)+" ::: "+distinctDocs.get(j));
							errors.add(error);
						//}
					}
				}
			}
			if(errors.size()>0) {
				Collections.sort(errors);
				float median = (errors.get(errors.size()/2) + errors.get(errors.size()/2 - 1))/2;
				Double average = errors.stream().mapToDouble(val -> val).average().orElse(0.0);
				int quartile = (int)((errors.size())*(25/100));
				float q1 = errors.get(quartile);
				quartile = (int)((errors.size())*(75/100));
				float q3 = errors.get(quartile);
				System.out.println(median +" : "+average+" "+q1 +" "+q3);
				medians.add(median); averages.add(average); q1s.add(q1); q3s.add(q3);
				errors.clear();
			}
			//createPDF("result");
		}
		pdfCreator.createPDF(limits,medians,averages,q1s,q3s);
	}
	
	
	void createShingles(Map<Integer,String> docidContent, int k) {
		DBController dbController = new DBController();
		for(Integer docid: docidContent.keySet()) {
			String content = docidContent.get(docid);
			String[] result ;
			try {
			 result = content.split("\\s+");
			}catch(NullPointerException e) {
				continue;
			}
			for(int i=0;i<result.length-k;i++) {
				String shingle ="";
				for(int j=i;j<k+i ;j++) {
					shingle = shingle + " " + result[j];
				}
				dbController.insertShingle(docid,shingle);
			}	
		}
	}
	
	@ResponseBody
	@RequestMapping("/search")
	public String search ( String query, 
			@RequestParam(required=false ,defaultValue = "D") String disCon,
			@RequestParam(required=false ,defaultValue = "20") int k, //top-k
			@RequestParam(required=false ,defaultValue = "3") int score, //tfidf-1 ; okapi-2; pageRankOkapi-3
			@RequestParam(required=false ,defaultValue = "en") String language,
			HttpServletRequest request) {
	
		if(request!=null)
		if(!requestFilter.doFilter(request))
			return "Too many requests";
		//limit the number of access to 10 per second
		limiter().acquire();

		boolean containsLink = false;
		String containingLink = "";
		ArrayList<String> qoutedQueryTerms = new ArrayList<String>();
		
		if(query=="" || query==null)
			return "";
		System.out.println("As soon as it enters "+query);
		//for site:
		if(query.contains("site:")) {
			
			Pattern p1 = Pattern.compile(
			        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
			                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
			                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
			        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL); 
			Matcher m=p1.matcher(query);
			String link = "";
			while (m.find()) {
			    int matchStart = m.start(1);
			    int matchEnd = m.end();
			    link = query.substring(matchStart, matchEnd);
			}
			containsLink = true;
			containingLink = link;
			query = query.replace(link, " ");
			query = query.replace("site:", " ");
		}
		String forProcessingContent = query;
		
		//for quotations
		String qString = "";
		if(query.contains("\"")) {
			Pattern p = Pattern.compile("\"([^\"]*)\"");
			Matcher m = p.matcher(query);
			while (m.find()) {
				qoutedQueryTerms.add(m.group(1));
				qString= qString +" "+ m.group(1);
			}
		}
		
		//for ~
		ArrayList<String> allTildas = new ArrayList<String>();
		if(query.contains("~")) {
			Pattern p = Pattern.compile("~([a-zA-Z0-9]*)");
			Matcher m = p.matcher(query);
			while (m.find()) {
				allTildas.add(m.group().replace("~", ""));
			}
		}
		Set<String> synonyms = new HashSet<>();
		if(language.equals("en")) {
			for(String tilda:allTildas) {
				synonyms.addAll(englishSynonymsUtil.findEnglishSynonyms(tilda));
			}
		}else {
			for(String tilda:allTildas) {
				synonyms.addAll(germanSynonymsUtil.findGermanSynonyms(tilda));
			}
		}
		System.out.println("Synonyms :: "+synonyms);
		
		ArrayList<String> stemmedTerms = new ArrayList<String>();
		Pattern p = Pattern.compile("[a-zA-Z]+"); 
		Matcher m1 = p.matcher(query); 
		String forStemming ="";
		while (m1.find()) { 
			forStemming = forStemming+m1.group()+" ";
			stemmedTerms.add(m1.group());
		} 
		
		String lang = languageUtil.checkForLanguage(germanWords, englishWords, forStemming);
		
		ArrayList<String> qStem = new ArrayList<String>();
		int noOfQueryTerms =0;
		if(language.equals("en")) {
			//stemming query words and removing stopwords
	        StemmingController stemmer = new StemmingController();
		    stemmedTerms = stemmer.stem(forStemming);
		    stemmedTerms.removeAll(stopwords);
		    noOfQueryTerms = stemmedTerms.size();
		    qStem = stemmer.stem(qString);
		    stemmedTerms.remove(qStem);
		    System.out.println("stemmed "+stemmedTerms);
		    ArrayList<String> syn = stemmer.stem(synonyms+"");
		    synonyms.clear();
		    synonyms.addAll(syn);
		    System.out.println("After stemming syn: "+synonyms);
		    
		}else {
			stemmedTerms.remove(qoutedQueryTerms);
			qStem = qoutedQueryTerms;
			noOfQueryTerms = stemmedTerms.size()+qStem.size();
		}
		
		ArrayList<String> allQueryTerms = new ArrayList<String>();
		//allQueryTerms.addAll(qStem);
		allQueryTerms.addAll(stemmedTerms);
		ArrayList<String> correctedTerms = new ArrayList<String>();
		if(language.equals("en"))
			correctedTerms = spellCheckUtil.spellCheck(allQueryTerms.toString());
		else
			correctedTerms = spellCheckUtil.spellCheckGerman(allQueryTerms.toString());
		System.out.println("corrected terms "+correctedTerms);
		
		stemmedTerms.addAll(synonyms);
	    
		String last = "";
		
		Map<Integer,Float> docidScores = new HashMap<Integer,Float>();
		if(disCon.equals("C")) {
			query="(";
		    for(String s: stemmedTerms) {
		    	query=query+"'"+s+"',";
		    }
		    for(String s: qStem) {
		    	query=query+"'"+s+"',";
		    }
			last = query.substring(0,query.length()-1);
			last=last+")";
			if(!containsLink)
				docidScores = dbController.searchConScore(last, k,noOfQueryTerms,score,language);
			else {
				try {
					containingLink = new URL(containingLink).getHost();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				docidScores = dbController.searchConScoreLink(last, k,noOfQueryTerms,containingLink,score,language);
			}
			docidScores = docidScores.entrySet()
		        .stream()
		        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
		        		-> e1, LinkedHashMap::new));
		}else {
			query="(";
		    for(String s: stemmedTerms) {
		    	query=query+"'"+s+"',";
		    }
			last = query.substring(0,query.length()-1);
			last=last+")";
			System.out.println("The query is: "+last +" quoted  :"+qStem);
			docidScores = dbController.searchAllRounder(last,k,
					containingLink,qStem,noOfQueryTerms,score,language);
			docidScores = docidScores.entrySet()
	        .stream()
	        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
	        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, 

	        		LinkedHashMap::new));
		}
		
		//processing the content
		Map<Integer,String> processedContent = processContent(docidScores,forProcessingContent,synonyms);
		
		return createJSONArrayNew(docidScores,k,stemmedTerms,correctedTerms,containingLink,processedContent)+"";
	}
	
	Map<Integer,String> processContent(Map<Integer,Float> docidScores , String query, Set<String> synonyms){
		ArrayList<String> words = new ArrayList<String>();
		Pattern p = Pattern.compile("[a-zA-Z]+"); 
		Matcher m1 = p.matcher(query);
		while (m1.find()) { 
			words.add(m1.group());
		} 
		int count=0;
		Map<Integer,String> docidContent = new HashMap<Integer,String>();
		String finalContentResult = "";
		String missing = "";
		for(Integer docid : docidScores.keySet()) {
			//count=0;
			missing="";
			finalContentResult = "";
			String c = dbController.getContentFromDocid(docid);
			System.out.println("the content is "+c);
			for(String word: words) {
				count=0;
				Pattern pattern = Pattern.compile("([^\\s]+\\s+[^\\s]+)\\s+"+word.toLowerCase()+"[a-z0-9]*\\s+([^\\s]+\\s+[^\\s]+)");
				Matcher matcher = pattern.matcher(c.toLowerCase());
				if(matcher.find()) {
					System.out.println("Contains "+word+docid);
			        Matcher matcher1 = pattern.matcher(c.toLowerCase());
			        String re = "";
			        while (matcher1.find() && count<4){
			        	System.out.println("match found"+re);
			        	re = re +" "+matcher1.group(1)+" "+word+ " "+matcher1.group(2)+" ... ";
			        	count++;
			        }
					finalContentResult = finalContentResult + " " +re;
				}else {
					missing = missing + " " + word;
				}
			}
			for(String word: synonyms) {
				Pattern pattern = Pattern.compile("([^\\s]+\\s+[^\\s]+)\\s+"+word.toLowerCase()+"[a-z0-9]*\\s+([^\\s]+\\s+[^\\s]+)");
		        Matcher matcher1 = pattern.matcher(c.toLowerCase());
				if(matcher1.find()) {
					System.out.println("Contains synonyms "+word+docid);
			        Matcher matcher = pattern.matcher(c.toLowerCase());
			        String re = "";
			        while (matcher.find() && count<4)
			        {
			        	re = re +" "+matcher.group(1)+" "+word+ " "+matcher.group(2)+" ... ";
			        	count++;
			        }
			        finalContentResult = finalContentResult + " " +re;
				}else {
					missing = missing + " " + word;
				}
			}
			if(missing.equals(""))
				docidContent.put(docid, finalContentResult);
			else
				docidContent.put(docid, finalContentResult+" Missing:"+missing);
		}
		System.out.println("final content "+docidContent);
		return docidContent;
		
	}
	
	@ResponseBody
	@RequestMapping("/json")
	public String json ( String query, 
			@RequestParam(required=false ,defaultValue = "D") String disCon,
			@RequestParam(required=false ,defaultValue = "20") int k,
			@RequestParam(required=false ,defaultValue = "3") int score,
			@RequestParam(required=false ,defaultValue = "en") String language,
			HttpServletRequest request) {
	
		if(request!=null)
		if(!requestFilter.doFilter(request))
			return "Too many requests";
		//limit the number of access to 10 per second
		limiter().acquire();

		boolean containsLink = false;
		String containingLink = "";
		ArrayList<String> qoutedQueryTerms = new ArrayList<String>();
		
		if(query=="" || query==null)
			return "";
		
		//for site:
		if(query.contains("site:")) {
			
			Pattern p1 = Pattern.compile(
			        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
			                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
			                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
			        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL); 
			Matcher m=p1.matcher(query);
			String link = "";
			while (m.find()) {
			    int matchStart = m.start(1);
			    int matchEnd = m.end();
			    link = query.substring(matchStart, matchEnd);
			}
			containsLink = true;
			containingLink = link;
			query = query.replace(link, " ");
			query = query.replace("site:", " ");
		}
		
		//for quotations
		String qString = "";
		if(query.contains("\"")) {
			Pattern p = Pattern.compile("\"([^\"]*)\"");
			Matcher m = p.matcher(query);
			while (m.find()) {
				qoutedQueryTerms.add(m.group(1));
				qString= qString +" "+ m.group(1);
			}
		}
		
		ArrayList<String> stemmedTerms = new ArrayList<String>();
		Pattern p = Pattern.compile("[a-zA-Z]+"); 
		Matcher m1 = p.matcher(query); 
		String forStemming ="";
		while (m1.find()) { 
			forStemming = forStemming+m1.group()+" ";
			stemmedTerms.add(m1.group());
		} 
		
		String lang = languageUtil.checkForLanguage(germanWords, englishWords, forStemming);
		
		ArrayList<String> qStem = new ArrayList<String>();
		int noOfQueryTerms =0;
		if(lang.equals("en")) {
			//stemming query words and removing stopwords
	        StemmingController stemmer = new StemmingController();
		    stemmedTerms = stemmer.stem(forStemming);
		    stemmedTerms.removeAll(stopwords);
		    noOfQueryTerms = stemmedTerms.size();
		    qStem = stemmer.stem(qString);
		    stemmedTerms.remove(qStem);
		}else {
			stemmedTerms.remove(qoutedQueryTerms);
			qStem = qoutedQueryTerms;
			noOfQueryTerms = stemmedTerms.size()+qStem.size();
		}
		
		ArrayList<String> allQueryTerms = new ArrayList<String>();
		//allQueryTerms.addAll(qStem);
		allQueryTerms.addAll(stemmedTerms);
		ArrayList<String> correctedTerms = new ArrayList<String>();
		if(language.equals("en"))
			correctedTerms = spellCheckUtil.spellCheck(allQueryTerms.toString());
		else
			correctedTerms = spellCheckUtil.spellCheckGerman(allQueryTerms.toString());
		System.out.println("hehe "+correctedTerms);
	    
		String last = "";
		
		Map<Integer,Float> docidScores = new HashMap<Integer,Float>();
		if(disCon.equals("C")) {
			query="(";
		    for(String s: stemmedTerms) {
		    	query=query+"'"+s+"',";
		    }
		    for(String s: qStem) {
		    	query=query+"'"+s+"',";
		    }
			last = query.substring(0,query.length()-1);
			last=last+")";
			if(!containsLink)
				docidScores = dbController.searchConScore(last, k,noOfQueryTerms,score,language);
			else {
				try {
					containingLink = new URL(containingLink).getHost();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				docidScores = dbController.searchConScoreLink(last, k,noOfQueryTerms,containingLink,score,language);
			}
			docidScores = docidScores.entrySet()
		        .stream()
		        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) 
		        		-> e1, LinkedHashMap::new));
		}else {
			query="(";
		    for(String s: stemmedTerms) {
		    	query=query+"'"+s+"',";
		    }
			last = query.substring(0,query.length()-1);
			last=last+")";
			System.out.println("The query is: "+last +" quoted  :"+qStem);
			docidScores = dbController.searchAllRounder(last,k,
					containingLink,qStem,noOfQueryTerms,score,language);
			docidScores = docidScores.entrySet()
	        .stream()
	        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
	        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, 

	        		LinkedHashMap::new));
		}
		return createJSONArray(docidScores,k,stemmedTerms,correctedTerms,containingLink)+"";
	}
	
	public JSONObject createJSONArrayNew(Map<Integer,Float> docidScores,int topCount,
			ArrayList<String> query,ArrayList<String> correctedTerms,String containingLink, 
			Map<Integer,String> processedContent) {
		JSONObject last = new JSONObject();
		JSONArray array = new JSONArray();
		int i=1;
		for(Integer docid:docidScores.keySet()) {
			JSONObject result = new JSONObject();
			result.put("docid", docid);
			result.put("url", dbController.getUrlForDocid(docid));
			result.put("score", docidScores.get(docid));
			result.put("rank", i);
			result.put("content",processedContent.get(docid));
			i++;
			array.put(result);
		}
		last.put("resultList", array);
		JSONObject q = new JSONObject();
		q.put("k", topCount);
		q.put("query", query);
		last.put("query",q);
		JSONArray stats = new JSONArray();
		for(String term: query) {
			int df = dbController.getDfForTerm(term);
			JSONObject termDf = new JSONObject();
			termDf.put("term", term);
			termDf.put("df",df);
			stats.put(termDf);
		}
		last.put("stat", stats);
		System.out.println(query +" ::::: "+correctedTerms);
		System.out.println(query.equals(correctedTerms));
		if(!query.equals(correctedTerms)) {
			String dummy ="";
			if(!containingLink.equals(""))
				dummy = String.join(" ", correctedTerms)+" site:"+containingLink;
			else
				dummy = String.join(" ", correctedTerms);
			
			last.put("correctedTerms", dummy);
		}
		last.put("cw", dbController.getAllDocuments().size());
		return last;
	}
	
	public JSONObject createJSONArray(Map<Integer,Float> docidScores,int topCount,
			ArrayList<String> query,ArrayList<String> correctedTerms,String containingLink) {
		JSONObject last = new JSONObject();
		JSONArray array = new JSONArray();
		int i=1;
		for(Integer docid:docidScores.keySet()) {
			JSONObject result = new JSONObject();
			//result.put("docid", docid);
			result.put("url", dbController.getUrlForDocid(docid));
			result.put("score", docidScores.get(docid));
			result.put("rank", i);
			i++;
			array.put(result);
		}
		last.put("resultList", array);
		JSONObject q = new JSONObject();
		q.put("k", topCount);
		q.put("query", query);
		last.put("query",q);
		JSONArray stats = new JSONArray();
		for(String term: query) {
			int df = dbController.getDfForTerm(term);
			JSONObject termDf = new JSONObject();
			termDf.put("term", term);
			termDf.put("df",df);
			stats.put(termDf);
		}
		last.put("stat", stats);
		System.out.println(query +" ::::: "+correctedTerms);
		System.out.println(query.equals(correctedTerms));
		if(!query.equals(correctedTerms)) {
			String dummy ="";
			if(!containingLink.equals(""))
				dummy = String.join(" ", correctedTerms)+" site:"+containingLink;
			else
				dummy = String.join(" ", correctedTerms);
			
			last.put("correctedTerms", dummy);
		}
		last.put("cw", dbController.getAllDocuments().size());
		return last;
	}
	
	@PostConstruct
	public void createSchema() {
		dbController.createSchema();
		//crawl();
	}
	
	@RequestMapping("/crawl")
	public void crawl () {
		crawling = true;
		maxSitesCrawled = 0;
		maxDepth = 0;
		arrayOfQueues.clear();
		
		urlList.add("https://www.patienten-information.de/kurzinformationen/herz-und-gefaesse/koronare-herzkrankheit");
		urlList.add("https://www.elcaminohealth.org/");
		
		ArrayList<String> queue = new ArrayList<>();
		for(int i=0;i<urlList.size();i++) {
			queue.add(urlList.get(i));
		}
		
		arrayOfQueues.add(queue);
		maxDepth= 0+maxDepthToCrawl;
		
		for(String url:queue) {
			if(crawling && maxSitesCrawled < maxSitesToCrawl && 0 <= maxDepth) {
				maxSitesCrawled++;
				CrawlerThread ct = new CrawlerThread(url,0);
				ct.start();
			}
		}
		checkIfAllThreadsEnded();
	}
	
	public void crawlAgain (int depth) {
		//dbController.deleteAllFromIdf();
		
			ArrayList q = arrayOfQueues.get(depth);
			if(q.size()>0) {
				for(int i=0;i<q.size();i++) {
					if(crawling && maxSitesCrawled < maxSitesToCrawl && depth <= maxDepth) {
						maxSitesCrawled++;
						CrawlerThread ct = new CrawlerThread(q.get(i).toString(),depth);
						ct.start();
					}
					else {
						ArrayList<String> allDocuments = dbController.getAllDocuments();
						for(int ii=0;ii<q.size();ii++) {
							if(!allDocuments.contains(q.get(ii).toString()))
								dbController.insertOutDocument(q.get(ii).toString(),depth);
						}
						break;
					}
				}
			}
	}
	
	@RequestMapping("/startCrawl")
	//@Scheduled(cron="0 0/10 * 1/1 * ?")
	public void startCrawling() {
		System.out.println("Started crawling at: "+new Date());
		crawling = true;
		int minDepth = dbController.getMinDepth();
		int maxDepth = dbController.getMaxDepth();
		ArrayList<String> notCrawled = dbController.getAllNotCrawledDocsFromDepth(minDepth);
		//System.out.println(notCrawled);
		arrayOfQueues.clear();
		
		for(int i=0;i<=maxDepth;i++) {
			arrayOfQueues.add(new ArrayList<String>());
		}
		
		for(int i=0;i<=maxDepth;i++) {
			for(int j=0;j<notCrawled.size();j++) {
				if(i==dbController.getDepthOfDOc(notCrawled.get(j))) {
					arrayOfQueues.get(i).add(notCrawled.get(j));
				}
			}
		}
		
		maxSitesCrawled=0;
		maxDepth = minDepth + maxDepthToCrawl;
		//System.out.println("MAXDEPTHTORAWK UPDATED : "+maxDepth);
		
		for(int i=0;i<arrayOfQueues.get(minDepth).size();i++) {
			if(crawling && maxSitesCrawled < maxSitesToCrawl && minDepth <= maxDepth) {
				maxSitesCrawled++;
				CrawlerThread ct = new CrawlerThread(arrayOfQueues.get(minDepth).get(i),minDepth);
				ct.start();
			}
		}
		
		checkIfAllThreadsEnded();
	}
	
	@RequestMapping("/stopCrawling")
	public void stopCrawling() {
		crawling = false;
		checkIfAllThreadsEnded();
	}
	
	@Bean
	public RateLimiter limiter() {
		RateLimiter rateLimiter = RateLimiter.create(10);
		return rateLimiter;
	}
	
	@Bean
	ArrayList<String> getStopWordsList() {
		File file = new File("stop.txt"); 
		try {
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  String st; 
		  while ((st = br.readLine()) != null) {
			stopwords.add((st.split(" "))[0]);
		  }
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stopwords; 
		  
	}
	
	@Bean
	ArrayList<String> getGermanWordsList() {
		File file = new File("german_words.txt"); 
		try {
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  String st; 
		  while ((st = br.readLine()) != null) {
			germanWords.add((st.split(" "))[0].toLowerCase());
		  }
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(germanWords);
		return germanWords; 
		  
	}
	
	@Bean
	ArrayList<String> getEnglishWordsList() {
		File file = new File("english_words.txt"); 
		try {
		  BufferedReader br = new BufferedReader(new FileReader(file));
		  String st; 
		  while ((st = br.readLine()) != null) {
			englishWords.add((st.split(" "))[0].toLowerCase());
		  }
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println(englishWords);
		return englishWords; 
		  
	}
	
	@RequestMapping("/indexer")
	public void indexer (String link,int depth) {
		URL url;
		  String pageContents="";
		  try {
			  url = new URL(link);
		        pageContents = indexerUtil.downloadPage(url);
		        String toStoreWithTags = pageContents;
		        //extract outgoing links from the page
		        HashSet h = new HashSet();
		        h.add(url);
		        ArrayList<String> outgoingLinks = new ArrayList<String>();
		        if(pageContents!=null)
		        	outgoingLinks = indexerUtil.retrieveLinks(url,pageContents,h);
	            //System.out.println(link+" : "+outgoingLinks);
		        
		        Map<String,String> images = new HashMap<String,String>();
		        String title = "";
				String lang = "";
	            if(pageContents!=null) {
	            	title = indexerUtil.getTitleOfPage(pageContents);
	            	System.out.println("TITLE : "+title);
					images = indexerUtil.getImageTags(pageContents,url);
					//System.out.println(images);
	            	pageContents = indexerUtil.removeHTMLTags(pageContents);
	            	//System.out.println(pageContents);
	            	lang = languageUtil.checkForLanguage(germanWords,englishWords,pageContents);
	            }
				
	    	    ArrayList<String> stemmedTerms = new ArrayList<String>();
	            if(lang.equals("en")) {
	            	System.out.println("ENGLISH");
		            
			        //stemming page words
		            StemmingController stemmer = new StemmingController();
		    	    stemmedTerms = stemmer.stem(pageContents);
		    	    
		    	    //removing stopwords
		    	    stemmedTerms.removeAll(stopwords);
	            }else if(lang.equals("de")){
	            	System.out.println("GERMAN");
	            	Pattern p = Pattern.compile("[a-zA-Z]+"); 
	        		Matcher m1 = p.matcher(pageContents); 
	        		while (m1.find()) { 
	        			stemmedTerms.add( m1.group().toLowerCase());
	        		} 
	            }
	    	    
	    	    //Storing to database only if it is not stored before
	    	    ArrayList<String> allDocuments = dbController.getAllDocuments();
				if(!allDocuments.contains(link)) {
					//System.out.println("Inserting "+title+" :"+link);
					dbController.insertDocument(link,depth,lang,title,pageContents,toStoreWithTags);
					dbController.saveImages(images, link);
				}else {
					dbController.updateLangForDocument(link,lang,title,pageContents,toStoreWithTags);
					dbController.saveImages(images, link);
				}
				
				//Storing stemmed and non-stopwords into features table
	    	    Map<String, Long> counts = new HashMap<String,Long>();
	    	    counts = stemmedTerms.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
	    	    int urlId = dbController.getUrlId(link);
	    	    ArrayList<Integer> x = dbController.getAllFeatures();
	    	    if(!x.contains(urlId)) {	
	    	    	//System.out.println(link+" uuuuuu: "+counts);
	    	    	dbController.insertTerms(link,counts);
	    	    }
	    	    counts =null;
	    	    	
	    	    //Removing duplicate links
	   			 Set<String> set = new LinkedHashSet<>(); 
	   		     set.addAll(outgoingLinks); 
	   		     outgoingLinks.clear(); 
	   		     outgoingLinks.addAll(set);
	   		     
	   		     //Adding outgoing links to docs
	    			ArrayList<String> allDocs = dbController.getAllDocuments();
	    			for(int i=0;i<outgoingLinks.size();i++) {
	    				if(!allDocs.contains(outgoingLinks.get(i))) {
	    					URL out = new URL(outgoingLinks.get(i));
	    					if(out.getHost().equals(new URL(link).getHost()) 
	    							|| (!out.getHost().equals(new URL(link)) && leaveDomain)) {
		    					//System.out.println("Inserting"+outgoingLinks.get(i));
		    					dbController.insertOutDocument(outgoingLinks.get(i),depth+1);
	    					}
	    				}
	    			}
	   		     
	   		     /*//removing those already in documents table
	   		     allDocuments = dbController.getAllDocuments();
	   		     outgoingLinks.removeAll(allDocuments);*/
   		     
	   		     //System.out.println("OUT "+link+outgoingLinks);
	   		     if(outgoingLinks.size()>0) {
	   		    	 
		   		     //if the index queue already exists
		   		     if(arrayOfQueues.size() <= depth+1) {
		   		    //queue does not exist
			   		    	ArrayList<String> queue = new ArrayList<>();
					  		for(int i=0;i<outgoingLinks.size();i++) {
					  			URL out = new URL(outgoingLinks.get(i));
		    					if(out.getHost().equals(new URL(link).getHost()) 
		    							|| (!out.getHost().equals(new URL(link)) && leaveDomain)) {
		    						queue.add(outgoingLinks.get(i));
		    					}
					  		} 
					  		arrayOfQueues.add(queue);
		   		     }
		   		     else {
		   		    //queue exists
			   		    	for(int i=0;i<outgoingLinks.size();i++) {
			   		    		URL out = new URL(outgoingLinks.get(i));
		    					if(out.getHost().equals(new URL(link).getHost()) 
		    							|| (!out.getHost().equals(new URL(link)) && leaveDomain)) {
		    						(arrayOfQueues.get(depth+1)).add(outgoingLinks.get(i));
		    					}
					  		}
		   		     }
	   		     }
	            
	            //mark as crawled
	            dbController.markCrawledLink(link);
	            
	            //crawlAgain
	            if(depth < maxDepth)
	            	crawlAgain(depth+1);
	            
	            //inserting outgoing links in link table
	            for(int i=0;i<outgoingLinks.size();i++) {
	            	URL out = new URL(outgoingLinks.get(i));
					if(out.getHost().equals(new URL(link).getHost()) 
							|| (!out.getHost().equals(new URL(link)) && leaveDomain)) {
		            	//System.out.println("Linking "+link+" & "+outgoingLinks.get(i));
			  			dbController.insertOutgoingLinks(link,outgoingLinks.get(i));
					}
		    	}
	    	    
		  }catch(Exception e) {
			  e.printStackTrace();
		  }
	    
	}

	public void calculateIdf(){
		dbController.deleteAllFromIdf();
		//inserting into idf table
	    ArrayList<String> allterms = dbController.getAllTermsFromFeatures();
	    if(allterms.size()>0) {
			int totalNoOfDocs = dbController.getAllDocuments().size();
			Map<String, Long> counts =
    	    	    allterms.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
			for(String name : counts.keySet()) {
				dbController.insertToIdf(name, totalNoOfDocs, counts.get(name));
			}
    	    
		}
	    updateTfIdfInFeatures();
	    pageRankUtil.calculatePageRank();
	}
	
	public void updateTfIdfInFeatures() {
		dbController.updateTfIdfInFeatures();
	}
	
	public void checkIfAllThreadsEnded(){
		int allDead = 1;
		for(int i =0;i<threads.size();i++) {
			if(!threads.get(i).isAlive()) {
				allDead++;
			}
		}
		//System.out.println(allDead+"::::::::"+threads.size());
		if(allDead==threads.size()) {
			threads.clear();
			calculateIdf();
		}
	}

	//Creates threads for crawler
	class CrawlerThread implements Runnable {
		   private Thread t;
		   private int depth;
		   private String url ="";
		   
		   CrawlerThread( String url,int depth) {
			      this.depth = depth;
			      this.url = url;
			      System.out.println("Creating " +  this.url +" : "+this.depth);
		   }
		   
		   public void run() {
		      
		      try {
		    	  if(!this.url.equals(""))
		    		  indexer(this.url,this.depth);
		          Thread.sleep(50);
		      }catch (InterruptedException e) {
		         System.out.println("Thread " +  url + " interrupted.");
		      }
		      System.out.println("Thread "  +url+" exiting.");
		      checkIfAllThreadsEnded();
		      System.out.println(arrayOfQueues.size()+" : "+arrayOfQueues);
		   }
		   
		   public void start () {
		      //System.out.println("Starting " +  threadName );
		      if (t == null) {
		         t = new Thread (this);
		         threads.add(t);
		         t.start ();
		      }
		   }
	}

}

