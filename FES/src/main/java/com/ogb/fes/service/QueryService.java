package com.ogb.fes.service;


import java.io.OutputStream;
import java.util.ArrayList;
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
import com.ogb.fes.execution.ExecutionMonitor;
import com.ogb.fes.execution.ExecutionTask;
import com.ogb.fes.execution.ExecutionTaskQuery;
import com.ogb.fes.execution.ExecutionTaskQueryElement;
import com.ogb.fes.net.NetManager;


@RestController
public class QueryService {

	@Autowired
	private UserRepository userRepo;
	
	ExecutionMonitor executionMonitor = ExecutionMonitor.sharedInstance();
	
	
	@SuppressWarnings("unchecked")
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/query-service/element/{cid}", produces="application/json")
	public void postElementQueryRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String cid, @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws Exception {
		
		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			byte resp[] = "{\"message\":\"Invalid authorization token\"}".getBytes();
			out.write(resp, 0, resp.length);
			out.flush();
			out.close();
			return;
		}
		
		HashMap<String, Object> queryParams = new HashMap<String, Object>();
		queryParams.put("content", params);
		
		ExecutionTask queryRunnable = new ExecutionTaskQueryElement(queryParams, user);
		executionMonitor.addRunnable(queryRunnable).get();
		Object result = queryRunnable.getResult();
		StringBuilder resString = new StringBuilder();
		if (result instanceof ArrayList) {
			resString.append("[");
			for (Object entry : (ArrayList<Object>)result) {
				resString.append((String)entry.toString());
			}
			resString.append("]");
		}
		System.out.println(queryRunnable.getStats());
		
//		byte resp[] = new ObjectMapper().writeValueAsBytes(result);
		byte resp[] = resString.toString().getBytes();
		out.write(resp, 0, resp.length);
		out.flush();
		out.close();
		return;
	}

	@CrossOrigin(origins="*")
	@RequestMapping(method=RequestMethod.POST, value="/OGB/query-service/{did}", consumes={"application/json"}, produces={"application/json"})
	public void postRangeQueryRequest(HttpServletResponse response, @RequestBody HashMap<String, Object> params, @PathVariable String did, @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws Exception {
		
		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			byte resp[] = "{\"message\":\"Invalid authorization token\"}".getBytes();
			out.write(resp, 0, resp.length);
			out.flush();
			out.close();
			return;
		}
		
		//for (String name: params.keySet()){
            	//	String key =name.toString();
            	//	String value = params.get(name).toString();  
            	//	System.out.println(key + " = " + value);
		//	value = value.replace("http://","http://");
		//	System.out.println(key + " = " + value);  
		//}

			
		HashMap<String, Object> queryParams = new HashMap<String, Object>();
		queryParams.put("content", params);
		queryParams.put("did", did);
		queryParams.put("tid", user.getUserID().split("/")[0]);
		
	//	for (String name: queryParams.keySet()){
        //                String key =name.toString();
        //                String value = queryParams.get(name).toString();
        //                System.out.println(key + " = " + value);
	//	}

			
		ExecutionTask queryRunnable = new ExecutionTaskQuery(queryParams, user);
		executionMonitor.addRunnable(queryRunnable).get();

		String result = (String)queryRunnable.getResult();

// 		System.out.println("result len: "+result.length());
		System.out.println(queryRunnable.getStats());
		
		byte resp[] = result.getBytes();
		out.write(resp, 0, resp.length);
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
