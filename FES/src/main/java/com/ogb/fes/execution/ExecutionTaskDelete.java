package com.ogb.fes.execution;


import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ogb.fes.domain.User;
import com.ogb.fes.ndn.NDNQueryRepoMap;
import com.ogb.fes.ndn.NDNRepoMap;
import com.ogb.fes.ndn.query.NDNFetchManager;
import com.ogb.fes.tesseler.spatial.SpatialTesseler;
import com.ogb.fes.utils.DateTime;


public class ExecutionTaskDelete extends ExecutionTask {

//	private ArrayList<String> deleteSharding;
	private ArrayList<String> deleteNaming;
	private Object            result;
	
	
	@SuppressWarnings("unchecked")
	public ExecutionTaskDelete(HashMap<String, Object> params, User user) {
		super();
		
		
		currentUser = user;
		
//		try {
//			//Fetch the object with the ExecutionQueryElement
//			ExecutionTaskQueryElement query    = new ExecutionTaskQueryElement(params, user);
//			ExecutionMonitor          executor = ExecutionMonitor.sharedInstance();
//			executor.addRunnable(query).get();
//			Object result = query.getResult();
//			
//			if (result instanceof ArrayList<?>) {
//				String json = ((ArrayList<String>)result).get(0);
//				executionParamsContent       = new ObjectMapper().readValue(json, HashMap.class);
//				executionParamsContentString = (String)((ArrayList<?>)result).get(0);
//				
//				String oid[] = ((String)params.get("oid")).split("/");
//				executionParams = new HashMap<String, Object>();
//				executionParams.put("content", executionParamsContent);
//				executionParams.put("oid",     params.get("oid"));
//				executionParams.put("tid",     user.getUserID().split("/")[0]);
//				executionParams.put("did",     oid[3]);
//				executionParams.put("nonce",   oid[oid.length-2]);
//				executionParams.put("segment", oid[oid.length-1]);
//				executionParamsString = new ObjectMapper().writeValueAsString(executionParams);
//			}
//		}
//		catch(Exception e) {
//			System.out.println(DateTime.currentTime()+"ExcutionTaskDelete - Errore\n" + e.getMessage());
//		}
//		
//		deleteSharding();
//		deleteNaming();
//		filterNaming();
		
		deleteNaming = new ArrayList<String>();
		deleteNaming.add((String) params.get("oid")+"/DELETE");
	}

	
	
	@Override
	public void run() {
		long start, stop;
		
		try { 
			start = System.currentTimeMillis();
			
			NDNFetchManager fetchManager = new NDNFetchManager();
			ArrayList<Object> delRes = fetchManager.deleteElement(deleteNaming);
			result = new ObjectMapper().writeValueAsString(delRes);
			
			stop = System.currentTimeMillis();
			stats.setNdnRequestTime(stop-start);
		} 
		catch(Exception e) {
			System.out.println(DateTime.currentTime() + "ExecutionTaskQuery - Exception:\n" + e.getMessage());
		}
	}
	
	@Override
	public Object getResult() {
		
		return result;
	}
	
	
	/*
	@SuppressWarnings("unchecked")
	private ArrayList<String> deleteSharding() {
		
		long start, stop;
		
		start = System.currentTimeMillis();
		
		HashMap<String, Object> geometry = (HashMap<String, Object>)executionParamsContent.get("geometry");

		String type = (String)geometry.get("type");
		if (type.equalsIgnoreCase("POLYGON") == true) {
			deleteSharding = SpatialTesseler.tesselatePolygon100x100(geometry);
		}
		else if (type.equalsIgnoreCase("POINT") == true) {
			deleteSharding = SpatialTesseler.tesselatePolygon100x100(geometry);
		}
		else if (type.equalsIgnoreCase("MULTIPOINT") == true) {
			deleteSharding = SpatialTesseler.tesselatePolygon100x100(geometry);
		}
		else {
			deleteSharding = new ArrayList<String>();
		}
		
		stop = System.currentTimeMillis();
		
		stats.setTessellingTime(stop-start);
		
		return deleteSharding;
	}
	
	private ArrayList<String> deleteNaming() {
		
		deleteNaming = new ArrayList<String>();
		
		ArrayList<String>queryRepoName = NDNQueryRepoMap.sharedInstance().filterNameList(new ArrayList<>(deleteSharding));

		for (String shardPrefix : queryRepoName) {
			String did     = (String)executionParams.get("did");
			String nonce   = (String)executionParams.get("nonce");
			String segment = (String)executionParams.get("segment");
			String name    = shardPrefix + "/" + did + "/" + currentUser.getUserID() + "/" + nonce + "/" + segment + "/DELETE";
			
			deleteNaming.add(name);
		}
		return deleteNaming;
	}
	
	private ArrayList<String> filterNaming() {
		deleteNaming = NDNQueryRepoMap.sharedInstance().filterNameList(deleteNaming);
		
		return deleteNaming;
	}
	
	*/
}
