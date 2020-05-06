package com.wse;

import java.util.Scanner;

import org.json.JSONObject;

public class CLIInterface {
	public static void main(String args[]) {
		String query = "";
		for(int i=0;i<args.length;i++) {
			query = query+","+args[i];
		}
		WebappApplication a = new WebappApplication();
		a.crawl();
		System.out.println("Enter C for conjunctive or D for Disjunctive query..");
		Scanner scan = new Scanner(System.in);
		String disCon = scan.nextLine();
		String results = a.search(query,disCon,5,3,"en", null);
		JSONObject json = new JSONObject(results);
		System.out.println(json.get("resultList"));
	}

}
