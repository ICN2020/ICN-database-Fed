package com.ogb.fes.ndn;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ogb.fes.ndn.query.NDNFetchManager;
import com.ogb.fes.utils.DateTime;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;

public class NDNQueryRepoMapManager 
{
	static long startTime, stopTime = -10000;
	
	
	public class NDNPrefixUpdater extends Thread {
		
		public class NDNVersionListenerCallback implements OnInterestCallback, OnRegisterFailed, OnRegisterSuccess
		{

			@Override
			public void onInterest(Name arg0, Interest arg1, Face arg2, long arg3, InterestFilter arg4) {
				startTime = System.nanoTime();
				Name interestName = arg1.getName();
				if(interestName.size()<3 || !interestName.toUri().contains("version"))
					{
						System.out.println("invalid interest : "+interestName);
						return;
					}
//				System.out.println("Received Interest: "+interestName+ "    -    #components: "+interestName.size());
				String repoID = interestName.get(1).toEscapedString();
				int repoVersion = Integer.parseInt(interestName.get(2).toEscapedString().split("version")[1]);
//				System.out.println("repoName : "+repoID+" - repoVersion: "+repoVersion);
				
				newVersion = queryRepoMap.updateVersion(repoID, repoVersion);
				
				if (newVersion) //retrieving list of prefix from repo
					getPrefixes(repoID);
				
				
			}

			@Override
			public void onRegisterFailed(Name arg0) {
				System.out.println("NDNVersionListenerCallback - Error on registration for " + arg0.toUri());	
			}

			@Override
			public void onRegisterSuccess(Name arg0, long arg1) {
				System.out.println("NDNVersionListenerCallback - Registration Success for " + arg0.toUri());	
			}
			
			
		}
		
		//Private attributes
		private Face    face;
		private boolean started;
		private KeyChain keyChain;
		private boolean newVersion;
		private NDNQueryRepoMap queryRepoMap;
		
		//Constructor
		public NDNPrefixUpdater() {
			super();
			
			face    = new Face(NDNFetchManager.serverIP);
			started = false;
			newVersion = false;
			queryRepoMap = NDNQueryRepoMap.sharedInstance();
			
			try {
				keyChain = buildTestKeyChain();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			
			startListener();
			
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
						
			
		}
		
		public void startListener()
		{
			System.out.println(DateTime.currentTime()+"NDNPrefixUpdater - Starting NDN updater deamon...");
			try {
				NDNVersionListenerCallback listenerCallback = new NDNVersionListenerCallback();

				Name filterName = new Name("/FES/");

				face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
				face.registerPrefix(filterName, listenerCallback, listenerCallback);

				started=true;
			}
			catch (Exception e) {
				e.printStackTrace();
				started=false;
				return;
			}

			while (started) {
				try {
					face.processEvents();
					Thread.sleep(1);
				} 
				catch (Exception e) {
					return;
				}
			}
			System.out.println(DateTime.currentTime()+"NDNPrefixUpdater - NDN updater deamon stopped!");
		}
	
		public void getPrefixes(String repoID)
		{
			Name requestIndexName = new Name("/repo/"+repoID+"/GET_PREFIX");

			ArrayList<String>requestIndexNameList = new ArrayList<String>();
			requestIndexNameList.add(requestIndexName.toUri());
			NDNFetchManager fetchManager = new NDNFetchManager();

			String repoResponse = "";
			
			int interestTimeout = 10000;
			ArrayList<Object> result = fetchManager.fetchElement(requestIndexNameList, interestTimeout);

			if (result instanceof ArrayList) {
				StringBuilder resStringB = new StringBuilder();
				for (Object entry : (ArrayList<Object>)result) {
					resStringB.append(entry);
				}
				repoResponse = resStringB.toString();
			}
			try {
				queryRepoMap.updateQueryRepoMap(repoResponse);
				stopTime = System.nanoTime();
				System.out.println("Prefix update time:"+(stopTime-startTime)/1E6+" ms");
			} catch (IOException e) {
				System.out.println(DateTime.currentTime()+"NDNPrefixUpdater - ERROR ON PARSING JSON: " + repoResponse);
				System.out.println("Restoring last valid version");
				queryRepoMap.restoreVersion(repoID);
			}

		}

		
	}

	public void startNDNPrefixUpdater()
	{
		NDNPrefixUpdater updaterThread = new NDNPrefixUpdater();
		updaterThread.start();
	}
	
	public static KeyChain buildTestKeyChain() throws net.named_data.jndn.security.SecurityException {
		MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
		MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
		IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
		KeyChain keyChain = new KeyChain(identityManager);
		try {
			keyChain.getDefaultCertificateName();
		} catch (net.named_data.jndn.security.SecurityException e) {
			keyChain.createIdentityAndCertificate(new Name("/OGB/FES/fes1"));
			keyChain.getIdentityManager().setDefaultIdentity(new Name("/OGB/FES/fes1"));
		}
		return keyChain;
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
}
