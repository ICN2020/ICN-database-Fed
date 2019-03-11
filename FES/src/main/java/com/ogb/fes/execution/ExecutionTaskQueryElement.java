package com.ogb.fes.execution;


import java.util.ArrayList;
import java.util.HashMap;

import com.ogb.fes.domain.User;
import com.ogb.fes.ndn.query.NDNFetchManager;
import com.ogb.fes.utils.DateTime;


public class ExecutionTaskQueryElement extends ExecutionTask {
	
	private ArrayList<String> queryNaming;
	private Object            result;
	
	
	public ExecutionTaskQueryElement(HashMap<String, Object> params, User user) {
		super(params, user);
	
		queryNaming();
	}
	
	
	
	@Override
	public void run() {
		
		try { 
			NDNFetchManager fetchManager = new NDNFetchManager();
			result = fetchManager.fetchElement(queryNaming);
		} 
		catch(Exception e) {
			System.out.println(DateTime.currentTime() + "ExecutionTaskQuery - Exception:\n" + e.getMessage());
		}
	}
	
	@Override
	public Object getResult() {
		
		return result;
	}
	
	
	
	public ArrayList<String> queryNaming() {
		
		queryNaming = new ArrayList<String>();
		
		String oid = (String)executionParamsContent.get("oid");
		if (oid != null)
			queryNaming.add(oid);
		
		return queryNaming;
	}
}

