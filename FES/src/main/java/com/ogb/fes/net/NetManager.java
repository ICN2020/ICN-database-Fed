package com.ogb.fes.net;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.*;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.boot.json.JacksonJsonParser;

import com.fasterxml.jackson.databind.ObjectMapper;


public class NetManager 
{
	public static String AUC_URL; // configured by FesApplicaiton
	public static String FES_IP; 
	public static String FES_PORT;
	
	public NetManager() {
		
	}
	
	
	public Map<String, Object> sendRegister(Map<String, Object>  postParams) {
		
		HashMap<String, String> headerParams = new HashMap<String, String>();
		headerParams.put("Content-Type", "application/json");
		ObjectMapper objectMapper = new ObjectMapper();
		String postString;
		
		try {
			postString = objectMapper.writeValueAsString(postParams);
			return sendPost(headerParams, postString, "/user/register");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Map<String, Object> sendLogin(Map<String, Object> postParams) {
		
		HashMap<String, String> headerParams = new HashMap<String, String>();
		headerParams.put("Content-Type", "application/json");
		ObjectMapper objectMapper = new ObjectMapper();
		String postString;
		
		try {
			postString = objectMapper.writeValueAsString(postParams);
			return sendPost(headerParams, postString, "/user/login");
		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Map<String, Object> sendCheckToken(Map<String, Object> postParams) {
		
		HashMap<String, String> headerParams = new HashMap<String, String>();
		headerParams.put("Content-Type", "application/json");
		ObjectMapper objectMapper = new ObjectMapper();
		String postString;
		
		try {
			postString = objectMapper.writeValueAsString(postParams);
			return sendPost(headerParams, postString, "/user/check-token");
		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Map<String, Object> sendPost(Map<String, String> headerParams, String postParams, String urlPath) {
		
		try {
			String URL = NetManager.AUC_URL + urlPath;
			//System.out.println("Contacting AUC Server: " + URL);
			HttpURLConnection urlConnection = (HttpURLConnection)new URL(URL).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			
			for (String key : headerParams.keySet())
				urlConnection.setRequestProperty(key, headerParams.get(key));
			
			DataOutputStream streamWriter = new DataOutputStream(urlConnection.getOutputStream());
			streamWriter.writeBytes(postParams.toString());
			streamWriter.flush();
			streamWriter.close();
			
			int responseCode = urlConnection.getResponseCode();
			
			InputStream inputStream;
			if (responseCode >= 200 && responseCode < 300)
				inputStream = urlConnection.getInputStream();
			else
				inputStream = urlConnection.getErrorStream();
			
			StringBuffer   response     = new StringBuffer();
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream));
			
			String inputLine;
			while ( (inputLine = streamReader.readLine()) != null)
				response.append(inputLine);
			streamReader.close();
			
			if (responseCode >= 200 && responseCode < 300) {
				return new JacksonJsonParser().parseMap(response.toString());
			}
			
			return null;
		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	@SuppressWarnings("unused")
	private JSONObject sendGet(Map<String, String> headerParams, JSONObject postParams) {
		
		try {
			HttpURLConnection urlConnection = (HttpURLConnection)new URL(NetManager.AUC_URL).openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			
			for (String key : headerParams.keySet())
				urlConnection.setRequestProperty(key, headerParams.get(key));
			
			DataOutputStream streamWriter = new DataOutputStream(urlConnection.getOutputStream());
			streamWriter.writeBytes(postParams.toString());
			streamWriter.flush();
			streamWriter.close();
			
			int responseCode = urlConnection.getResponseCode();
			
			InputStream inputStream;
			if (responseCode >= 200 && responseCode < 300)
				inputStream = urlConnection.getInputStream();
			else
				inputStream = urlConnection.getErrorStream();
			
			StringBuffer   response     = new StringBuffer();
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream));
			
			String inputLine;
			while ( (inputLine = streamReader.readLine()) != null)
				response.append(inputLine);
			streamReader.close();
			
			if (responseCode >= 200 && responseCode < 300)
				return new JSONObject(response.toString());
			
			return null;
		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
