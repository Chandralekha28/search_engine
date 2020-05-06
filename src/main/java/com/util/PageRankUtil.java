package com.util;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.la4j.Matrix;
import org.la4j.matrix.SparseMatrix;
import org.la4j.matrix.dense.Basic1DMatrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import com.controller.DBController;

public class PageRankUtil {
	
	DecimalFormat df2 = new DecimalFormat("#.###");
	
	DBController dbController = new DBController();
	
	public void calculatePageRank() {
		SparseMatrix matrix = SparseMatrix.from2DArray(dbController.getCountOutGoing());
		double[] row = new double[matrix.columns()];
		
		//counting the number of occurences in each row
		for(int i=0;i<matrix.rows();i++) {
			for(int j=0;j<matrix.columns();j++) {
				row[i] = (double)Math.round((row[i]+matrix.get(i, j)) * 1000d) / 1000d ;
			}
		}
		
		//based on the row count update matrix -> divide by row count or update with 1/|V|
		//matrix = T ; after handling dangling nodes
		for(int i=0;i<matrix.rows();i++) {
			for(int j=0;j<matrix.columns();j++) {
				if(row[i]==0)
					matrix.set(i, j, 1.00/ (matrix.columns()));
				else
					matrix.set(i, j, matrix.get(i, j)/row[i]);
			}
		}
		
		//creating vector = (E)[1...1]T [1/|V|.....1/|V|]
		double[][] v = new double[matrix.columns()][matrix.columns()];
		Matrix vector = new Basic2DMatrix(v);
		vector.add((1.00 / matrix.columns()) *0.1);
		
		//multiplying matrix = (1-E)T
		matrix.multiply(0.9);
		
		//P = (1-E)T + (E)[1...1]T [1/|V|.....1/|V|]
		Matrix P = matrix.add(vector);
		
		//power iteration
		double[] pi1 = new double[matrix.columns()];
		pi1[0] = 1.00;
		for(int i=1;i<matrix.columns();i++)
			pi1[i] = 0.00;
		Matrix pi =  new Basic1DMatrix(1,matrix.columns(),pi1);
		
		//Iterating piNew = pi*P
		double norm = 0.00;
		do {
			Matrix newpi = pi.multiply(P);
			System.out.println(newpi);
			Matrix v1 = newpi.subtract(pi);
			norm = v1.norm();
			pi = newpi;
		}	while(norm>0.001);	
		
		dbController.addPageRankToDocuments(pi);
	}
	
	public float okapi(ArrayList<String> queryTerms) {
		dbController.updateViewbm25();
		float score = dbController.getbm25Score(queryTerms);
		return score;
	}
	
}
