package com.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexerUtil {

	public boolean crawling;

	private PrintWriter logFileWriter;
	
	public String downloadPage(URL pageUrl) { 
		try { 
			// Open connection to URL for reading. 
			BufferedReader reader = new BufferedReader(new InputStreamReader( pageUrl.openStream()));
			// Read page into buffer. 
			String line; StringBuffer pageBuffer = new StringBuffer(); 
			while ((line = reader.readLine()) != null) { 
				pageBuffer.append(line); 
			}
			return pageBuffer.toString(); 
		} catch (Exception e) { }
		return null;
	}
	
	public String removeHTMLTags(String pageContent) {
		//pageContent = pageContent.replaceAll("\\<.*?\\>", "");
		pageContent = pageContent.replaceAll("<script[^>]*>(.*?)</script>", "");
		pageContent = pageContent.replaceAll("<head[^>]*>(.*?)</head>", "");
		pageContent = pageContent.replaceAll("<style[^>]*>(.*?)</style>", "");
		pageContent = pageContent.replaceAll("\\<.*?\\>", "");
		System.out.println("After removing HTML Tags: " + pageContent);
		return pageContent;   
	}

	public ArrayList<String> retrieveLinks( URL pageUrl, String pageContents, HashSet crawledList/*, boolean limitHost*/) {
		//System.out.println("RETRIEVE "+pageUrl);
		Pattern p = Pattern.compile("<a\\s+href\\s*=\\s*\"?(.*?)[\"|>]", Pattern.CASE_INSENSITIVE); 
		Matcher m=p.matcher(pageContents);

		ArrayList<String> linkList = new ArrayList<String>();

		while (m.find()) { 
			String link = m.group(1).trim();
			// Skip empty links. 
			if (link.length() < 1) { 
				continue; 
			}
			// Skip links that are just page anchors. 
			if (link.charAt(0) == '#') { 
				continue; 
			}
			// Skip mailto links. 
			if (link.indexOf("mailto:") != -1) {
				continue; 
			}
			
			// Skip JavaScript links. 
			/*if (link.toLowerCase().indexOf("javascript") != -1) { 
				continue; 
			}*/
			
			// Prefix absolute and relative URLs if necessary. 
			if (link.indexOf("://") == -1) { 
				// Handle absolute URLs. 
				if (link.charAt(0) == '/') { 
					link = "http://" + pageUrl.getHost() + link; 
					// Handle relative URLs. 
				} 
				else { 
					String file = pageUrl.getFile(); 
					if (file.indexOf('/') == -1) { 
						link = "http://" + pageUrl.getHost() + "/" + link; 
					} 
					else { 
						String path = file.substring(0, file.lastIndexOf('/') + 1); 
						link = "http://" + pageUrl.getHost() + path + link; 
					} 
				} 
			}
			
			// Remove anchors from link. 
			int index = link.indexOf('#'); 
			if (index != -1) { 
				link = link.substring(0, index);
			}
			
			// Remove leading "www" from URL's host if present. 
			//link = removeWwwFromUrl(link);

			// Verify link and skip if invalid. 
			URL verifiedLink = verifyUrl(link); 
			if (verifiedLink == null) { 
				//System.out.println("verified link "+verifiedLink);
				continue; 
			}
			
			/* If specified, limit links to those having the same host as the start URL. */ 
			if (/*limitHost && */pageUrl.getHost().toLowerCase().equals( verifiedLink.getHost().toLowerCase())) { 
				continue; 
			}
			
			// Skip link if it has already been crawled. 
			if (crawledList.contains(link)) { 
				continue; 
			}
			
			// Skip link if it has already been added to linkList. 
			if (linkList.contains(link)) { 
				continue; 
			}
			
			//System.out.println("Adding link "+link);
			// Add link to list. 
			linkList.add(link+"");
			
		}
		return (linkList);

	}
	
	//Verify URL format. 
		private URL verifyUrl(String url) { 
			
			/*// Only allow HTTP URLs. 
			if (!url.toLowerCase().startsWith("http://")) 
				return null;*/
			
			//Verify format of URL. 
			URL verifiedUrl = null; 
			try { 
				verifiedUrl = new URL(url); 
			} catch (Exception e) { 
				return null; 
			}
			
			return verifiedUrl;
		}

		private String removeWwwFromUrl(String url) { 
			int index = url.indexOf("://www."); 
			if (index != -1) { 
				return url.substring(0, index + 3) + url.substring(index + 7); 
			}
			return (url);
		}

		public Map<String,String> getImageTags(String pageContents, URL url) {
			System.out.println("IN get images");
			Map<String,String> imagesAndAlts = new HashMap<String,String>();
			Pattern pattern = Pattern.compile("<img[^>]*(.*?)>");
	        Matcher matcher = pattern.matcher(pageContents);
	        int i =0;
	        while(matcher.find()) {
		        	System.out.println(matcher.group());
	            	String s = matcher.group();
	            	/*String m = "src=\"";
	            	int c = s.indexOf(m)+m.length();*/
	            	String splits = "";
	            	if(s.contains("src=")) {
		            	String[] dummy = s.split("src=\"");
		            	String [] dummy2 = dummy[1].split("\"");
		            	splits = dummy2[0];
	            	}
	            	
	            	if(!splits.contains("//")) {
	            		imagesAndAlts.put( i+"",url.getHost()+splits);
	            			
	            	}
	            	else {
		            		imagesAndAlts.put(i+"",splits);
	            	}
		        i++;
	        }
	        return imagesAndAlts;  
		}

		public String getTitleOfPage(String pageContents) {
			String title = "";
			Pattern pattern = Pattern.compile("<title>(.+?)</title>", Pattern.DOTALL);
	        Matcher matcher = pattern.matcher(pageContents);
	        while(matcher.find()) {
		        String html = matcher.group();
		        title = html.substring(html.indexOf("<title>") + 7, html.indexOf("</title>"));
	        }
			return title;
		}
		
		public Map<Integer,Float> getNeighbouringText(String pageContents,ArrayList<String> words) {
			//System.out.println(" bye bye");
			pageContents = pageContents.replaceAll("\\<img.*?\\>", " poojachandralekha ");
			//pageContents = pageContents.replaceAll("\\<.*?\\>", "");
			pageContents = pageContents.replaceAll("<script[^>]*>(.*?)</script>", "");
			pageContents = pageContents.replaceAll("<head[^>]*>(.*?)</head>", "");
			Map<Integer,Float> indicesScore = new HashMap<Integer,Float>();
			int j=0;
			for (int i = -1; (i = pageContents.indexOf("poojachandralekha", i + 1)) != -1; i++) {
				indicesScore.put(j,(float) 0);
			    j++;
			} 
			ArrayList<String> content = new ArrayList<String>();
			Pattern p = Pattern.compile("[a-zA-Z]+"); 
			Matcher m1 = p.matcher(pageContents);
			while (m1.find()) { 
				content.add(m1.group().toLowerCase());
			} 
			ArrayList<Integer> pcIndices = new ArrayList<Integer>();
			for(int i=0;i<content.size();i++) {
				if(content.get(i).equals("poojachandralekha")) {
					pcIndices.add(i);
				}
			}
			
			for(int i=0;i<content.size();i++) {
				int sIndex ;
				if(words.contains(content.get(i))) {
					sIndex = i;
					for(int k=0;k<pcIndices.size();k++) {
						float val = (float) (indicesScore.get(k) + (1/Math.pow(sIndex - pcIndices.get(k), 2)));
						indicesScore.replace(k, val);
					}
				}
			}
			for(Integer f:indicesScore.keySet()) {
				System.out.println(f + " : " +indicesScore.get(f));
			}
			return indicesScore;
		}
		
}
