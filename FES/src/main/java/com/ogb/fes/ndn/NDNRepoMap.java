package com.ogb.fes.ndn;


import java.io.File;
import java.io.IOException;

import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.policy.ConfigPolicyManager;


public class NDNRepoMap {
	
	public class NDNPrefixResolver extends Thread {
		public class NDNPrefixResolverCallback implements OnData, OnTimeout {
			
			String  result     = "";
			boolean isComplete = false;
			
			
			public NDNPrefixResolverCallback() {
				super();
				
				isComplete = false;
			}
			
			@Override
			public void onData(Interest arg0, Data data) {		
				ByteBuffer content = data.getContent().buf();
				byte       bytes[] = new byte[content.limit() - content.position()];
				
				for (int i = content.position(); i < content.limit(); i++) {
					bytes[i] = content.get(i);
				}
				
				result     = new String(bytes);
				isComplete = true;
			}
			 
			@Override
			public void onTimeout(Interest arg0) {
				isComplete = false;
				result     = null;
			}
			
			public boolean isCompleted() {
				return isComplete;
			}
			 
			public String getResult() {
				try {
					while (!isComplete) {
						Thread.sleep(10);
					}
				}
				catch(Exception e) {
				}
				
				return result;
			}
		}
		
		//Private attributes
		private Face    face;
		private boolean started; 

		
		//Constructor
		public NDNPrefixResolver() {
			super();
			
			face    = new Face();
			started = false;
		}
		
		public synchronized JSONArray resolvePrefix(Name prefix) {    	
	    	try {
				NDNPrefixResolverCallback prefixResolverCallback = new NDNPrefixResolverCallback();
				
				Name ogbIpRes = new Name("OGB/IP_RES");	// temporary patch
				
				int gpsTag = -1;
				for (int i=0; i<prefix.size(); i++) {
					if (prefix.get(i).toEscapedString().equals("GPS_id")){
						gpsTag = i;
						break;
					}
				}
				ogbIpRes = new Name(prefix.getSubName(0, gpsTag+1).append("IP_RES"));

				
		    	Interest interest = new Interest(new Name(ogbIpRes));
				interest.setInterestLifetimeMilliseconds(500);
				interest.setMustBeFresh(true);
				
				ConfigPolicyManager policyManager   = new ConfigPolicyManager();
				IdentityManager     identityManager = new IdentityManager();
				KeyChain            keyChain        = new KeyChain(identityManager, policyManager);
				keyChain.setFace(face);
				keyChain.sign(interest, keyChain.getDefaultCertificateName());
				
				face.expressInterest(interest, prefixResolverCallback, prefixResolverCallback);
				
				//GetResult is blocking until one of the callback is called
				return new JSONArray(prefixResolverCallback.getResult());
			}
	    	catch (Exception e) {
	    		return null;
	    	}
	    }
		
		public synchronized boolean isStarted() {
			return started;
		}
		
		public synchronized void stopResolver() {
			started = false;
		}
		
		@Override
		public void run() {
			super.run();
			
			started = true;
			
			System.out.println(DateTime.currentTime()+"NDNPrefixResolver - Starting NDN resolver deamon...");
			while (started) {
				
				try {
					face.processEvents();
					Thread.sleep(1);
				} 
				catch (Exception e) {
				}
			}
			
			System.out.println(DateTime.currentTime()+"NDNPrefixResolver - NDN resolver deamon stopped!");
		}
	}
	
	
	//Private Attributes
	private static NDNRepoMap sharedInstance;
	
	private NDNPrefixResolver       ndnPrefixResolver;
	private HashSet<String>         repoPrefix;
	private HashMap<String, Socket> repoMap;
	private HashMap<String, String> repoMapIP;
	

	private NDNRepoMap() {
		super();
		
		repoMap    = new HashMap<String, Socket>();
		repoMapIP  = new HashMap<String, String>();
		repoPrefix = new HashSet<String>();
		
		File configFile = new File(FileManager.CONFIG_DIR+"/IP-RES.conf");
		if (configFile.exists())
			loadFromConfigFile(configFile);
		
		//Allocate and start the NDN Prefix Resolver Thread
		ndnPrefixResolver = new NDNPrefixResolver();
		ndnPrefixResolver.start();
	}

