package com.ogb.fes.execution;


import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ogb.fes.domain.User;
import com.ogb.fes.ndn.NDNRepoMap;
import com.ogb.fes.ndn.insert.NDNContentObject;
import com.ogb.fes.ndn.insert.NDNInsertResolver;
import com.ogb.fes.ndn.insert.NDNContentObject.NDNMaxPacketSizeException;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.Utils;

import net.named_data.jndn.Name;


public class ExecutionTaskInsert extends ExecutionTask {

	private String repoPrefix;
	private String objectName;


	public ExecutionTaskInsert(HashMap<String, Object> params, User user) {
		super(params, user);

		//Call the insert sharding function and the insert naming function for populate the list
		repoPrefix= NDNRepoMap.sharedInstance().getRepoPrefix().get(0);
		objectName=insertObjectName();
		//		filterNaming();
		//repoNaming();
	}

	@Override
	public void run() {

		try { 
			long start, stop;

			start = System.currentTimeMillis();

			//System.out.println(DateTime.currentTime() + "InsertTask content params: " + executionParamsContentString);
			HashMap<String, Object> contentMap = new HashMap<String, Object>();
			contentMap.put("content",   executionParamsContent); // Geojson
			contentMap.put("reference", objectName); // Object name
			String content = new ObjectMapper().writeValueAsString(contentMap);

			ArrayList<NDNContentObject> contentObjectList = new ArrayList<NDNContentObject>();
			//for (int i = 0; i < objectName.size(); i++) {
			//System.out.println(DateTime.currentTime() + "ContentObjectName: " + insertNaming.get(i));
			executionParamsContentString = executionParamsContentString.substring(0, executionParamsContentString.length()-1);
			executionParamsContentString += ", \"oid\": \"" +objectName+"\"}";
			NDNContentObject bigContentObject = new NDNContentObject(executionParamsContentString, objectName, currentUser, true);
                                        
			//This content object can be grater than NDN_MAX_PACKET_SIZE 
			//so we split it into a list of content object
			contentObjectList.addAll(bigContentObject.fragmentAtMaxNDNPacketSize());

			if (contentObjectList.size()==1)
			{
				//System.out.println(DateTime.currentTime() + "ExecutionTaskInsert - tcpBulkInsertion");
				NDNInsertResolver.tcpBulkInsertion(contentObjectList.get(0));
			}
			else if (contentObjectList.size()>1)
			{
				//System.out.println(DateTime.currentTime() + "ExecutionTaskInsert - HttpInsertion");
				NDNInsertResolver.HttpInsertion(contentObjectList, content);
			}
			else
				System.out.println(DateTime.currentTime() + "ExecutionTaskInsert - ERROR: empty content object list");

			stop = System.currentTimeMillis();
			stats.setNdnRequestTime(stop-start);
			//System.out.println(DateTime.currentTime() + "ExecutionTask - Total Insert Time: " + (stop-start)+"ms");
		} 
		catch(Exception e) {
			System.out.println(DateTime.currentTime() + "ExecutionTaskInsert - Exception:\n");
			e.printStackTrace();
		}
	}


	@Override
	public Object getResult() {

		return objectName;
	}



	private String insertObjectName() {
		String nonce = Utils.generateNonce(10);
		String did     = (String)executionParams.get("did");
		String name    = repoPrefix + "/" + did + "/" + currentUser.getUserID() + "/" + nonce;
		Name   ndnName = new Name(name);
		ndnName.appendSegment(0);
		return ndnName.toUri();
	}

}
