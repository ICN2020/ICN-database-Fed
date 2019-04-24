package com.ogb.clientlib;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


/***
 * 
 * @author Bonvoyage/ICN2020 Univ. Tor Vergata Roma Project team
 *
 */
public class OgbClientLib {

	public String FrontEndServerURL;

	/***
	 * 
	 * @param URL of the Front End Server
	 */
	public OgbClientLib(String serverURL) {
		super();
		this.FrontEndServerURL = serverURL;
	}



	/***
	 * 
	 * @param userId : user name
	 * @param tenant : tenant name
	 * @param password : user password
	 * @return secure token to be used for next operations with the FrontEndServer. This token identify the user. keep it secret..
	 */
	public String login(String userId, String tenant, String password)
	{
		String token = null;
		JSONObject loginParams = new JSONObject();
		try 
		{
			HashMap<String, String> headerParams = new HashMap<String, String>();
			headerParams.put("Content-Type", "application/json");

			loginParams.put("tenantName", tenant);
			loginParams.put("userName", userId);
			loginParams.put("password", password);

			JSONObject response = sendPost(headerParams, loginParams.toString(), FrontEndServerURL+"/OGB/user/login");
			//System.out.println("response: " + response.toString(4));

			if (response!= null)
			{
				int code = response.optInt("code", -1); 
				if (code > 199 && code < 300)
				{
					JSONObject responseObj = new JSONObject(response.optString("response"));
					token = responseObj.optString("token");
					//System.out.println("token: "+ token);
					return token;
				}
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return token;
	}



	private String insertGeoJSON(String token, String cid, String geoJSON)
	{
		try {
			String url = FrontEndServerURL+ "/OGB/content/insert/"+cid;
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Authorization", token);	
			headers.put("Content-Type", "application/json");
			JSONObject result = sendPost(headers, geoJSON, url);
			if (result.optInt("code") == 200) {
				//System.out.println("Insertion ok");
				JSONObject rj = new JSONObject(result.optString("response"));
				return rj.optString("oid");
				//System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));
			} else {
				System.out.println("Insertion failed");
				System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));
				return null;
			}

		} catch (Exception ex) {
			System.err.println(ex);
		} 
		return null;
	}


