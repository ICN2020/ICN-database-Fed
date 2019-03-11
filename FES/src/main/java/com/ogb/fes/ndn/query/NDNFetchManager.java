package com.ogb.fes.ndn.query;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;

import com.ogb.fes.ndn.query.NDNFixedWindowSegmentFetcher;
import com.ogb.fes.ndn.query.NDNFixedWindowSegmentFetcher.ErrorCode;
import com.ogb.fes.ndn.query.NDNFixedWindowSegmentFetcher.OnComplete;
import com.ogb.fes.ndn.query.NDNFixedWindowSegmentFetcher.OnError;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;


public class NDNFetchManager implements ICNFetchManager {
	
	public class NDNFetchName implements OnComplete, OnError {
		
		@Override
		public void onError(ErrorCode errorCode, String message) {

			resultCount++;
			inFlightQueries--;
			
			System.out.println(DateTime.currentTime()+"NDNFetchName - Timeout data packet (" + resultCount + "/" + ndnRequestNames.size()+")");
			
			if (!message.contains("Network Nack"))
				System.out.println(DateTime.currentTime()+ "NDNFetchName - Error " + errorCode + " for interest " + message);
			
			System.out.println(DateTime.currentTime()+"NDNFetchName - OnError Time Elapsed: " + (stopTime-startTime) + "ms");
		}

		@Override
		public void onComplete(Blob content) {
			
			resultCount++;
			inFlightQueries--;

			Data data = new Data();
			data.setContent(content);

			contentBuffers.add( getStringElement(data.getContent().buf()) );
			//System.out.println("NDNResolver - OnComplete Time Elapsed: " + (stopTime-startTime) + "ms"+"inFlight: "+inFlightFetcher);
		}
	}
	
	public class NDNFetchContent implements OnComplete, OnError {
		
		@Override
		public void onError(ErrorCode errorCode, String message) {

			resultCount++;
			inFlightQueries--;
			
			System.out.println(DateTime.currentTime()+"NDNFetchContent - Timeout data packet (" + resultCount + "/" + ndnRequestNames.size()+")");
			
			if (!message.contains("Network Nack"))
				System.out.println(DateTime.currentTime()+ "NDNFetchContent - Error " + errorCode + " for interest " + message);
			
			System.out.println(DateTime.currentTime()+"NDNFetchName - OnError Time Elapsed: " + (stopTime-startTime) + "ms");
		}

		@Override
		public void onComplete(Blob content) {
			
			resultCount++;
			inFlightQueries--;

			Data data = new Data();
			data.setContent(content);
			
			String result = getStringElement(data.getContent().buf());
			contentBuffers.add(result);
			//System.out.println("NDNResolver - OnComplete Time Elapsed: " + (stopTime-startTime) + "ms"+"inFlight: "+inFlightFetcher);
		}
	
	}
	
	
	
	public static String serverIP;
	
	private ArrayList<String> contentBuffers;
	private HashSet<String>   ndnRequestNames;
	
	private int  resultCount;
	private int  requestTimeout;
	private Face face;
	
	private int  querySent;
	private int  inFlightQueries;
	private int  queryWindow;
	
	private long startTime = 0;
	private long stopTime  = 0;
	
	private NDNFetchName    ndnFetchName;
	private NDNFetchContent ndnFetchContent;
	
	
	
	//Constructor
	public NDNFetchManager() {
		super();
		
		init();

		ndnFetchName    = new NDNFetchName();
		ndnFetchContent = new NDNFetchContent();
	}
	
	public NDNFetchManager(String serverIP) {
		super();
		
		NDNFetchManager.serverIP = serverIP;
		
		init();

		ndnFetchName    = new NDNFetchName();
		ndnFetchContent = new NDNFetchContent();
	}
	
	public void init() {
		this.resultCount           = 0;
		this.querySent             = 0;
		this.inFlightQueries       = 0;
		this.queryWindow           = 4;
		this.requestTimeout        = 500;
		this.face                  = new Face(NDNFetchManager.serverIP);
		this.contentBuffers        = new ArrayList<String>();
		this.ndnRequestNames       = new HashSet<String>();
	}
	
	
	
	
	private String getStringElement(ByteBuffer contentBuffer) {
		
		try {
			StringBuilder resultRow = new StringBuilder();
		    if (contentBuffer != null) {
		    	for (int i = contentBuffer.position(); i < contentBuffer.limit(); ++i) {
		    		resultRow.append((char)contentBuffer.get(i));
		    	}
		    }
		    
		    return resultRow.toString();
	    }
		catch (Exception e) {
			System.out.println(DateTime.currentTime()+"NDNFetchManager - Exception: " + e.getMessage());
			
			return "";
		}
	}
	/***************************************************************/
	
	
	
