package com.controller;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.la4j.Matrix;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;

import com.database.DBConnector;
import com.database.DBConnector.IsolationLevel;
import com.entity.AdData;

@Controller
@Repository
public class DBController {
	
	Connection getConnection(IsolationLevel isolationLevel) {
    	if(DBConnector.testConnection(isolationLevel)) {
            try {
            	Connection conn = DBConnector.getConnection(isolationLevel);
            	return conn;
			} catch (SQLException e) {
				e.printStackTrace();
			}
        } else {
            System.out.println("Database could not connect");
        }
		return null;
    }
	
	public void createSchema() {
	
	  String createSequence = "CREATE SEQUENCE IF NOT EXISTS document_sequence START WITH 0 " + 
	  		" INCREMENT BY  1 minvalue -1";
	  String cr_documents = "CREATE TABLE IF NOT EXISTS documents (docid int not null DEFAULT nextval('document_sequence'), url varchar(1000), "
	  		+ " crawled_on_date varchar(500), crawled boolean,depth int, pagerank float, lang varchar(50), title varchar(1000), content varchar(10485759), content_tags varchar(10485759), " 
			+ " primary key (docid))";
	  String alterDocuments = "ALTER SEQUENCE document_sequence OWNED BY documents.docid";
	  
      String cr_feature = "CREATE TABLE IF NOT EXISTS features (docid int, term varchar(1000), term_frequency float,tf float, tfidf float, "
      		+ "primary key (docid,term), " 
    		+ "FOREIGN KEY (docid) REFERENCES documents(docid))";
      
      String cr_images = "CREATE TABLE IF NOT EXISTS images (docid int, image_url varchar(1000), position int, "
        		+ "primary key (docid,position), " 
        		+ "FOREIGN KEY (docid) REFERENCES documents(docid))";
      
      String cr_links = "CREATE TABLE IF NOT EXISTS links (from_docid int, to_docid int, "
      		+ "primary key (from_docid,to_docid), "
      		+ "FOREIGN KEY (from_docid) REFERENCES documents(docid), " 
    		+ "FOREIGN KEY (to_docid) REFERENCES documents(docid))";
      
      String cr_ads = "create TABLE IF NOT EXISTS ads(n_grams varchar(10000),url varchar(1000) primary key,image_url varchar(1000),"
      		+ "text varchar(1000),budget float,bill_after_clicks float);";
      
      String cr_idf = "CREATE TABLE IF NOT EXISTS idf( term varchar(1000) primary key, idf float, okapi_idf float)";
      
      String cr_meta = "CREATE TABLE IF NOT EXISTS meta(host varchar(100), term varchar(1000), df int, cw int, T float, I float,score float,primary key (host,term) )";
      
      String cr_idfIndex = "CREATE INDEX IF NOT EXISTS idf_index on idf using hash(term)";
      
      String cr_featuresIndex = "CREATE INDEX IF NOT EXISTS features_index on features using hash(term)";
      
      String cr_DocsIndex = "create index if not exists documentscrawled_index on documents using hash(crawled)";
      
      String cr_docsUrlIndex = "create index if not exists documentsurl_index on documents using hash(url)" ;
      
      String cr_view_featuresTfIdf = "CREATE OR REPLACE VIEW features_tfidf AS SELECT docid, term, term_frequency, tf, tfidf as score from features";
      
      String cr_view_featuresbm25 = "CREATE OR REPLACE VIEW features_bm25 AS  " + 
      		"	SELECT docid, term,term_frequency,  " + 
      		"	(select okapi_idf from idf where term=f.term) * (f.term_frequency * (1.5+1) )/ (f.term_frequency + 1.5 *(1-0.75+0.75* " + 
      		"	((select count(*) from features f1 where f.docid=f1.docid)/((select sum(f3.term_frequency) from features f3 )/(select count(*) from documents)))) ) " + 
      		"	as score " + 
      		"	from features f";
      
      String cr_view_featuresCombined = "CREATE OR REPLACE VIEW features_combined AS \r\n" + 
      		"SELECT docid, term, term_frequency, \r\n" + 
      		"(0.7* score + 0.3 * (select pagerank from documents where docid=f.docid))\r\n" + 
      		"as score from features_bm25 f";
      
      try {
    	  PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(createSequence);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_documents);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(alterDocuments);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_feature);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_images);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_links);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_idf);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_ads);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_meta);
    	  s1.executeUpdate();

    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_idfIndex);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_featuresIndex);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_DocsIndex);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_docsUrlIndex);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_view_featuresTfIdf);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_view_featuresbm25);
    	  s1.executeUpdate();
    	  
    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
    			  .prepareStatement(cr_view_featuresCombined);
    	  s1.executeUpdate();
    	  
      }catch (SQLException e) {
			e.printStackTrace();
      }
	}
	
	public void insertOutDocument(String url,int depth){
		String q = "INSERT INTO documents(url,crawled_on_date, crawled,depth) VALUES(?,?,?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			 s.setString(1, url+"");
			 s.setString(2, (new Date()).toString());
			 s.setBoolean(3, false);
			 s.setInt(4, depth);
			 s.executeUpdate();
			 
        } catch (SQLException e) {
			e.printStackTrace();
        }
	}
	
	public void insertDocument(String url,int depth,String lang,String title,String content, String contentWithTags){
		String q = "INSERT INTO documents(url,crawled_on_date, crawled,depth,lang,title,content,content_tags) VALUES(?,?,?,?,?,?,?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			 s.setString(1, url+"");
			 s.setString(2, (new Date()).toString());
			 s.setBoolean(3, false);
			 s.setInt(4, depth);
			 s.setString(5, lang);
			 s.setString(6, title);
			 s.setString(7, content);
			 s.setString(8, contentWithTags);
			 s.executeUpdate();
			 
        } catch (SQLException e) {
			e.printStackTrace();
        }
	}
	

	public void saveLinks() {
		getAllDocuments();
		String q = "INSERT INTO links(from_docid,to_docid) VALUES(?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			// s.setString(1, url+"");
			 s.setString(2, (new Date()).toString());
			 s.executeUpdate();
			 
        } catch (SQLException e) {
			e.printStackTrace();
        }
	}

	public ArrayList<String> getAllDocuments() {
		ArrayList<String> docs = new ArrayList<String>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM documents");
			while (rs.next())
			{
			    docs.add(rs.getString("url"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return docs;
	}
	
	public ArrayList<String> getAllCrawledDocuments() {
		ArrayList<String> docs = new ArrayList<String>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM documents WHERE crawled=true");
			while (rs.next())
			{
			    docs.add(rs.getString("url"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return docs;
	}

	public void markCrawledLink(String link) {
		String SQL = "UPDATE documents SET crawled = ? WHERE url = ?";
		PreparedStatement pstmt;
		int affectedrows = 0;
		try {
			pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL);

	        pstmt.setBoolean(1, true);
	        pstmt.setString(2, link);
	 
	        affectedrows = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getUrlId(String link) {
		Statement st;
		int docid = 0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT docid FROM documents WHERE url = ?");
			pr.setString(1, link);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
			    docid = rs.getInt("docid");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docid;
	}
	
	public String getContentFromDocid(int docid) {
		Statement st;
		String content = "";
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT content FROM documents WHERE docid = ?");
			pr.setInt(1, docid);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
			    content = rs.getString("content");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return content;
	}
	
	public void insertOutgoingLinks(String link, String outgoingLink) {
		int mainId = getUrlId(link);
		int outGoingLink = getUrlId(outgoingLink);
		
		String q = "INSERT INTO links(from_docid,to_docid) VALUES(?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			 s.setInt(1, mainId);
			 s.setInt(2, outGoingLink);
			 s.executeUpdate();
			 
        } catch (SQLException e) {
			//System.out.println(link+"**************************************** LINK ALREADY EXISTS");
        }
	}

	public void insertTerms(String link, Map<String, Long> counts) {
		int docid = getUrlId(link);
		
		for(String name : counts.keySet()) {
			String q = "INSERT INTO features(docid,term, term_frequency,tf) VALUES(?,?,?,?)";
	        try {
				PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
				
				 s.setInt(1, docid);
				 s.setString(2, name);
				 //System.out.println(name);
				 s.setLong(3, counts.get(name));
				 s.setFloat(4, (float) (1 + Math.log10(counts.get(name))) );
				 s.executeUpdate();
				
				 
	        } catch (SQLException e) {
				e.printStackTrace();
	        }
		}
	}

	public int getMinDepth() {
		Statement st;
		int mindepth = 0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT MIN(depth) FROM documents where crawled=false");
			while (rs.next())
			{
			    return rs.getInt(1);
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	public int getMinDocOfDepth(int docid) {
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT MIN(docid) FROM documents where depth=? and crawled=false");
			 ps.setInt(1, docid);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
			    return(rs.getInt(1));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public ArrayList<String> getAllNotCrawledDocsFromDepth(int minDepth) {
		ArrayList<String> docs = new ArrayList<String>();
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT * FROM documents where depth=? and crawled=false");
			 ps.setInt(1, minDepth);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
			    docs.add(rs.getString("url"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docs;
	}

	public int getDepthOfDOc(String url) {
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT depth FROM documents where url=?");
			 ps.setString(1, url);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
			    return(rs.getInt(1));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public ArrayList<String> getAllNotCrawledDocuments() {
		ArrayList<String> docs = new ArrayList<String>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM documents WHERE crawled=false");
			while (rs.next())
			{
			    docs.add(rs.getString("url"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return docs;
	}

	public int getMaxDepth() {
		Statement st;
		int mindepth = 0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT MAX(depth) FROM documents");
			while (rs.next())
			{
			    return rs.getInt(1);
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}

	public ArrayList<String> getAllTermsFromFeatures() {
		ArrayList<String> terms = new ArrayList<String>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM features");
			while (rs.next())
			{
				terms.add(rs.getString("term"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return terms;

	}
	
	public ArrayList<Integer> getAllFeatures() {
		ArrayList<Integer> docs = new ArrayList<Integer>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM features");
			while (rs.next())
			{
			    docs.add(rs.getInt("docid"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return docs;

	}

	public ArrayList<Integer> getLinks() {
		ArrayList<Integer> links = new ArrayList<Integer>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM links");
			while (rs.next())
			{
			    links.add(rs.getInt("from_docid"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return links;
	}

	public void deleteAllFromIdf() {
		String SQL = "DELETE FROM idf";
		 
        try (
        	PreparedStatement pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
        	.prepareStatement(SQL)) {
            pstmt.executeUpdate();
 
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
	}

	public void insertToIdf(String name,int totalNoOfDocs, Long count) {
		String q = "INSERT INTO idf(term,idf,okapi_idf) VALUES(?,?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.SERIALIZABLE).prepareStatement(q);
			
			 s.setString(1, name);
			 s.setFloat(2, (float) Math.log10(totalNoOfDocs/count));
			 s.setFloat(3, (float) Math.log10((totalNoOfDocs-count+0.5)/(count+0.5)));
			 s.executeUpdate();
			
			 
        } catch (SQLException e) {
			e.printStackTrace();
        }
	}

	public Map<String,Float> getAllTermsAndTfsFromFeatures() {
		Map<String,Float> termsTf = new HashMap<String,Float>();
		Statement st;
		int x=0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM features");
			while (rs.next())
			{
				termsTf.put(rs.getString("term")+":"+(x++), rs.getFloat("tf"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return termsTf;
	}

	public float getIdfOfTerm(String term) {
		Statement st;
		float idf = 0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT idf FROM idf WHERE term = ?");
			pr.setString(1, term);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
			    idf = rs.getFloat("idf");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return idf;	
	}
	
	public void updateTfIdfInFeatures() {
		Map <String,Float> termTfs = getAllTermsAndTfsFromFeatures();
		String SQL = "UPDATE features "
                + "SET tfidf = ? "
                + "WHERE  term= ? and tf=?";
		float idf;
		for(String term:termTfs.keySet()) {
			String[] dummy = term.split(":");
			idf = getIdfOfTerm(dummy[0]); 
			idf = idf * termTfs.get(term);
	        try (
	        	PreparedStatement pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL)) {
	        	pstmt.setFloat(1, idf);
	            pstmt.setString(2, dummy[0]);
	            pstmt.setFloat(3, termTfs.get(term));
	            pstmt.executeUpdate();
	 
	        } catch (SQLException ex) {
	            System.out.println(ex.getMessage());
	        }
		}
	}

	public Map<Integer,Float> searchDisjunctive(String queryTerms,int topCount, int s,String language) {
		String tablename ="";
		if(s==1)
			tablename = "features_tfidf";
		if(s==2)
			tablename = "features_bm25";
		if(s==3)
			tablename = "features_combined";
		Statement st;

		Map<Integer,Float> docidScore = new HashMap<Integer,Float>();
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("select x.docid, x.score from(select distinct f.docid , "
							+ "(select sum(score) from "+tablename+" where term in "+queryTerms+" \r\n" + 
							"				  and docid=f.docid)as score  " + 
							"from "+tablename+" f )as x " + 
							"where x.score>0 " + 
							"order by score desc " + 
							"limit ?");
			pr.setInt(1, topCount);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				//System.out.println(rs.getInt("docid" )+"::"+rs.getFloat("score"));
			    docidScore.put( rs.getInt("docid"),rs.getFloat("score"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Map<Integer,Float> newdocidScore = new HashMap<Integer,Float>();
		newdocidScore = checkForLang(docidScore,language);
		return newdocidScore;	
	}

	public Map<Integer,Integer> searchConjunctive(String last, int noOfTerms, int s) {
		String tablename ="";
		if(s==1)
			tablename = "features_tfidf";
		if(s==2)
			tablename = "features_bm25";
		if(s==3)
			tablename = "features_combined";
				
		Statement st;

		Map<Integer,Integer> docidAndCount = new HashMap<Integer,Integer>();
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("select  docid,count(*) as c\r\n" + 
							"		from  " + tablename+
							"		where term in "+last+"  " + 
							"		group by docid");
			
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				//System.out.println("\\"+rs.getInt("docid" )+"::"+rs.getFloat("c"));
				if(rs.getInt("c")==noOfTerms)
					docidAndCount.put( rs.getInt("docid"),rs.getInt("c"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docidAndCount;	
	}
	
	public Map<Integer,Float> searchConScore(String queryTerms,int topCount, int noOfTerms, int s,String language) {
		Map<Integer,Integer> docidAndCount = searchConjunctive(queryTerms, noOfTerms,s);
		Statement st;

		Map<Integer,Float> docidScore = new HashMap<Integer,Float>();
		for(Integer docid: docidAndCount.keySet()) {
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("select x.docid, x.score from(select distinct f.docid , "
							+ "(select sum(tfidf) from features where term in "+queryTerms+" \r\n" + 
							"				  and docid=f.docid)as score \r\n" + 
							"from features f )as x\r\n" + 
							"where x.score>0 and docid=?\r\n" + 
							"order by score desc\r\n" + 
							"limit ?");
			pr.setInt(1, docid);
			pr.setInt(2, topCount);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				//System.out.println(rs.getInt("docid" )+"::"+rs.getFloat("score"));
				//if(docid==rs.getInt("docid"))
				docidScore.put( rs.getInt("docid"),rs.getFloat("score"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		}
		Map<Integer,Float> newdocidScore = new HashMap<Integer,Float>();
		newdocidScore = checkForLang(docidScore,language);
		return newdocidScore;	
	}
	
	private Map<Integer, Float> checkForLang(Map<Integer, Float> docidScore,String language) {
		Map<Integer,Float> newdocidScore = new HashMap<Integer,Float>();
		Statement st;
		for(Integer docid: docidScore.keySet()) {
			try {
				st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
				st.setFetchSize(0);
				PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						.prepareStatement("select * from documents where docid=?");
				pr.setInt(1, docid);
				ResultSet rs = pr.executeQuery();
				while (rs.next())
				{
					if(rs.getString("lang").equals(language)) {
						newdocidScore.put(docid, docidScore.get(docid));
					}
				}
				rs.close();
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			}
		return newdocidScore;
	}

	public String getUrlForDocid(int docid){
		Statement st;
		String url = "";
		
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT * FROM documents WHERE docid = ?");
			pr.setInt(1, docid);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				url = rs.getString("url");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return url;
	}

	public Map<Integer, Float> searchConScoreLink(String queryTerms,int topCount, int noOfTerms, 
			String containingLink,int s,String language) {
		//Map<Integer,Integer> docidAndCount = searchConjunctive(queryTerms, noOfTerms);
		String tablename ="";
		if(s==1)
			tablename = "features_tfidf";
		if(s==2)
			tablename = "features_bm25";
		if(s==3)
			tablename = "features_combined";
				
		Statement st;
		
		int docidOfContainedLink = getContainingUrlId(containingLink);

		Map<Integer,Float> docidScore = new HashMap<Integer,Float>();
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("select x.docid, x.score from(select distinct f.docid , "
							+ "(select sum(score) from "+tablename+" where term in "+queryTerms+" \r\n" + 
							"				  and docid=f.docid)as score \r\n" + 
							"from "+tablename+" f )as x\r\n" + 
							"where x.score>0 and docid=?\r\n" + 
							"order by score desc\r\n" + 
							"limit ?");
			pr.setInt(1, docidOfContainedLink);
			pr.setInt(2, topCount);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				//System.out.println(rs.getInt("docid" )+"::"+rs.getFloat("score"));
				//if(docid==rs.getInt("docid"))
				docidScore.put( rs.getInt("docid"),rs.getFloat("score"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Map<Integer,Float> newdocidScore = new HashMap<Integer,Float>();
		newdocidScore = checkForLang(docidScore,language);
		return newdocidScore;	
	}

	public int getContainingUrlId(String link) {
		Statement st;
		int docid = 0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT docid FROM documents WHERE url = ?");
			pr.setString(1, link+"%");
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
			    docid = rs.getInt("docid");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docid;
	}

	public Map<Integer, Float> searchDisjunctiveLink(String queryTerms,int topCount,
			String containingLink, int s,String language) {
		String tablename ="";
		if(s==1)
			tablename = "features_tfidf";
		if(s==2)
			tablename = "features_bm25";
		if(s==3)
			tablename = "features_combined";
		Statement st;

		int docidOfContainedLink = getContainingUrlId(containingLink);
		
		Map<Integer,Float> docidScore = new HashMap<Integer,Float>();
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("select x.docid, x.score from(select distinct f.docid , "
							+ "(select sum(score) from "+tablename+" where term in "+queryTerms+" \r\n" + 
							"				  and docid=f.docid)as score \r\n" + 
							"from "+tablename+" f )as x\r\n" + 
							"where x.score>0 and docid=?\r\n" + 
							"order by score desc\r\n" + 
							"limit ?");
			pr.setInt(1, docidOfContainedLink);
			pr.setInt(2, topCount);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				//System.out.println(rs.getInt("docid" )+"::"+rs.getFloat("score"));
			    docidScore.put( rs.getInt("docid"),rs.getFloat("score"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Map<Integer,Float> newdocidScore = new HashMap<Integer,Float>();
		newdocidScore = checkForLang(docidScore,language);
		return newdocidScore;
	}

	public int getDfForTerm(String term) {
		Statement st;
		int count = 0;
		
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT * FROM features WHERE term = ?");
			pr.setString(1, term);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				count++;
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count;
	}

	public Map<Integer, Float> searchAllRounder(String queryTerms, int topCount, String containingLink,
			ArrayList<String> quotedTerms, int noOfQueryTerms, int ss,String language) {
		Map<Integer,Float> docidandscore = new HashMap<Integer,Float>();
		Map<Integer,Float> qandlink = new HashMap<Integer,Float>();
		Map<Integer,Float> qandnolink = new HashMap<Integer,Float>();
		Map<Integer,Float> noqandlink = new HashMap<Integer,Float>();
		Map<Integer,Float> noqandnolink = new HashMap<Integer,Float>();
		
		//if there are quotes
		if(quotedTerms.size()>0) {
			if(!containingLink.equals("")) {
				String query="(";
			    for(String s: quotedTerms) {
			    	query=query+"'"+s+"',";
			    }
				String last = query.substring(0,query.length()-1);
				last=last+")";
				qandlink = searchConScoreLink(last, topCount, quotedTerms.size(), containingLink,ss,language);
				qandlink = checkForLang(qandlink, language);
				System.out.println("qandlink "+qandlink);
			}else {
				String query="(";
			    for(String s: quotedTerms) {
			    	query=query+"'"+s+"',";
			    }
				String last = query.substring(0,query.length()-1);
				last=last+")";
				qandnolink = searchConScore(last,topCount,quotedTerms.size(),ss,language);
				qandnolink = checkForLang(qandnolink, language);
				System.out.println("qandnolink "+qandnolink);
			}
		}
		//if there are no quotes
		else {
			if(!containingLink.equals("")) {
				noqandlink = searchDisjunctiveLink(queryTerms, topCount, containingLink,ss,language);
				noqandlink = checkForLang(noqandlink, language);
				System.out.println("noqandlink "+noqandlink);
			}else {
				noqandnolink = searchDisjunctive(queryTerms, topCount,ss,language);
				noqandnolink = checkForLang(noqandnolink, language);
				System.out.println("noqandnolink "+noqandnolink);
			}
		}
		docidandscore.putAll(qandlink);
		docidandscore.putAll(qandnolink);
		docidandscore.putAll(noqandlink);
		docidandscore.putAll(noqandnolink);
		return docidandscore;
	}

	public int getMaxDocid() {
		int max =0;
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT max(docid)as m FROM documents");
			while (rs.next())
			{
			    max=rs.getInt("m");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return max;
	}
	public double[][] getCountOutGoing() {
		int max = getMaxDocid();
		double[][] matrix = new double[max+1][max+1];
		Statement st;
		for(int i=0;i<=max;i++) {
			try {
				st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
				st.setFetchSize(0);
				PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						.prepareStatement("SELECT count(to_docid)as c, from_docid FROM links where to_docid = ?"
								+ " group by from_docid");
				pr.setInt(1, i);
				ResultSet rs = pr.executeQuery();
				while (rs.next())
				{
					matrix[rs.getInt("from_docid")] [i] = rs.getInt("c")*1.00;
				}
				rs.close();
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return matrix;
	}
	
	public int getOutOfFromDocid(int docid) {
		int count = 0;
		Statement st;
			try {
				st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
				st.setFetchSize(0);
				PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						.prepareStatement("SELECT count(to_docid)as c FROM links where from_docid = ?");
				pr.setInt(1, docid);
				ResultSet rs = pr.executeQuery();
				while (rs.next())
				{
					count = rs.getInt("c");
				}
				rs.close();
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		return count;
	}

	public void addPageRankToDocuments(Matrix pi) {
		String SQL = "UPDATE documents SET pagerank = ? WHERE docid = ?";
		PreparedStatement pstmt;
		int affectedrows = 0;
		for(int i=0;i<pi.columns();i++) {
			try {
				pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL);
	
		        pstmt.setDouble(1, pi.get(0, i));
		        pstmt.setInt(2, i);
		 
		        affectedrows = pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateViewbm25() {
		String cr_view_featuresbm25 = "CREATE OR REPLACE VIEW features_bm25 AS  " + 
	      		"	SELECT docid, term,term_frequency,  " + 
	      		"	(select okapi_idf from idf where term=f.term) * (f.term_frequency * (1.5+1) )/ (f.term_frequency + 1.5 *(1-0.75+0.75* " + 
	      		"	((select count(*) from features f1 where f.docid=f1.docid)/((select sum(f3.term_frequency) from features f3 )/(select count(*) from documents)))) ) " + 
	      		"	as score " + 
	      		"	from features f";
	    PreparedStatement s1;
		try {
			s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					  .prepareStatement(cr_view_featuresbm25);
	    	s1.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void updateViewTfidf() {
		String cr_view_featuresTfIdf = "CREATE OR REPLACE VIEW features_tfidf AS SELECT docid, term, term_frequency, tf, tfidf as score from features";
	      
	    PreparedStatement s1;
		try {
			s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					  .prepareStatement(cr_view_featuresTfIdf);
	    	s1.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	public float getbm25Score(ArrayList<String> queryTerms) {
		float score = 0;
		for(String term:queryTerms) {
			try {
				 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						 .prepareStatement("SELECT * FROM features_bm25 where term=?");
				ps.setString(1, term.toLowerCase());
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
				    score = score + rs.getFloat("score");
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return score;
	}

	public void updateLangForDocument(String link, String lang,String title, String pageContents, String contentWithTags) {
		String SQL = "UPDATE documents SET lang = ? , content=? ,title=?,content_tags=? WHERE url = ?";
		PreparedStatement pstmt;
		int affectedrows = 0;
		try {
			pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL);

	        pstmt.setString(1, lang);
	        pstmt.setString(2, pageContents);
	        pstmt.setString(3, title);
	        pstmt.setString(4, contentWithTags);
	        pstmt.setString(5, link);
	 
	        affectedrows = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void saveImages(Map<String,String> imagesAndAlts,String link) {
		int docid = getUrlId(link);
		String q = "INSERT INTO images(docid, image_url, position) VALUES(?,?,?)";
		PreparedStatement s ;
		for(String ss : imagesAndAlts.keySet()) {
			try {
				s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
				 s.setInt(1, docid);
				 s.setInt(3, Integer.parseInt(ss));
				 s.setString(2, imagesAndAlts.get(ss));
				 s.executeUpdate();
				 
	        } catch (SQLException e) {
				e.printStackTrace();
	        }
		}
	}

	public boolean checkIfWordExitsInDocid(String word, int docid) {
		Statement st;
		int docidResult = -1;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT docid FROM features WHERE docid = ? and term=?");
			pr.setInt(1, docid);
			pr.setString(2, word);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
			    docidResult = rs.getInt("docid");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(docidResult!=-1)
			return true;
		else 
			return false;
	}

	public Map<String,Integer> getAllImagesDocidAndUrl() {
		Map<String,Integer> images = new HashMap<String,Integer>();
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery("SELECT * FROM images");
			while (rs.next())
			{
			    images.put(rs.getString("image_url"),rs.getInt("docid"));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return images;
	}

	public Map<String,String> getImagesForDocid(int docid) {
		Map<String,String> images = new HashMap<String,String>();
			try {
				 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						 .prepareStatement("SELECT * FROM images where docid=?");
				ps.setInt(1, docid);
				ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
				    images.put(rs.getString("image_url"), rs.getString("description"));
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		return images;
	}
	
	public void createShinglesTable() {
		String createShingles = "CREATE TABLE IF NOT EXISTS shingles( docid int, shingle varchar(100000), md5 varchar(1000), md5_int int, "
				+ " primary key (docid,shingle), " + 
				" FOREIGN KEY (docid) REFERENCES documents(docid) )";
		String index = "create index if not exists shingles_index on shingles using hash(docid)";
	      
	      try {
	    	  PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
	    			  .prepareStatement(createShingles);
	    	  s1.executeUpdate();
	    	  s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
	    			  .prepareStatement(index);
	    	  s1.executeUpdate();
	      }catch(Exception e ) {
	    	  e.printStackTrace();
	      }
	}
	
	public Map<Integer,String> getPageContentForAllDocs(){
		Map<Integer,String> docidContent = new HashMap<Integer,String>();
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT * FROM documents");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				docidContent.put(rs.getInt("docid"),rs.getString("content"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docidContent;
	}

	public void insertShingle(Integer docid, String shingle) {
		String q = "INSERT INTO shingles(docid,shingle,md5,md5_int) VALUES(?,?,?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			 s.setInt(1, docid);
			 s.setString(2, shingle.trim());
			 String query = "select md5(?)";
			 String hashValue="";
			 Statement st;
				try {
					PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(query);
					 s1.setString(1, shingle.trim());
					ResultSet rs = s1.executeQuery();
					while (rs.next())
					{
						hashValue = rs.getString("md5");
					}
					 
		        } catch (SQLException e) {
					e.printStackTrace();
		        }
			s.setString(3, hashValue);
			byte[] arr = hashValue.getBytes();
			ByteBuffer wrapped = ByteBuffer.wrap(arr); // big-endian by default
			int num = wrapped.getInt();
			s.setInt(4, num);
			s.executeUpdate();
        } catch (SQLException e) {
			System.out.println("Shingle already exists"+ shingle);
        }
	}

	public void createJaccardTable() {
		String createJaccard = "CREATE TABLE IF NOT EXISTS jaccard( docid1 int, docid2 int, jaccard float, "
				+ " primary key (docid1,docid2), FOREIGN KEY (docid2) REFERENCES documents(docid), " + 
				" FOREIGN KEY (docid1) REFERENCES documents(docid) )";
	      
	      try {
	    	  PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
	    			  .prepareStatement(createJaccard);
	    	  s1.executeUpdate();
	      }catch(Exception e ) {
	    	  e.printStackTrace();
	      }
	}

	public void calculateJaccardScore() {
		String jaccardScore = "with tempa(docid1, docid2, inter) as\r\n" + 
				"(\r\n" + 
				"	select abc.docid1 , abc.docid2  , count(*) \r\n" + 
				"	from (\r\n" + 
				"		SELECT s1.docid as docid1,s1.shingle as shingle1,s2.docid as docid2,s2.shingle as shingle2\r\n" + 
				"		FROM shingles s1 JOIN shingles s2 on s1.docid!=s2.docid\r\n" + 
				"	) as abc \r\n" + 
				"	where abc.shingle1=abc.shingle2\r\n" + 
				"	group by abc.docid1,abc.docid2\r\n" + 
				")\r\n" + 
				"select tempa.docid1,tempa.docid2,cast(\r\n" + 
				"(cast(tempa.inter as float)/(   cast((select count(*) from shingles where docid=tempa.docid1)as float)\r\n" + 
				"	+cast((select count(*) from shingles where docid=tempa.docid2) as float) - cast(tempa.inter as float)))as decimal(10,6)) as j\r\n" + 
				"																from tempa";
		
		float inter;
		Statement st;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();

			st.setFetchSize(0);
			ResultSet rs = st.executeQuery(jaccardScore);
			while (rs.next())
			{
			    inter = rs.getFloat("j");
			    int docid1= rs.getInt("docid1");
			    int docid2 = rs.getInt("docid2");
			    String q = "INSERT INTO jaccard(docid1,docid2,jaccard) VALUES(?,?,?)";
		        try {
					PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
					 s.setInt(1, docid1);
					 s.setInt(2, docid2);
					 s.setFloat(3, inter);
					 s.executeUpdate();
					 
		        } catch (SQLException e) {
					e.printStackTrace();
		        }
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> getJaccardScore(int docid, double threshold) {
		ArrayList<Integer> docs = new ArrayList<Integer>();
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("select jaccardThreshold(?,?)");
			 ps.setInt(1, docid);
			 ps.setDouble(2, threshold);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				docs.add(rs.getInt("jaccardThreshold"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docs;
	}
	
	public void createJaccardUDF() {
		String createJaccard = "create or replace function jaccardThreshold(id int,threshold float)\r\n" + 
				"returns table(docid int)\r\n" + 
				"AS $$\r\n" + 
				"begin\r\n" + 
				"return query\r\n" + 
				"select docid2 from jaccard where docid1=id and threshold<jaccard;\r\n" + 
				"end; $$\r\n" + 
				"language 'plpgsql';";
	      
	      try {
	    	  PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
	    			  .prepareStatement(createJaccard);
	    	  s1.executeUpdate();
	      }catch(Exception e ) {
	    	  e.printStackTrace();
	      }
	}
	
	public void createMinNShinglesUDF() {
		String createJaccard = "create or replace function calculateMinNShingles(docidParam int,limitParam int) \r\n" + 
				"returns table(md5int int) As $$\r\n" + 
				"begin\r\n" + 
				"return query\r\n" + 
				"select md5_int from shingles  \r\n" + 
				"where docid=docidParam\r\n" + 
				"order by md5_int\r\n" + 
				"limit limitParam;\r\n" + 
				"end; $$\r\n" + 
				"language 'plpgsql';";
	      
	      try {
	    	  PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
	    			  .prepareStatement(createJaccard);
	    	  s1.executeUpdate();
	      }catch(Exception e ) {
	    	  e.printStackTrace();
	      }
	}
	
	public ArrayList<Integer> getMinNShingles(int docid,int limit){
		ArrayList<Integer> mins = new ArrayList<Integer>();
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("select calculateMinNShingles(?,?)");
			 ps.setInt(1, docid);
			 ps.setInt(2, limit);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				mins.add(rs.getInt("calculateminnshingles"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return mins;
	}
	
	public ArrayList<Integer> getDistinctDocsForShingles(){
		ArrayList<Integer> docids = new ArrayList<Integer>();
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("select distinct(docid) from shingles");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				docids.add(rs.getInt("docid"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return docids;
	}
	
	public void createCompareShinglesUDF() {
		String createCompare = "create or replace function compareMins(docid1 int,docid2 int,limitParam int) \r\n" + 
				"returns float As $$\r\n" + 
				"DECLARE\r\n" + 
				"ccccc   int;\r\n" + 
				"begin \r\n" + 
				"	select count(*) into ccccc from (select calculateminnshingles(docid1,limitParam)) as docid1Mins,\r\n" + 
				"												(select calculateminnshingles(docid2,limitParam)) as docid2Mins\r\n" + 
				"																			  where docid1Mins.calculateminnshingles =docid2Mins.calculateminnshingles ;\r\n" + 
				"	return cast(ccccc as float)/cast(limitParam as float);\r\n" + 
				"end; $$ \r\n" + 
				"language 'plpgsql';";
	      
	      try {
	    	  PreparedStatement s1 = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
	    			  .prepareStatement(createCompare);
	    	  s1.executeUpdate();
	      }catch(Exception e ) {
	    	  e.printStackTrace();
	      }
	}
	
	public float compareNShingles(int docid1,int docid2,int limit){
		float minHashValue = 0 ;
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("select compareMins(?,?,?)");
			 ps.setInt(1, docid1);
			 ps.setInt(2, docid2);
			 ps.setInt(3, limit);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				minHashValue = rs.getFloat("compareMins");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return minHashValue;
	}
	
	public double getJaccardScoreForPair(int docid1,int docid2){
		double jaccardScore = 0 ;
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("select jaccard from jaccard where docid1=? and docid2=?");
			 ps.setInt(1, docid1);
			 ps.setInt(2, docid2);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				jaccardScore = rs.getDouble("jaccard");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return jaccardScore;
	}

	public String getPageContentWithTags(int docid) {
		String pageContentWithTags ="";
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT * FROM documents where docid=?");
			ps.setInt(1, docid);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				pageContentWithTags = rs.getString("content_tags");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pageContentWithTags;
	}

	public String getImageByDocidPosition(int docid, int position) {
		String image_url ="";
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT * FROM images where docid=? and position=?");
			ps.setInt(1, docid);
			ps.setInt(2, position);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				image_url = rs.getString("image_url");
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return image_url;		
	}
	
	//version 4
	public void insertAd(String nGrams,String url,String imageUrl, String text, float budget){
		String q = "INSERT INTO ads(n_grams,url, image_url,text,budget,bill_after_clicks) VALUES(?,?,?,?,?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			 s.setString(1, nGrams);
			 s.setString(2, url);
			 s.setString(3, imageUrl);
			 s.setString(4, text);
			 s.setDouble(5, budget);
			 s.setDouble(6, 0);
			 s.executeUpdate();
			 
        } catch (SQLException e) {
			e.printStackTrace();
        }
	}

	public ArrayList<AdData> getAds(String query) {
		ArrayList<AdData> adData = new ArrayList<AdData>();
		String image_url ="";
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT * FROM ads where n_grams like '%"+query+"%' and budget>bill_after_clicks limit 4");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				AdData ad = new AdData();
				ad.setImageUrl(rs.getString("image_url"));
				ad.setText(rs.getString("text"));
				ad.setUrl(rs.getString("url"));
				adData.add(ad);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return adData;	
	}

	public void reduceCost(String url) {
		double bill = getBillAfterClicksForUrl(url);
		String SQL = "UPDATE ads SET bill_after_clicks = ? WHERE url = ?";
		PreparedStatement pstmt;
		int affectedrows = 0;
		try {
			pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL);
			pstmt.setDouble(1, bill + 0.01);
	        pstmt.setString(2, url);
	 
	        affectedrows = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public double getBillAfterClicksForUrl(String url) {
		Statement st;
		double billAfterClicks = 0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT * FROM ads WHERE url = ?");
			pr.setString(1, url);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				billAfterClicks = rs.getDouble("bill_after_clicks");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return billAfterClicks;
	}

	public Map<String,Double> checkScore(String [] hosts, Set<String> words) {
		Statement st;
		Map<String,Double> res = new HashMap<String,Double>();
		String query="(",last="";
	    for(String s: words) {
	    	query=query+"'"+s+"',";
	    }
		last = query.substring(0,query.length()-1);
		last=last+")";
		for(int i=0;i<hosts.length;i++) {
			try {
				st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
				st.setFetchSize(0);
				PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						.prepareStatement("SELECT sum(score) FROM meta WHERE host=? and term in"+last);
				pr.setString(1, hosts[i]);
				ResultSet rs = pr.executeQuery();
				while (rs.next())
				{
					res.put(hosts[i], rs.getDouble("sum"));
					System.out.println(hosts[i]+" h:s "+rs.getDouble("sum"));
				}
				rs.close();
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	public boolean hostTermExists(String[] hosts, String word) {
		Statement st;
		int count=0;
		for(int i=0;i<hosts.length;i++) {
			try {
				st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
				st.setFetchSize(0);
				PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
						.prepareStatement("SELECT * FROM meta WHERE term=? and host =?");
				pr.setString(1, word);
				pr.setString(2, hosts[i]);
				ResultSet rs = pr.executeQuery();
				while (rs.next())
				{
					count++;
				}
				rs.close();
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if(count==hosts.length)
			return true;
		else
			return false;
	}

	public ArrayList<AdData> getAllAds() {
		ArrayList<AdData> adData = new ArrayList<AdData>();
		String image_url ="";
		try {
			 PreparedStatement ps = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					 .prepareStatement("SELECT * FROM ads where budget>bill_after_clicks");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				AdData ad = new AdData();
				ad.setImageUrl(rs.getString("image_url"));
				ad.setText(rs.getString("text"));
				ad.setUrl(rs.getString("url"));
				ad.setnGrams(rs.getString("n_grams"));
				ad.setScore(0);
				adData.add(ad);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return adData;
	}

	public void updateTInMeta(String host,String term,double t) {
		String SQL = "UPDATE meta SET t = ?  WHERE term = ? and host = ?";
		PreparedStatement pstmt;
		int affectedrows = 0;
		try {
			pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL);
			pstmt.setDouble(1, t);
			pstmt.setString(2, term);
			pstmt.setString(3, host);
	        affectedrows = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertHostAndTerm(String host, String term,int df, int cw,JSONArray resultList) {
		String q = "INSERT INTO meta(host,term,df,cw) VALUES(?,?,?,?)";
        try {
			PreparedStatement s = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(q);
			 s.setString(1, host);
			 s.setString(2, term);
			 s.setInt(3, df);
			 s.setInt(4, cw);
			 s.executeUpdate();
			 
        } catch (SQLException e) {
			e.printStackTrace();
        }
	}
	
	public int getCfForTerm(String term) {
		Statement st;
		int cf=0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT count(*) FROM meta WHERE term=? and df>0");
			pr.setString(1, term);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				cf = rs.getInt("count");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return cf;
	}

	public void updateI(double i, double score, String host, String term) {
		String SQL = "UPDATE meta SET i = ? , score=?  WHERE term = ? and host = ?";
		PreparedStatement pstmt;
		int affectedrows = 0;
		try {
			pstmt = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).prepareStatement(SQL);
			pstmt.setDouble(1, i);
			pstmt.setDouble(2, score);
			pstmt.setString(3, term);
			pstmt.setString(4, host);
	        affectedrows = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public double getTForTermAndHost(String term, String host) {
		Statement st;
		double t=0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT * FROM meta WHERE term=? and host=?");
			pr.setString(1, term);
			pr.setString(2, host);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				t = rs.getDouble("t");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return t;
	}

	public double getIFromMeta(String host) {
		Statement st;
		double i=0;
		try {
			st = getConnection(DBConnector.IsolationLevel.READ_COMMITTED).createStatement();
			st.setFetchSize(0);
			PreparedStatement pr = getConnection(DBConnector.IsolationLevel.READ_COMMITTED)
					.prepareStatement("SELECT * FROM meta WHERE host=? limit 1");
			pr.setString(1, host);
			ResultSet rs = pr.executeQuery();
			while (rs.next())
			{
				i = rs.getDouble("i");
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return i;
	}
	
	
}