	/***
	 * 
	 * @param token : string retrieved from login
	 * @param cid : collection identifier
	 * @param oid : unique identifier of the geoJSON object
	 * @return true/false if success/failure
	 */
	public boolean deleteObject(String token, String oid) {
		String url = FrontEndServerURL+ "/OGB/content/delete/";
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Authorization", token);	
		headers.put("Content-Type", "application/json");
		String json = "{\"oid\": \""+oid+"\"}";
		JSONObject result = sendPost(headers, json, url);
		if (result.optInt("code")==200) {
			//System.out.println("Insertion ok");
			//JSONObject rj = new JSONObject(result.optString("response"));

			//System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));
			return true;
		} else {
			System.out.println("Delete failed");
			System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));    		
		}   
		return false;
	}


	/***
	 * 
	 * @param token : string retrieved from login
	 * @param cid : collection identifier
	 * @param oid : unique identifier of the geoJSON object
	 * @return a string representing geoJSON object
	 */
	public String queryObject(String token, String cid, String oid) {
		String url = FrontEndServerURL+ "/OGB/query-service/element/"+cid;
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Authorization", token);	
		headers.put("Content-Type", "application/json");
		String json = "{\"oid\": \""+oid+"\"}";
		JSONObject result = sendPost(headers, json, url);
		if (result.optInt("code")==200) {
			//System.out.println("Insertion ok");
			//JSONObject rj = new JSONObject(result.optString("response"));

			//System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));
			return result.optString("response");
		} else {
			System.out.println("Query failed");
			System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));    		
		}   
		return null;
	}

	/***
	 * 
	 * @param token : string retrieved from login
	 * @param cid : collection identifier
	 * @param JSON : query in escaped JSON object
	 * @return a string representing geoJSON object
	 */
	public String queryJSON(String token, String cid, String JSON) {
		String url = FrontEndServerURL+ "/OGB/query-service/"+cid;
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Authorization", token);	
		headers.put("Content-Type", "application/json");
		String json = "";
		if(isJSONValid(JSON)){
			json = JSON;
			//System.out.println("valid JSON: \n"+json);
		}
		else{
			System.out.println("Not valid JSON");
			return null;
		}
		JSONObject result = sendPost(headers, json, url);
		if (result.optInt("code")==200) {
			//System.out.println("Insertion ok");
			//JSONObject rj = new JSONObject(result.optString("response"));

			//System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));
			return result.optString("response");
		} else {
			System.out.println("Query failed");
			System.out.println("SERVER REPLY, code: "+result.optInt("code")+" response: "+result.optString("response"));    		
		}   
		return null;
	}

	private JSONObject sendPostU(Map<String, String> headerParams, String postParams, String url) {

		try {

			HttpURLConnection urlConnection = (HttpURLConnection)new URL(url).openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);

			for (String key : headerParams.keySet())
				urlConnection.setRequestProperty(key, headerParams.get(key));

			//			long start = System.currentTimeMillis();
			DataOutputStream streamWriter = new DataOutputStream(urlConnection.getOutputStream());
			streamWriter.writeBytes(postParams.toString());
			streamWriter.flush();
			streamWriter.close();

			int    responseCode    = urlConnection.getResponseCode();
			String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
			//			System.out.println("(library) Real post time: "+(System.currentTimeMillis()-start));

			InputStream inputStream;
			if (responseCode >= 200 && responseCode < 300)
				inputStream = urlConnection.getInputStream();
			else
				inputStream = urlConnection.getErrorStream();

			StringBuffer response = new StringBuffer();

			if (contentEncoding != null && contentEncoding.compareTo("gzip") == 0)
				inputStream = new GZIPInputStream(inputStream);

			BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream));

			String inputLine;
			while ( (inputLine = streamReader.readLine()) != null)
				response.append(inputLine);
			streamReader.close();

			JSONObject result = new JSONObject();
			result.put("code", responseCode);
			result.put("response", response.toString());
			return result;

		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}



	private JSONObject sendPost(Map<String, String> headerParams, String postParams, String url) {
		if (url.contains("http:")) {
			return sendPostU(headerParams, postParams, url);
		}

		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}

						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType)
								throws CertificateException {
						}
						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType)
								throws CertificateException {
						}
					}
			};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			HttpsURLConnection urlConnection = (HttpsURLConnection)new URL(url).openConnection();
			urlConnection.setRequestMethod("POST");

			urlConnection.setDoOutput(true);

			for (String key : headerParams.keySet())
				urlConnection.setRequestProperty(key, headerParams.get(key));

			//long start = System.currentTimeMillis();
			DataOutputStream streamWriter = new DataOutputStream(urlConnection.getOutputStream());
			streamWriter.writeBytes(postParams.toString());
			streamWriter.flush();
			streamWriter.close();

			int    responseCode    = urlConnection.getResponseCode();
			String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
			//System.out.println("Real post time: "+(System.currentTimeMillis()-start));

			InputStream inputStream;
			if (responseCode >= 200 && responseCode < 300)
				inputStream = urlConnection.getInputStream();
			else
				inputStream = urlConnection.getErrorStream();

			if (contentEncoding != null && contentEncoding.compareTo("gzip") == 0)
				inputStream = new GZIPInputStream(inputStream);

			StringBuffer   response     = new StringBuffer();
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream));

			String inputLine;
			while ( (inputLine = streamReader.readLine()) != null)
				response.append(inputLine);
			streamReader.close();


			JSONObject result = new JSONObject();
			result.put("code",responseCode);
			result.put("response",response.toString());

			return result;
		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param geoJSON : string representing geoJSON object 
	 * @return object identifier
	 */
	public String addGeoJSON(String token, String cid, String geoJSON){
		String oid = null;
		if (!isJSONValid(geoJSON))
			System.out.println("Not valid GeoJSON");
		else
			oid=insertGeoJSON(token, cid, geoJSON);

		return oid;
	}



	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param propertiesMap : set of geo-json properties, hashmap <string properties, string property_value>
	 * @param location : [latitude longitude] double array
	 * @return object identifier
	 */
	
	public String addPoint(String token, String cid, HashMap<String, String> propertiesMap, final double[] location)
	{
		double lat = location[0];
		double lon = location[1];
		boolean first=true;
		String oid = null;

		String json = "{\"geometry\" : {\"type\" : \"Point\", \"coordinates\" : ["+ lon + ", " + lat +"]},\"type\":\"Feature\", \"properties\" : {";

		for (String key : propertiesMap.keySet()) {
			if (!first)
				json = json+",";
			else
				first=false;

			json = json +"\""+key+"\" : \""+propertiesMap.get(key)+"\"";
		}
		json = json+"}}";
		//		System.out.println("geoJSON to be inserted: "+json);
		if (!isJSONValid(json))
			System.out.println("Not valid GeoJSON");
		else
			oid=insertGeoJSON(token, cid, json);
		return oid;
	}


	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param propertiesMap : set of geo-json properties, hashmap <string properties, string property_value>
	 * @param location : array list of [latitude longitude] double arrays
	 * @return object identifier
	 */
	public String addMultiPoint(String token, String cid, HashMap<String, String> propertiesMap, ArrayList<double[]> coordinates)
	{
		//		long startTime=System.currentTimeMillis();
		String json = makeJSONStringForMultipoint(token, cid, propertiesMap, coordinates);
		String oid = null;
		//		System.out.println("(library) json calculation time:"+(System.currentTimeMillis()-startTime));
		if (!isJSONValid(json))
			System.out.println("Not valid GeoJSON");
		else
			oid=insertGeoJSON(token, cid, json);
		//		System.out.println("(library) total time: "+(System.currentTimeMillis()-startTime));

		return oid;

	}

	private String makeJSONStringForPolygon(String token, String cid, HashMap<String, String> propertiesMap, ArrayList<double[]> coordinates)
	{
		boolean first=true;
		StringBuilder json = new StringBuilder();
		json.append("{\"geometry\" : {\"type\" : \"Polygon\", \"coordinates\" : [[");

		for (int i = 0; i < coordinates.size(); i++) {
			double lat=coordinates.get(i)[0];
			double lon=coordinates.get(i)[1];

			if (i>0) json.append(",");
			json.append("[").append(lon).append(", ").append(lat).append("]");
		}
		json.append("]]},\"type\":\"Feature\", \"properties\" : {");

		for (String key : propertiesMap.keySet()) {
			if (!first)
				json.append(",");
			else
				first=false;

			json.append("\"").append(key).append("\" : \"").append(propertiesMap.get(key)).append("\"");
		}

		json.append("}}");
		return json.toString();
	}

	private String makeJSONStringForMultipoint(String token, String cid, HashMap<String, String> propertiesMap, ArrayList<double[]> coordinates)
	{
		StringBuilder json = new StringBuilder();

		boolean first=true;
		json.append("{\"geometry\" : {\"type\" : \"MultiPoint\", \"coordinates\" : [");

		for (int i = 0; i < coordinates.size(); i++) {
			double lat=coordinates.get(i)[0];
			double lon=coordinates.get(i)[1];
			if (i>0) json.append(",");
			json.append("[").append(lon).append(", ").append(lat).append("]");
		}
		json.append("]},\"type\":\"Feature\", \"properties\" : {");
		for (String key : propertiesMap.keySet()) {
			if (!first)
				json.append(",");
			else
				first=false;

			json.append("\"").append(key).append("\" : \"").append(propertiesMap.get(key)).append("\"");
		}
		json.append("}}");

		return json.toString();
	}


	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param propertiesMap : set of geo-json properties, hashmap <string properties, string property_value>
	 * @param location : array list of list of [latitude longitude] double arrays
	 * @return object identifier
	 */
	public String addPolygon(String token, String cid, HashMap<String, String> propertiesMap, ArrayList<double[]> coordinates)
	{
		String  oid  = null;

		String json = makeJSONStringForPolygon(token, cid, propertiesMap, coordinates);

		if (!isJSONValid(json))
			System.out.println("Not valid GeoJSON");
		else
			oid=insertGeoJSON(token, cid, json);

		return oid;

	}

	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param sw_lat : south west latitude
	 * @param sw_lon : south west longitude
	 * @param boxSize : range query box size in lat/lon degrees
	 * @return a string representing JSON array of geoJSON objects
	 */
	public String rangeQuery(String token, String cid, double sw_lat, double sw_lon, double boxSize) 
	{
		return this.rangeQueryBox(token, cid, sw_lat, sw_lon, sw_lat+boxSize, sw_lon+boxSize);
	}


	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param sw_lat : south west latitude
	 * @param sw_lon : south west longitude
	 * @param ne_lat : north east latitude
	 * @param ne_lon : north east longitude
	 * @return a string representing JSON array of geoJSON objects
	 */
	public String rangeQueryBox(String token, String cid, double sw_lat, double sw_lon, double ne_lat, double ne_lon) 
	{
		if (sw_lat>ne_lat||sw_lon>ne_lon)
			return "Not valid box edge coordinates";

		ArrayList<ArrayList<double[]>> polygon = new ArrayList<>(); polygon = new ArrayList<>();	
		ArrayList<double[]> subPolygon = new ArrayList<>(); 
		double queryPol_lat_point1 = sw_lat;
		double queryPol_lon_point1 = sw_lon;
		double queryPol_lat_point2 = ne_lat;
		double queryPol_lon_point2 = sw_lon;
		double queryPol_lat_point3 = ne_lat;
		double queryPol_lon_point3 = ne_lon;
		double queryPol_lat_point4 = sw_lat;
		double queryPol_lon_point4 = ne_lon;
		subPolygon.add(new double[] {queryPol_lat_point1,queryPol_lon_point1});
		subPolygon.add(new double[] {queryPol_lat_point2,queryPol_lon_point2});
		subPolygon.add(new double[] {queryPol_lat_point3,queryPol_lon_point3});
		subPolygon.add(new double[] {queryPol_lat_point4,queryPol_lon_point4});
		subPolygon.add(new double[] {queryPol_lat_point1,queryPol_lon_point1});
		polygon.add(subPolygon);
		return rangeQueryPolygon(token, cid, polygon);
	}


	/***
	 * 
	 * @param token : string retrieved by login  
	 * @param cid : collection identifier
	 * @param coordinates : ArrayList of ArrayList<[latitude longitude]> double arrays, at the moment supporting only polygon without holes (for more information see http://geojson.org/geojson-spec.html#id4)
	 * @return a string representing JSON array of geoJSON objects
	 */
	public String rangeQueryPolygon(String token, String cid, ArrayList<ArrayList<double[]>> coordinates )
	{
		try{
			String json = "{\"geometry\":{\"$geoIntersects\":{\"$geometry\":{\"type\":\"Polygon\", \"coordinates\":[";


			int counter =0;

			for(ArrayList<double[]> coordinateArray : coordinates)		// if Polygon has "holes" we have multiple array<coordinate[lat,lon]>
			{
				if (counter>0) json += ",";
				json = json + "[";
				for (int i = 0; i < coordinateArray.size(); i++) {
					double lat=coordinateArray.get(i)[0];
					double lon=coordinateArray.get(i)[1];
					if (i>0) json += ",";
					json = json + "["+ lon + ", " + lat +"]";
				}
				json = json + "]";
				counter++;
			}

			json = json + "]}}}}";
			//		System.out.println("geoJSON query: "+json);

			String url = FrontEndServerURL+ "/OGB/query-service/"+cid;
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Authorization", token);	
			headers.put("Content-Type", "application/json");
			JSONObject result = sendPost(headers, json, url);

			return result.optString("response");
		}
		catch(Exception e) {
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		}

		return null;
	}


	private boolean isJSONValid(final String json) 
	{
		//		long startTime = System.currentTimeMillis();
		boolean valid = false;

		try {
			final JsonParser parser = new ObjectMapper().getFactory().createParser(json);
			while (parser.nextToken() != null) {
			}
			valid = true;
		}
		catch (JsonParseException jpe) {
			jpe.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		//		System.out.println("(library) isJSONValid time: "+(System.currentTimeMillis()-startTime));
		return valid;

	}



	/***
	 * 
	 * @param args
	 * 
	 */
	public static void main( String args[] ) {

				String serverURL = "https://127.0.0.1:443";
				String token;
		
				String uid = "myUser-id";	  	// user id
				String tid = "myTenant-id";		// tenant id
				String pwd = "myPassword";   	// password
				String cid = "myCollection-id";	// collection id

		OgbClientLib ogbTestClient = new OgbClientLib(serverURL);

		// LOGIN
		token=ogbTestClient.login(uid, tid, pwd);	// get token for HTTP next interactions
		if (token!= null) {
			System.out.println("Authentication ok, token: "+token);
		} 
		else {
			System.out.println("Authentication failure");
			System.exit(-1);
			return;
		}

		// INSERTION OF POINT OBJECT 
		System.out.println("\n**** Point geoJSON Insert ****\n\n");
		// point coordinates, where latitude is the first and latitude the second
		double [] coordinates = {0.1, 0.1};
		// point properties
		HashMap<String,String> prop = new HashMap<String,String>();
		prop.put("prop100", "value100");	
		prop.put("prop200", "value200");
		// db insertion, response is the object identifier (oid)
		String oid = ogbTestClient.addPoint(token, cid, prop, coordinates);
		if (oid != null) {
			System.out.println("Insertion ok, oid : "+oid);
		}
		else {
			System.exit(-1);
			return;
		}

		// Sleep necessary to allow backend to finish insert procedure, otherwise range query could not return the inserted item
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// RANGE QUERY, response is a JSON Array
		System.out.println("\n**** Range Query ****\n\n");
		String response = ogbTestClient.rangeQuery(token, cid, 0.0, 0.0, 0.5);
		System.out.println("query response: " + response);

		// DELETE OBJECT
		System.out.println("**** Delete object ****\n\n");
		if (ogbTestClient.deleteObject(token, oid)) {
			System.out.println("Delete of " + oid+" OK");
		};

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// RANGE QUERY, response is a JSON Array
		System.out.println("\n**** Range Query ****\n");
		response = ogbTestClient.rangeQuery(token, cid, 0.0, 0.0, 0.5);
		System.out.println("query response: " + response);


		// INSERTION OF MULTI-POINT OBJECT 
		System.out.println("\n**** MultiPoint geoJSON Insert ****\n\n");
		// multipoint coordinate

		ArrayList<double[]> mcoordinates = new ArrayList<double[]>();
		mcoordinates.add(new double[] {0.1,  0.1 }); //point 1
		mcoordinates.add(new double[] {0.11, 0.11}); // point 2
		// point properties
		HashMap<String,String> mprop = new HashMap<String,String>();
		mprop.put("prop100", "value100");	
		mprop.put("prop200", "value200");

		// db insertion, response is the object identifier (oid)
		String moid = ogbTestClient.addMultiPoint(token,cid, mprop, mcoordinates);
		if (moid!=null) {
			System.out.println("Insertion ok, oid : "+moid);
		} else {
			return;
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		// RANGE QUERY, response is a JSON Array
		System.out.println("\n**** Range Query ****\n");
		response = ogbTestClient.rangeQuery(token, cid, 0.0, 0.0, 0.5);
		System.out.println("query response: " + response);

		// DELETE OBJECT
		System.out.println("**** Delete object ****\n\n");
		if (ogbTestClient.deleteObject(token, moid)) {
			System.out.println("Delete of " + moid+" OK");
		};

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		// INSERTION OF POLYGON OBJECT 
		System.out.println("\n\n**** Polygon geoJSON Insert ****\n");
		// polygon coordinate [latitude, longitude]
		ArrayList<double[]> pcoordinates = new ArrayList<double[]>();
		double polygon_lat_point1 = 0.00;	//point 1 latitude
		double polygon_lon_point1 = 0.00;   //point 1 longitude
		double polygon_lat_point2 = 0.11;	//point 2 latitude
		double polygon_lon_point2 = 0.11;	//point 2 longitude
		double polygon_lat_point3 = 0.33;	//point 3 latitude
		double polygon_lon_point3 = 0.33;	//point 3 longitude
		pcoordinates.add(new double[] { polygon_lat_point1, polygon_lon_point1 }); 
		pcoordinates.add(new double[] { polygon_lat_point2, polygon_lon_point2 });
		pcoordinates.add(new double[] { polygon_lat_point3, polygon_lon_point3});
		pcoordinates.add(new double[] { polygon_lat_point1, polygon_lon_point1});
		// point properties
		HashMap<String,String> polygonProp = new HashMap<String,String>();
		polygonProp.put("prop100", "value100");	
		polygonProp.put("prop200", "value200");

		// db insertion, response is the object identifier (oid)
		String poid = ogbTestClient.addPolygon(token,cid, polygonProp, pcoordinates);
		if (poid!=null) {
			System.out.println("Insertion ok, oid : "+poid);
		} else {
			return;
		}


		System.out.println("\n**** Range Query Polygon ****\n\n");
		ArrayList<ArrayList<double[]>> polygon = new ArrayList<>();
		ArrayList<double[]> subPolygon = new ArrayList<>(); 
		double queryPol_lat_point1 = 0.0;	//point 1 latitude in degree
		double queryPol_lon_point1 = 0.0;  //point 1 longitude in degree
		double queryPol_lat_point2 = 0.0;	//point 2 latitude in degree
		double queryPol_lon_point2 = 0.5;	//point 2 longitude in degree
		double queryPol_lat_point3 = 0.5;	//point 3 latitude in degree
		double queryPol_lon_point3 = 0.5;	//point 3 longitude in degree
		double queryPol_lat_point4 = 0.5;	//point 4 latitude in degree
		double queryPol_lon_point4 = 0.0;  //point 4 longitude in degree
		subPolygon.add(new double[] {queryPol_lat_point1,queryPol_lon_point1});
		subPolygon.add(new double[] {queryPol_lat_point2,queryPol_lon_point2});
		subPolygon.add(new double[] {queryPol_lat_point3,queryPol_lon_point3});
		subPolygon.add(new double[] {queryPol_lat_point4,queryPol_lon_point4});
		subPolygon.add(new double[] {queryPol_lat_point1,queryPol_lon_point1});
		polygon.add(subPolygon);
		String responsePolQ = ogbTestClient.rangeQueryPolygon(token, cid, polygon);
		System.out.println("query response: " + responsePolQ+"\n\n");

		// DELETE OBJECT
		System.out.println("**** Delete object ****\n\n");
		if (ogbTestClient.deleteObject(token, poid)) {
			System.out.println("Delete of " + poid+" OK");
		};

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		// RANGE QUERY, response is a JSON Array
		System.out.println("\n**** Range Query ****\n");
		response = ogbTestClient.rangeQuery(token, cid, 0.0, 0.0, 0.5);
		System.out.println("query response: " + response);

	}
}