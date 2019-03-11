package com.ogb.fes.ndn.insert;


import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.ndn.NDNRepoMap;
import com.ogb.fes.ndn.query.NDNFetchManager;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.Utils;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;


public class NDNInsertResolver {
	
	private static NDNRepoMap repoMap = NDNRepoMap.sharedInstance();
	
	private static boolean isInFlight = true;


    
    public static void tcpBulkInsertion(NDNContentObject contentObject) {
    	
//    	System.out.println(DateTime.currentTime()+"NDNInsertResolver - Data Prefix Name: " + contentObject.nameURI.toUri());
//		Socket socket = repoMap.getRepoSocket(contentObject.getNameURI());
		
		//Content is inserted in first repo of IP-RES.conf
		Socket socket = repoMap.getFirstSocket();
		
//		System.out.println("TCP - FOR contentObject "+contentObject.getNameURI().toUri());
//		System.out.println(new String(contentObject.getSignedContent()));
		
		//System.out.println(DateTime.currentTime()+"NDNInsertResolver - Socket: " + socket);
		if (socket != null && socket.isConnected()) {
			sendOnTCP(socket, contentObject.getSignedContent());	
			
		}
		else {
			repoMap.checkAndFixSocketConnectionError(socket);
			if (socket != null && socket.isConnected())
				sendOnTCP(socket, contentObject.getSignedContent());	
		}
    }
     
    private static void sendOnTCP(Socket socket, byte[] value) {
    	
    	int  maxWaitTimeMillis = 10000;
    	long start = System.currentTimeMillis();
    	
    	while (System.currentTimeMillis() < start+maxWaitTimeMillis) {
    		try {
				//System.out.println(DateTime.currentTime()+"NDNInsertResolver - Write on Socket START");
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				dataOutputStream.write(value);
				dataOutputStream.flush();
				//System.out.println(DateTime.currentTime()+"NDNInsertResolver - Write on Socket END"+(System.currentTimeMillis()-start));
				return;
			} 
			catch (Exception e) {
				System.out.println(DateTime.currentTime() +"NDNInsertResolver Exception - sendOnTCP - " + e.getMessage());
				socket = repoMap.checkAndFixSocketConnectionError(socket);
				
				try { Thread.sleep(500); } 
				catch (InterruptedException e1) {}
				
				continue;
			}
    	}
	}
    
    
    private static String createZipFile(String content) {
    	
    	try {

			String fileName = Utils.generateRandomName(20) ;
			String filePath = FileManager.UPLOAD_DIR + "/" + fileName+".gz";
			String URL      = NetManager.FES_IP+":"+NetManager.FES_PORT+"/OGB/files/gz/download/" + fileName;
			
//			System.err.println("ZIP file path: " + filePath);
			FileOutputStream f = new FileOutputStream(filePath);
			Writer writer = new OutputStreamWriter(new GZIPOutputStream(f), "UTF-8");
			writer.write(content);
			writer.close();	
			
			return URL;
		} 
    	catch (IOException e) {
			e.printStackTrace();
		}	
    	
    	return null;
    }
  
    
    private static void sendInsertInterest(String zipURL)
    {
    	Name name = new Name (repoMap.getFirstRepoName()).append("INSERT_REQUEST").append(zipURL);
    	//System.out.println("Name: "+ name.toUri());
    	
    	Face face = new Face(NDNFetchManager.serverIP);
		Interest requestIndex = new Interest(name);
		requestIndex.setMustBeFresh(true);
		requestIndex.setInterestLifetimeMilliseconds(10000);
		try{
			face.expressInterest(requestIndex, new OnData() {
				public void onData(Interest interest, Data data) {
					System.out.println("recieved data");
					isInFlight = false;
				}

			}, new OnTimeout() {
				public void onTimeout(Interest interest) {
					isInFlight = false;
					System.out.println("Timeout for interest " + interest.toUri());
					System.out.println(DateTime.currentTime()+"NDNPrefixUpdater - ERROR ON RETRIEVING REPO PREFIXES. Restoring last valid version");
				}
			});

			while(isInFlight)
			{
				face.processEvents();
				Thread.sleep(1);
			}

		}
		catch(Exception e){
			e.printStackTrace();
		}

    }

    @SuppressWarnings("deprecation")
	public static void HttpInsertion(ArrayList<NDNContentObject> contentObjectList, String content)
    {	
    	try {
	        ObjectMapper mapper = new ObjectMapper();
	        
	        ObjectNode geoJSONObject = mapper.createObjectNode();
	        geoJSONObject.put("content", content);
	        
	        ArrayNode contentObjectsArray = mapper.createArrayNode();
	        for (NDNContentObject segment : contentObjectList)
	        {
//                         System.out.println("HTTP - unsigned segment "+new String(segment.getUnsignedContent()));
// 	        	System.out.println("HTTP - segment "+new String(segment.getSignedContent()));
	        	contentObjectsArray.add(segment.getSignedContent());
	        }
	        geoJSONObject.put("segments", contentObjectsArray);
// 	        System.out.println("json len: "+new String(mapper.writeValueAsString(geoJSONObject)).length());
	        
// 	        System.out.println(new String(mapper.writeValueAsString(geoJSONObject)));   
                String URL = createZipFile(mapper.writeValueAsString(geoJSONObject));

                sendInsertInterest(URL);			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
    	
}