	/******************ICNFetchManager Interface********************/
	@Override
	public ArrayList<Object> fetch(ArrayList<String> names) {
		
		ArrayList<Object> result = fetchData(names, ndnFetchName, ndnFetchName);
		
		HashSet<String> resultNames = new HashSet<String>();
		for (Object obj : result) {
			String stringObj = ((String)obj).replace("\"", "");
			
			if (stringObj.length() < 3)
				continue;
			
			for (String name : stringObj.split(",")) {
				resultNames.add(name);
// 				System.out.println("NDNFetchManager - oid: "+name);
			}
		}
		
		if (resultNames.size() <= 0)
			return new ArrayList<Object>();
		
		result = fetchData(new ArrayList<>(resultNames), ndnFetchContent, ndnFetchContent);
		
//		for (Object entry : result) {
//			if (entry instanceof String) {
//				String strEntry = (String)entry;
//				System.out.println("Entry Before = " + strEntry);
//				strEntry = strEntry.replaceAll("\\", "");
//				strEntry = strEntry.replaceAll("\"{", "{");
//				strEntry = strEntry.replaceAll("\"}", "}");
//				System.out.println("Entry After = " + strEntry);
//				entry = strEntry;
//			}
//		}

		return result;
	}
	
	public ArrayList<Object> fetchElement(ArrayList<String> names) {
		
		return fetchData(names, ndnFetchContent, ndnFetchContent);	
	}
	
	public ArrayList<Object> fetchElement(ArrayList<String> names, int timeout) {
		
		return fetchData(names, ndnFetchContent, ndnFetchContent, timeout);	
	}
	
	public ArrayList<Object> deleteElement(ArrayList<String> names) {
		
		return fetchData(names, ndnFetchName, ndnFetchName);	
	}
	
	private ArrayList<Object> fetchData(ArrayList<String> ndnRequestNameList, NDNFixedWindowSegmentFetcher.OnComplete onComplete, NDNFixedWindowSegmentFetcher.OnError onError) {
		
		init();
		try {
			//System.out.println("Request list for: " + ndnRequestNameList.size());
			while (querySent < ndnRequestNameList.size()) {
				if (inFlightQueries < queryWindow) {
					String   reqName  = ndnRequestNameList.get(querySent);
					Name     name     = new Name(reqName);
					if (!name.get(-1).isSegment())
						name.appendSegment(0);
					Interest interest = new Interest(name, requestTimeout);
		
					NDNFixedWindowSegmentFetcher.fetch(face, interest, NDNFixedWindowSegmentFetcher.DontVerifySegment, onComplete, onError);
					//System.out.println(DateTime.currentTime()+"NDNFetchManager - fetchNames - Interest " +name.toUri());
					
					inFlightQueries++;
					querySent++;
				}
				else {
					try {
						face.processEvents();
						Thread.yield();
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			//Flush of interest enqueued 
			while (resultCount < ndnRequestNameList.size()) {
				try {
					face.processEvents();
					Thread.yield();
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			face.shutdown();
	    }
		catch (Exception e) {
			System.out.println(DateTime.currentTime()+"NDNFetchManager - fetchNames - Exception: " + e.getMessage());
		}
		
		//System.out.println(DateTime.currentTime()+"NDNFetchManager - fetchNames - ContentBuffer: " + contentBuffers);
		return new ArrayList<Object>(contentBuffers);
	}

	private ArrayList<Object> fetchData(ArrayList<String> ndnRequestNameList, NDNFixedWindowSegmentFetcher.OnComplete onComplete, NDNFixedWindowSegmentFetcher.OnError onError, int timeout) {
		
		init();
		try {
			//System.out.println("Request list for: " + ndnRequestNameList.size());
			while (querySent < ndnRequestNameList.size()) {
				if (inFlightQueries < queryWindow) {
					String   reqName  = ndnRequestNameList.get(querySent);
					Name     name     = new Name(reqName);
					if (!name.get(-1).isSegment())
						name.appendSegment(0);
					Interest interest = new Interest(name, timeout);
		
					NDNFixedWindowSegmentFetcher.fetch(face, interest, NDNFixedWindowSegmentFetcher.DontVerifySegment, onComplete, onError);
					//System.out.println(DateTime.currentTime()+"NDNFetchManager - fetchNames - Interest " +name.toUri());
					
					inFlightQueries++;
					querySent++;
				}
				else {
					try {
						face.processEvents();
						Thread.yield();
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			//Flush of interest enqueued 
			while (resultCount < ndnRequestNameList.size()) {
				try {
					face.processEvents();
					Thread.yield();
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			face.shutdown();
	    }
		catch (Exception e) {
			System.out.println(DateTime.currentTime()+"NDNFetchManager - fetchNames - Exception: " + e.getMessage());
		}
		
		//System.out.println(DateTime.currentTime()+"NDNFetchManager - fetchNames - ContentBuffer: " + contentBuffers);
		return new ArrayList<Object>(contentBuffers);
	}
	
	/***************************************************************/
}
