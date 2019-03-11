package com.ogb.fes.service;


import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ogb.fes.domain.User;
import com.ogb.fes.domain.UserRepository;
import com.ogb.fes.domain.ErrorResponse;

import com.ogb.fes.execution.ExecutionMonitor;
import com.ogb.fes.execution.ExecutionTask;
import com.ogb.fes.execution.ExecutionTaskDelete;
import com.ogb.fes.execution.ExecutionTaskInsert;

import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;


@RestController
public class ContentService {
	
	@Autowired
	private UserRepository userRepo;
	
	ExecutionMonitor executionMonitor = ExecutionMonitor.sharedInstance();
	
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/content/delete", produces="application/json")
	public void removeContent(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws Exception {
		
		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		long start, stop;

		start = System.currentTimeMillis();
		
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			ErrorResponse error = new ErrorResponse(420, "Invalid authorization token");
			out.write(error.toString().getBytes());
			out.flush();
			out.close();
			return;
		}
		
		String[] oidComponents = ((String)params.get("oid")).split("/");
		String   tid           = user.getUserID().split("/")[0];
		String   uid     	   = user.getUserID().split("/")[1];

		if ( !(oidComponents[4]+"/"+oidComponents[5]).equals(tid+"/"+uid)) {
			
			if (!user.isSuperUser() && !user.isAdmin()) {
				response.setStatus(403);
				String result = "{\"message\": \"User unauthorized!\"}";
				out.write(result.getBytes());
				out.flush();
				out.close();
				return;
			}
		}
		else if (!user.permissionCheck()) {
			response.setStatus(403);
			String result = "{\"message\": \"User unauthorized!\"}";
			out.write(result.getBytes());
			out.flush();
			out.close();
			return;
		}
		
		HashMap<String, Object> deleteParams = new HashMap<String, Object>();
		deleteParams.put("content", params);
		deleteParams.put("oid",     params.get("oid"));
		
		ExecutionTask deleteRunnable = new ExecutionTaskDelete(deleteParams, user);
		executionMonitor.addRunnable(deleteRunnable).get();
		String result = (String)deleteRunnable.getResult();
		
		stop = System.currentTimeMillis();
		System.out.println(DateTime.currentTime()+"ContentService - Delete Time: " + (stop-start) + "ms" );
		
		out.write(result.getBytes());
		out.flush();
		out.close();
		return;
    }
	

	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/content/insert/{did}", produces="application/json")
    public void insertContent(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String did, @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws Exception {
		
		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		long start, stop;

		start = System.currentTimeMillis();
		
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			ErrorResponse error = new ErrorResponse(420, "Invalid authorization token");
			out.write(error.toString().getBytes());
			out.flush();
			out.close();
			return;
		}
		
		if (!user.permissionCheck()) {
			response.setStatus(403);
			String result = "{\"message\": \"User unauthorized!\"}";
			out.write(result.getBytes());
			out.flush();
			out.close();
			return;
		}
		
		HashMap<String, Object> insertParams = new HashMap<String, Object>();
		insertParams.put("content", params);
		insertParams.put("did", did);
		
		ExecutionTask insertRunnable = new ExecutionTaskInsert(insertParams, user);
		executionMonitor.addRunnable(insertRunnable).get();
		String result = (String)insertRunnable.getResult();
		
		result = "{\"oid\": \"" + result + "\"}";

		stop = System.currentTimeMillis();
		System.out.println(DateTime.currentTime()+"ContentService - Total Insert Time: " + (stop-start) + "ms" );
		
		out.write(result.getBytes());
		out.flush();
		out.close();
		return;
    }
	
	
	
	
	
	
	private User checkAuthToken(String authToken) {
		
		if (authToken.length() <= 0)
			return null;
		
		User user = userRepo.findByToken(authToken);
		if (user == null)
			user = checkTokenOnAUCServer(authToken);
		
		return user;
	}
	
	private User checkTokenOnAUCServer(String token) {
		
		Map<String, Object> postParams = new HashMap<String, Object>();
		postParams.put("token", token);
		
		Map<String, Object> aucUser = new NetManager().sendCheckToken(postParams);
		if (aucUser == null)
			return null;
		
		return new User(aucUser);
	} 
}
