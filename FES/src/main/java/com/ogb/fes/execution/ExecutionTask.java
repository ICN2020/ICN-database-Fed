package com.ogb.fes.execution;


import java.util.HashMap;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogb.fes.domain.ServiceStats;
import com.ogb.fes.domain.User;
import com.ogb.fes.utils.DateTime;


public class ExecutionTask implements Runnable {
	
	ExecutionMonitor        executionMonitor;
	
	HashMap<String, Object> executionParams;
	String                  executionParamsString;
	
	HashMap<String, Object> executionParamsContent;
	String                  executionParamsContentString;
	
	Future<?>               executionTask;
	User                    currentUser;
	
	ServiceStats            stats;
	
	
	public ExecutionTask() {
		super();
		
		stats = new ServiceStats();
	}
	
	@SuppressWarnings("unchecked")
	public ExecutionTask(HashMap<String, Object> params, User user) {
		this();
		
		long start, stop;
		
		start = System.currentTimeMillis();
		
		executionMonitor       = ExecutionMonitor.sharedInstance();
		executionParams        = params;
		executionParamsContent = (HashMap<String, Object>)params.get("content"); 
		currentUser            = user;
		
		try {
			executionParamsString        = new ObjectMapper().writeValueAsString(executionParams);
			executionParamsContentString = new ObjectMapper().writeValueAsString(executionParamsContent);
		}
		catch(Exception e) {
			executionParamsString = "";
			System.out.println(DateTime.currentTime() + "ExecutionTask - Error while parsing the params hashmap.");
		}
		
		stop = System.currentTimeMillis();
		
		stats.setParsingTime(stop-start);
		//System.out.println(DateTime.currentTime() + "ExecutionTask - Parsing ExecutionParams Time: " + (stop-start) + "ms");
	}
	
	
	


	public ServiceStats getStats() {
		return stats;
	}
	

	@Override
	public void run() {
		
		try { 
			System.out.println(DateTime.currentTime() + "ExecutionTask - Basic implementation. Do nothing and sleep for 100ms...");
			Thread.sleep(100); 
		} 
		catch(Exception e) { 
			System.out.println(DateTime.currentTime() + "ExecutionTask - Exception: "+ e.getMessage());
		}
		
		//isRunning = false;
	}
	
	public Object getResult() {
		
		return new Object();
	}
}
