package com.ogb.auc.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import com.ogb.auc.ndnsec.NDNSecManager;

public class SqliteManager {
	
	 private Connection connection = null;
	 private Statement  stmt = null;
	 private static SqliteManager sharedInstance = null;
	 
	 private SqliteManager()
	 {
		 try {
			 Class.forName("org.sqlite.JDBC");
		 }
		 catch (Exception e) {
			 e.printStackTrace();
		 }
	 }
	 
	 public static SqliteManager getInstance()
	 {
		 if (sharedInstance == null)
			 sharedInstance = new SqliteManager();
		 return sharedInstance;
	 }
	
	 public ArrayList<String> findUser(String username)
	 {
		 ArrayList<String> results = new ArrayList<String>();
		 ResultSet rs = null;
		 try {
			 connection = DriverManager.getConnection("jdbc:sqlite:"+NDNSecManager.NDN_PATH+"/.ndn/ndnsec-public-info.db");
			 connection.setAutoCommit(false);
			 System.out.println("Opened database successfully");
			 stmt = connection.createStatement();
			 rs = stmt.executeQuery("SELECT * FROM Key AS K WHERE K.identity_name==\""+ username +"\"");
			 while ( rs.next() ) 
				 results.add(rs.getString(2));
			 rs.close();
			 stmt.close();
			 connection.close();
		 } 
		 catch ( Exception e ) {
			 System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		 }
		 System.out.println("Operation done successfully");
		 return results;
	 }
	 
	 
	 public ArrayList<String> findKeyLocatorOfUser(String username)
	 {
		 ArrayList<String> results = new ArrayList<String>();
		 ResultSet rs = null;
		 try {
			 connection = DriverManager.getConnection("jdbc:sqlite:"+NDNSecManager.NDN_PATH+"/.ndn/ndnsec-public-info.db");
			 connection.setAutoCommit(false);
			 System.out.println("Opened database successfully");
			 stmt = connection.createStatement();
			 rs = stmt.executeQuery("SELECT cert_issuer FROM Certificate AS C WHERE C.identity_name==\""+ username +"\"");
			 while ( rs.next() ) 
				 results.add(rs.getString(1));
			 rs.close();
			 stmt.close();
			 connection.close();
		 } 
		 catch ( Exception e ) {
			 System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		 }
		 System.out.println("Operation done successfully");
		 return results;
	 }

}
