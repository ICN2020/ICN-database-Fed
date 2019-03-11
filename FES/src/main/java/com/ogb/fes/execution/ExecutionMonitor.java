 package com.ogb.fes.execution;


import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ogb.fes.utils.DateTime;


public class ExecutionMonitor {
	public enum ExecutionType {
		QUERY,
		INSERT,
		DELETE,
	}
	
	
	int maxExecutionThread = 16;
	int waitQueueSize      = 1000;
	int keepAliveTime      = 60; //Time in seconds
	
	LinkedBlockingQueue<Runnable> waitQueue;
	ThreadPoolExecutor            executor;
	
	static ExecutionMonitor       sharedInstance;
	
	
	private ExecutionMonitor() {
		
		waitQueue = new LinkedBlockingQueue<Runnable>(waitQueueSize);
		executor  = new ThreadPoolExecutor(maxExecutionThread, maxExecutionThread, keepAliveTime, TimeUnit.SECONDS, waitQueue);
		
		executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				System.out.println(DateTime.currentTime() + "ExecutionMonitor - Work rejected... waiting 15 millis and retry");
				
				try { Thread.sleep(15); } catch (Exception e) {}
				
				executor.execute(r);
			}
		});	
	}
	
	public static ExecutionMonitor sharedInstance() {
		if (sharedInstance == null)
			sharedInstance = new ExecutionMonitor();
		
		return sharedInstance;
	}
	
	
	public Future<?> addRunnable(ExecutionTask runnable) throws InterruptedException {
		return executor.submit(runnable);
	}
}
