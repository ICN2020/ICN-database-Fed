package com.ogb.fes.ndn;


import java.io.IOException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
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


public class NDNQueryRepoMap {

	public class NDNQueryPrefixResolver extends Thread {
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
		public NDNQueryPrefixResolver() {
			super();

			face    = new Face();
			started = false;
		}

		public synchronized JSONArray resolveRepoNames() {    	
			try {
				NDNPrefixResolverCallback prefixResolverCallback = new NDNPrefixResolverCallback();

				Name ogbIpRes = new Name("OGB/GET_INDEX");

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
	private static NDNQueryRepoMap sharedInstance;

	private NDNQueryPrefixResolver ndnQueryPrefixResolver;
	private HashMap<Name, ArrayList<Name>>  queryRepoMap;		//map: prefix , list of repo_name
	private HashMap<String, Integer> repoVersionMap;
	
	public HashMap<String, RTree<String, Rectangle>> rtreeMap;
	
	private NDNQueryRepoMap() {
		super();

		queryRepoMap    = new HashMap<Name, ArrayList<Name>>();
		repoVersionMap  = new HashMap<String, Integer>();
		rtreeMap 		= new HashMap<String, RTree<String, Rectangle>> ();
		
		//Allocate and start the NDN Prefix Resolver Thread
		ndnQueryPrefixResolver = new NDNQueryPrefixResolver();
		ndnQueryPrefixResolver.start();
	}

	public static NDNQueryRepoMap sharedInstance() {
		if (sharedInstance == null)
			sharedInstance = new NDNQueryRepoMap();

		return sharedInstance;
	}

	public ArrayList<String> filterNameList(ArrayList<String> names) {

		HashSet<String> filterMap = new HashSet<String>();
		long start = 0;
		start = System.currentTimeMillis();
		for (String name : names) {

			Name toCheck = new Name(name);
			ArrayList<Name> repoNames = containPrefix(toCheck);
			String repoPrefix = null;
			if(repoNames.size()>0){
				for (Name repoName : repoNames){
					repoPrefix = repoName.toUri();
//					if (filterMap.get(repoPrefix) == null)
					filterMap.add(repoPrefix);
				}
			}
		}
		System.out.println("Total lookup time: "+(System.currentTimeMillis()-start)+" ms");
		return new ArrayList<String>(filterMap);
	}

	@SuppressWarnings("unchecked")
	public void updateQueryRepoMap(String response) throws JsonParseException, JsonMappingException, IOException
	{
			HashMap<String, Object> repoResponse = new ObjectMapper().readValue(response, HashMap.class);

//			for (Map.Entry<String, Object> entry : repoResponse.entrySet()) {
//				String key = entry.getKey();
//				Object value = entry.getValue();
//				System.out.println(key + " = " +value);
//			}
			
			
//			System.out.println("------ MAP BEFORE UPDATE ------");
//			for (Map.Entry<Name, ArrayList<Name>> entry : queryRepoMap.entrySet()) {
//				Name key = entry.getKey();
//				System.out.print(key.toUri()+" = ");
//				ArrayList<Name> value = entry.getValue();
//				for (Name repo : value)
//					System.out.print(repo.toUri()+" ");
//				System.out.println("");
//			}
			
			Name repoName = new Name((String)(repoResponse.get("repo_name")));
			
			
			
			ArrayList<String> prefixes = (ArrayList<String>) repoResponse.get("prefix");
			
			updateRtree(repoName.toUri(),prefixes);
			
			if (true) return;
			
			//first remove all entries to <repoName>
			HashMap<Name, ArrayList<Name>> toUpdateList = new HashMap<Name, ArrayList<Name>>();
			ArrayList<Name> toDeleteList = new ArrayList<Name>();
			
			for (Map.Entry<Name, ArrayList<Name>> entry : queryRepoMap.entrySet()){
				ArrayList<Name> repoToUpdate = new ArrayList<>(entry.getValue()); //list of repo Names matching a given tile prefix
				for (Name repo : entry.getValue()){
					if (repo.toUri().equals(repoName.toUri()))
					{
						repoToUpdate.remove(repo);
						if (repoToUpdate.size()<1)
							toDeleteList.add(entry.getKey());
						else toUpdateList.put(entry.getKey(), repoToUpdate);
					}
				}
			}
			for (Name deleteName : toDeleteList)
				queryRepoMap.remove(deleteName);
			queryRepoMap.putAll(toUpdateList);
			
			// now the old queqyRepoMap no more contains entries for current updating repo (repoName.toUri()) 
			
			
			//now put all new prefixes of repoName.toUri() in the map
			
			for (String prefixString : prefixes)
			{
				Name prefix = new Name(prefixString);
				ArrayList<Name> repoNamesToInsert = new ArrayList<Name>();
				if (queryRepoMap.get(prefix)==null)
				{
					repoNamesToInsert.add(repoName);
					queryRepoMap.put(prefix, repoNamesToInsert);
				}
				else
				{
					repoNamesToInsert = queryRepoMap.get(prefix);
					repoNamesToInsert.add(repoName);
					queryRepoMap.put(prefix, repoNamesToInsert);
				}
				System.out.println("Prefix: "+prefix);
			}
				
			updateRtree(repoName.toUri(),prefixes);
//			System.out.println("------ MAP AFTER UPDATE ------");
//			for (Map.Entry<Name, ArrayList<Name>> entry : queryRepoMap.entrySet()) {
//				Name key = entry.getKey();
//				System.out.print(key.toUri()+" = ");
//				ArrayList<Name> value = entry.getValue();
//				for (Name repo : value)
//					System.out.print(repo.toUri()+" ");
//				System.out.println("");
//			}
	}
	
	public void updateRtree (String repoName, ArrayList<String> prefixes) {
		rtreeMap.remove(repoName); // clean repoNAme entries in the rtree map
		RTree<String, Rectangle> rtree = RTree.create();	
		double swLon=0;
		double swLat=0;
		double neLon=0;
		double neLat=0;
		for (String prefixString : prefixes)
		{
			//Name prefix = new Name(prefixString);
			if(!prefixString.startsWith("/"))
				prefixString="/"+prefixString;
			String[] components = prefixString.split("/");
			if (components.length==5) {
				swLon = Double.parseDouble(components[1]+"."+components[3].substring(0, 1)+components[4].substring(0, 1));
				swLat = Double.parseDouble(components[2]+"."+components[3].substring(1, 2)+components[4].substring(1, 2));
				neLon = swLon+0.01;
				neLat = swLat+0.01;
			} else if (components.length==4) {
				swLon = Double.parseDouble(components[1]+"."+components[3].substring(0, 1));
				swLat = Double.parseDouble(components[2]+"."+components[3].substring(1, 2));
				neLon = swLon+0.1;
				neLat = swLat+0.1;
			} else if (components.length==3) {
				swLon = Double.parseDouble(components[1]);
				swLat = Double.parseDouble(components[2]);
				neLon = swLon+1.0;
				neLat = swLat+1.0;
			}
			Rectangle rectangle = Geometries.rectangleGeographic(swLon, swLat, neLon, neLat);
			rtree=rtree.add(repoName,rectangle);
		}
		rtreeMap.put(repoName,rtree);
		//System.out.println("Rtree size: "+rtree.size());
	}

	public ArrayList<String> getRepoPrefix()
	{
		ArrayList<String> prefixes = new ArrayList<String>();
		for (Map.Entry<Name, ArrayList<Name>> entry : queryRepoMap.entrySet()){
			prefixes.add(entry.getKey().toUri());
		}
		return prefixes;
	}


	public void queryRepoMapUpdater(Name prefix, Name repoName)
	{
		
		
	}
	
	
	public boolean updateVersion (String repoID, int newVersion)
	{
		boolean isAnUpdate = false;
		Integer oldVersion = repoVersionMap.get(repoID);
		
		if(oldVersion!=null)
			if (oldVersion < newVersion){
				System.out.println("update for "+repoID+": oldVersion="+oldVersion+" - newVersion="+newVersion);
				isAnUpdate = true;
				repoVersionMap.put(repoID, newVersion);
			}
			else
				;//System.out.println("nothing to update for "+repoID+": oldVersion="+oldVersion+" - newVersion="+newVersion);
		else
		{
			System.out.println("new entry for "+repoID+", version"+newVersion);
			isAnUpdate = true;
			repoVersionMap.put(repoID, newVersion);
		}
		return isAnUpdate;
	}
	
	public void restoreVersion (String repoID)
	{
		int versionToRestore = repoVersionMap.get(repoID) - 1;
		repoVersionMap.put(repoID, versionToRestore);
	}

	public synchronized ArrayList<Name> containPrefix(Name name) {
                // longest prefix matching
		int  matchSize   = -1;
		ArrayList<Name> repoName    = new ArrayList<>();
		for (Entry<Name,ArrayList<Name>> entry : queryRepoMap.entrySet()) {
			Name tilePrefix = new Name(entry.getKey());
			if (tilePrefix.match(name)) {
//                         if (repoPrefix.isPrefixOf(name)) {
				if (tilePrefix.size() > matchSize) {
					matchSize = tilePrefix.size();
					repoName  = entry.getValue();
// 					System.out.println("match: "+name.toUri()+" - "+repoPrefix.toUri()+" -> "+repoName);
				}
			}
		}
		return repoName;
	}	
}