    private void loadFromConfigFile(File config) {
    	repoMap = new HashMap<String, Socket>();
    	String ip   = "";
		int    port = 0;
		
    	try {
    		byte[]    encoded       = Files.readAllBytes(Paths.get(config.getAbsolutePath()));
    		String    configuration = new String(encoded).replace("/n", "");
    		JSONArray loadedIPRes   = new JSONArray(configuration);
    		
    		for (int i = 0; i<loadedIPRes.length(); i++){
    			JSONObject elem        = loadedIPRes.getJSONObject(i);
	    		String     prefix      = elem.getString("prefix");
	    				   ip          = elem.getString("ip");
	    				   port        = elem.getInt("port");
	    		boolean    socketExist = false; 
	    		Socket     socket      = null;
	    		

	    		System.out.println(DateTime.currentTime()+"NDNRepoMap - prefix: "+prefix+" -> "+ip+":"+port);
	    		for (String key : repoMap.keySet()) {
					socket = repoMap.get(key);
					if (socket.getInetAddress().getHostAddress().equals(ip)) {
						socketExist = true;
						System.out.println(DateTime.currentTime()+"NDNRepoMap - Socket Exist");
						break;
					}
	    		}
	    		
	    		if (socketExist == false) 
	    			socket = new Socket(ip, port);
	    		
	    		repoMap.put(prefix, socket);
	    		repoMapIP.put(prefix, ip+"::"+port);
	    		repoPrefix.add(prefix);
    		}
    	}
    	catch (ConnectException connEx) {
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - IP: "+ip + ":"+port);
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - Try to reload config file...");
    		
    		checkAndFixSocketConnectionError();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
	}
    
    private void checkAndFixSocketConnectionError() {
    	String ip   = "";
    	int    port = 0;
    	
    	try {
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - Sockets count " + repoMap.keySet().size());
			
    		if (repoMap.keySet().size() == 0) {
    			File configFile = new File(FileManager.CONFIG_DIR+"/IP-RES.conf");
    			if (configFile.exists()) {
    				try { Thread.sleep(500); } 
    	    		catch (InterruptedException e) {}
    				
    				loadFromConfigFile(configFile);
    			}
    			return;
    		}
    		
	    	for (String key : repoMap.keySet()) {
				Socket s = repoMap.get(key);
				
				if (s.isClosed() == true || s.isConnected() == false) {
					ip   = repoMapIP.get(key).split("::")[0];
				    port = Integer.parseInt(repoMapIP.get(key).split("::")[1]);
				    
					repoMap.put(key, new Socket(ip, port));
					repoPrefix.add(key);
					
					System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - IP: "+ip + ":"+port);
					System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - Try to reconnecting to " + ip + "...\n");
				}
			}
    	}
    	catch (ConnectException exc) {
    		try { Thread.sleep(500); } 
    		catch (InterruptedException e) {}
    		
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - IP: "+ip + ":"+port);
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - Try to reconnecting...\n");
    		
    		checkAndFixSocketConnectionError();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public Socket checkAndFixSocketConnectionError(Socket socket) {
    	String ip   = "";
    	int    port = 0;
    	
    	try {
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - Broken Socket");
			
    		if (repoMap.keySet().size() == 0) {
    			File configFile = new File(FileManager.CONFIG_DIR+"/IP-RES.conf");
    			if (configFile.exists()) {
    				try { Thread.sleep(500); } 
    	    		catch (InterruptedException e) {}
    				
    				loadFromConfigFile(configFile);
    			}
    			return socket;
    		}
    		
	    	for (String key : repoMap.keySet()) {
				Socket s = repoMap.get(key);
				
				if (s == socket) {
					ip   = repoMapIP.get(key).split("::")[0];
					port = Integer.parseInt(repoMapIP.get(key).split("::")[1]);
			
					s.close();
					try { Thread.sleep(500); }
			        catch (InterruptedException e) {}
					
					socket = new Socket(ip, port);
					repoMap.put(key, new Socket(ip, port));
					repoPrefix.add(key);
					
					System.out.println(DateTime.currentTime()+"NDNRepoMap - Broken Socket - IP: "+ip + ":"+port);
					System.out.println(DateTime.currentTime()+"NDNRepoMap - Broken Socket - Try to reconnecting to " + ip + "...\n");
				}
	    	}
	    	
	    	return socket;
    	}
    	catch (ConnectException exc) {
    		try { Thread.sleep(500); } 
    		catch (InterruptedException e) {}
    		
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - Broken Socket - IP: "+ip + ":"+port);
			System.out.println(DateTime.currentTime()+"NDNRepoMap - Broken Soket - Try to reconnecting to " + ip + "...\n");
    		
    		return checkAndFixSocketConnectionError(socket);
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return socket;
    }

	public static NDNRepoMap sharedInstance() {
    	if (sharedInstance == null)
    		sharedInstance = new NDNRepoMap();
    	
    	return sharedInstance;
    }
	
	public ArrayList<String> getRepoPrefix() {
	
		return new ArrayList<String>(repoMap.keySet());
	}
	
	public ArrayList<String> filterNameList(ArrayList<String> names) {
		
		HashMap<String, String> filterMap = new HashMap<String, String>();
		for (String name : names) {
			String repoPrefix = containPrefix(name);
		
			if (repoPrefix != null) {
				if (filterMap.get(repoPrefix) == null)
					filterMap.put(repoPrefix, name);
			}
		}
		
		return new ArrayList<String>(filterMap.values());
	}
    
    public synchronized Socket containPrefix(Name name) {
    	// longest prefix matching
    	int    matchSize = -1;
    	Socket socket    = null;
    	
    	for (Entry<String,Socket> entry : repoMap.entrySet()) {
    		Name repoPrefix = new Name(entry.getKey());
    		if (repoPrefix.match(name)) {
				if (repoPrefix.size() > matchSize) {
					matchSize = repoPrefix.size();
					socket    = entry.getValue();
				}
			}
    	}
    	
    	return socket;
    }
    
    public synchronized Socket getFirstSocket() {
    	Map.Entry<String, Socket> entry = repoMap.entrySet().iterator().next();
    	return entry.getValue();
    }
    
    public synchronized String getFirstRepoName() {
    	Map.Entry<String, Socket> entry = repoMap.entrySet().iterator().next();
    	return entry.getKey();
    }
    
    public synchronized String containPrefix(String name) {
    	
    	String[] component = name.split("/");
    	String   prefix    = "";
    	
    	if (repoPrefix.contains("/"+ component[1] + "/" + component[2] + "/")) {
    		prefix = "/"+ component[1] + "/" + component[2] + "/";
    		return repoMap.get(prefix).getInetAddress().toString();
    	}
    	if (repoPrefix.contains("/"+ component[1] + "/")) {
    		prefix = "/"+ component[1] + "/";
    		return repoMap.get(prefix).getInetAddress().toString();
    	}
    	if (repoPrefix.contains("/")) {
    		prefix = "/";
    		return repoMap.get(prefix).getInetAddress().toString();
    	}
    	
    	System.out.println("NAME NOT FOUND: " + name);
    	return null;

    	
    	
    	/*
    	String longestPrefix = null;
    	
    	for (Entry<String,Socket> entry : repoMap.entrySet()) {
    		Name repoPrefix = new Name(entry.getKey());
    		if (repoPrefix.isPrefixOf(new Name(name))) {
    			longestPrefix = entry.getValue().getInetAddress().toString();
			}
    	}
    	
    	return longestPrefix;
    	*/
    }

    
    public Socket getRepoSocket(Name name) {
    	//First check if prefix is resolved (contained in repoMap)
    	Socket socket = containPrefix(name);
    	if (socket != null && socket.isConnected()) {
    		return socket;
    	}
    	else {
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - Try to reconnecting...\n");
    		
    		checkAndFixSocketConnectionError();
    	}
    	
     	//Prefix not found in repoMap. 
     	//Resolving the prefix with ndnPrefixResolver daemon. 
    	//Waiting until the daemon is started...
    	while (ndnPrefixResolver.isStarted() == false) {
    		try { Thread.sleep(5); }
    		catch (Exception e) { }
    	}
    	
    	JSONArray prefixList = ndnPrefixResolver.resolvePrefix(name);
    	try {
	    	for (int i = 0; i < prefixList.length(); i++) {
	    		JSONObject elem   = prefixList.getJSONObject(i);
	    		String     prefix = elem.getString("prefix");
	    		String     ip     = elem.getString("ip");
	    		int        port   = elem.getInt("port");
	    		
	    		System.out.println(DateTime.currentTime()+"NDNRepoMap - prefix: "+prefix+" -> "+ip+":"+port);
	    		Socket s = new Socket(ip, port);
	    		repoMap.put(prefix, s);
	    		repoPrefix.add(prefix);
	    	}
    	}
    	catch (UnknownHostException exc) {
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - Try to reconnecting...");
    		checkAndFixSocketConnectionError();
    	}
    	catch (IOException e) {
    		System.out.println(DateTime.currentTime()+"NDNRepoMap - ConnectionException! - Try to reconnecting...");
    		checkAndFixSocketConnectionError();
    	}
 
    	
    	return containPrefix(name);
    }
}
