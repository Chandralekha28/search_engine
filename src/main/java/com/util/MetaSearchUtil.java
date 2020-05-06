package com.util;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.controller.DBController;

public class MetaSearchUtil {
	
	DBController dbController = new DBController();

	public void calculateT(ArrayList<JSONObject> jsonArray,double avg_cw){
		for(JSONObject json :jsonArray) {
			JSONArray stat = json.getJSONArray("stat");
			int cw = json.getInt("cw");
			double t=0.0;
			String host = json.getString("host");
			for(int i=0;i<stat.length();i++){ 
				JSONObject obj = stat.getJSONObject(i);
				int df = obj.getInt("df");
				String term = obj.getString("term");
				t=df/(df+50+150*cw/avg_cw);
				dbController.updateTInMeta(host,term,t);
			}
		}
	}

	public void calculateI(int length, ArrayList<JSONObject> jsonArray) {
		for(JSONObject json :jsonArray) {
			String host = json.getString("host");
			JSONArray stat = json.getJSONArray("stat");
			for(int i=0;i<stat.length();i++){ 
				JSONObject obj = stat.getJSONObject(i);
				String term = obj.getString("term");
				int cf = dbController.getCfForTerm(term);
				if(cf==0)
					cf=1;
				double I =  Math.log10((jsonArray.size()+0.5)/cf) / Math.log10(jsonArray.size() +1.0);
				double t = dbController.getTForTermAndHost(term,host);
				double score = 0.4+(0.6*t*I);
				System.out.println("score:"+score+host+term+t+" "+I);
				//double score = 0.4+(1-0.4)*t*I;
				dbController.updateI(I,score,host,term);
			}
		}
	}
}
