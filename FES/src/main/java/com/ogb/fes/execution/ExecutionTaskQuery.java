package com.ogb.fes.execution;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;

import net.named_data.jndn.Name;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.SpatialReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.ogb.fes.domain.User;
import com.ogb.fes.ndn.NDNQueryRepoMap;
import com.ogb.fes.ndn.query.NDNFetchManager;
import com.ogb.fes.tesseler.spatial.SpatialPoint;
import com.ogb.fes.tesseler.spatial.SpatialTesseler;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.Utils;
import com.ogb.fes.utils.Utils.Format;


public class ExecutionTaskQuery extends ExecutionTask {
	
	private ArrayList<String>                  queryDecomposition; 
	private HashMap<String, ArrayList<String>> querySharding;
	private ArrayList<String>                  queryNaming;
	private Object                             result;
	
	
	public ExecutionTaskQuery(HashMap<String, Object> params, User user) {
		super(params, user);
		
		queryDecomposition();
		querySend(repoIntersect());
		//querySharding();
		//queryNaming();
		//filterNaming();
	}
	
	
	@Override
	public void run() {
		
		long start, stop;
		
		try {
			start = System.currentTimeMillis();
			
			NDNFetchManager fetchManager = new NDNFetchManager();
			result = fetchManager.fetch(queryNaming);
			
			stop = System.currentTimeMillis();
			stats.setNdnRequestTime(stop-start);
			
			stats.setQueryNameCount(queryNaming.size());
		} 
		catch(Exception e) {
			System.out.println(DateTime.currentTime() + "ExecutionTaskQuery - Exception:\n" + e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object getResult() {
		
		try {
			if (result instanceof ArrayList) {
				StringBuilder resStringB = new StringBuilder();
				resStringB.append("[");
				for (Object entry : (ArrayList<Object>)result) {
					resStringB.append(entry).append(",");
				}
				String resString = resStringB.toString();
				
				if (resString.toCharArray()[resString.length()-1] == ',')
					resString = resString.substring(0, resString.length()-1);
				resString += "]";
			
				return resString;
			}
			
			return result;
		}
		catch(Exception e) {
			System.out.println(DateTime.currentTime() + "ExecutionTaskQuery - getResult Error.");
			return "";
		}
	}
	
	
	public ArrayList<String> queryDecomposition() {
		
		queryDecomposition = new ArrayList<String>();
		queryDecomposition.add(executionParamsContentString);
		return queryDecomposition;
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, ArrayList<String>> querySharding() {
		
		if (queryDecomposition == null)
			queryDecomposition();
		
		querySharding = new HashMap<String, ArrayList<String>>();
		
		HashMap<String, Object> geometry;
		
		boolean isFlooding = false;
		
		ArrayList<Object> operator = (ArrayList<Object>)executionParamsContent.get("$and");
		if (operator == null || operator.size() <= 0) {
			geometry = (HashMap<String, Object>)executionParamsContent.get("geometry");
		}
		else {
			geometry = (HashMap<String, Object>)((HashMap<String, Object>)operator.get(0)).get("geometry");
		}
		
		if (geometry == null) 
			isFlooding = true;
		else if (geometry.get("$geoIntersects") == null)
			isFlooding = true;
		
		
		//The query has not geometry filed: Query Flooding needed. 
		if (isFlooding) {
//			ArrayList<String> shardNames = NDNRepoMap.sharedInstance().getRepoPrefix();
			ArrayList<String> shardNames = NDNQueryRepoMap.sharedInstance().getRepoPrefix();
			for (String shardName : shardNames) {
				if (shardName.compareTo("/") == 0)
					shardName = "/000/000/";
				else if (shardName.split("/").length == 2)
					shardName += "/000/";
				else if (shardName.split("/").length == 3 && shardName.split("/")[2].length() == 0)
					shardName += "000/";
				
				querySharding.put(shardName, queryDecomposition);
			}
			
			return querySharding;
		}
		
		HashMap<String, Object> operation = (HashMap<String, Object>)geometry.get("$geoIntersects");
		HashMap<String, Object> geom      = (HashMap<String, Object>)operation.get("$geometry");
		List<List<Double>>      coords    = (List<List<Double>>)geom.get("coordinates");
		
		String type = (String)geom.get("type");
		if (type.equalsIgnoreCase("POLYGON") == true) {
			long start, stop;
			
			start = System.currentTimeMillis();
// 			ArrayList<String> shardNames = SpatialTesseler.tesselatePolygon100x100(geom);
			ArrayList<String> shardNames = SpatialTesseler.tesselatePolygon10x10(geom);
			stop = System.currentTimeMillis();
			
			stats.setTessellingTime(stop-start);
			for (String shardName : shardNames) {
				querySharding.put(shardName, queryDecomposition);
			}
		}
		else if (type.equalsIgnoreCase("BOX") == true) {
			long start, stop;
			
			SpatialPoint sw = new SpatialPoint(coords.get(0).get(1), coords.get(0).get(0));
			SpatialPoint ne = new SpatialPoint(coords.get(1).get(1), coords.get(1).get(0));
			
			start = System.currentTimeMillis();	
// 			ArrayList<String> shardNames = SpatialTesseler.tasselateBox100x100(sw, ne);
			ArrayList<String> shardNames = SpatialTesseler.tasselateBox10x10(sw, ne);
			stop = System.currentTimeMillis();			
			stats.setTessellingTime(stop-start);
			for (String shardName : shardNames) {
				querySharding.put(shardName, queryDecomposition);
			}
		}
		else {
			querySharding = new HashMap<String, ArrayList<String>>();
		}
//                 System.out.println("querySharding: "+querySharding);
		return querySharding;
	}
	
	public ArrayList<String> queryNaming() {
		
		if (querySharding == null)
			querySharding();
		
		
		queryNaming = new ArrayList<String>();
		ArrayList<String>queryRepoName = NDNQueryRepoMap.sharedInstance().filterNameList(new ArrayList<>( querySharding.keySet()));
		
		for (String repoName : queryRepoName) {
			for (String paramsString : queryDecomposition) {
				String did  = (String)executionParams.get("did");
				String tid  = (String)executionParams.get("tid");
				//paramsString = paramsString.replace("$", "\\$");
				//paramsString = paramsString.replace("\"geometry\"", "\"content.geometry\"");
				
				try {
					paramsString = Name.toEscapedString(ByteBuffer.wrap(paramsString.getBytes("UTF8")));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String name = repoName + "/" + did + "/" + tid + "/QUERY=" + paramsString + "/" + Utils.generateNonce(10);
				queryNaming.add(name);
			}
		}
		
		
		return queryNaming;
	}
	
	private ArrayList<String> filterNaming() {
		queryNaming = NDNQueryRepoMap.sharedInstance().filterNameList(queryNaming);
		return queryNaming;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> repoIntersect() {
		long start = System.currentTimeMillis();
		String polygonJSON = "";
		boolean isFlooding = false;
		ArrayList<String> result = new ArrayList<>();
		HashMap<String, Object> geometry;
				
		if (queryDecomposition == null)
			queryDecomposition();
		ArrayList<Object> operator = (ArrayList<Object>)executionParamsContent.get("$and");
		if (operator == null || operator.size() <= 0) {
			geometry = (HashMap<String, Object>)executionParamsContent.get("geometry");
		}
		else {
			geometry = (HashMap<String, Object>)((HashMap<String, Object>)operator.get(0)).get("geometry");
		}
		
		if (geometry == null) 
			isFlooding = true;
		else if (geometry.get("$geoIntersects") == null)
			isFlooding = true;
		
		if (isFlooding) {
			for (Map.Entry<String, RTree<String, Rectangle>> entry : NDNQueryRepoMap.sharedInstance().rtreeMap.entrySet())
				result.add(entry.getKey());
			return result;
		}
		
		HashMap<String, Object> operation = (HashMap<String, Object>)geometry.get("$geoIntersects");
		HashMap<String, Object> geom      = (HashMap<String, Object>)operation.get("$geometry");
		
		try {
			polygonJSON = new ObjectMapper().writeValueAsString(geom);
		}
		catch(Exception e) {
			polygonJSON = "";
		}
		
		Envelope boxEnvelope = new Envelope();
		Geometry egeometry    = GeometryEngine.geometryFromGeoJson(polygonJSON, GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
		egeometry.queryEnvelope(boxEnvelope);
		
		double startLng = Utils.floor10(boxEnvelope.getXMin(), 1);
		double startLat = Utils.floor10(boxEnvelope.getYMin(), 1);
		double stopLng  = Utils.ceil10(boxEnvelope.getXMax(), 1);
		double stopLat  = Utils.ceil10(boxEnvelope.getYMax(), 1);
		
		if (startLng == stopLng)
			stopLng++;
		if (startLat == stopLat)
			stopLat++;
		
		Rectangle query = Geometries.rectangleGeographic(startLng, startLat, stopLng, stopLat);	
		HashMap<String, RTree<String, Rectangle>> map = NDNQueryRepoMap.sharedInstance().rtreeMap;
		for (Map.Entry<String, RTree<String, Rectangle>> entry : map.entrySet()) {
			RTree<String, Rectangle> rtree =  entry.getValue();
			List<Entry<String,Rectangle>> results = rtree.search(query).toList().toBlocking().single();
			if (results.size()>0) {
//				System.out.println("Repo intersect: "+entry.getKey());
				result.add(entry.getKey());
			}
		}
		long stop = System.currentTimeMillis();
		stats.setIntersectTime(stop-start);
		return result;
		
	}
	
public ArrayList<String> querySend(ArrayList<String> queryRepoName) {
		
	queryNaming = new ArrayList<String>();	
	for (String repoName : queryRepoName) {
			for (String paramsString : queryDecomposition) {
				String did  = (String)executionParams.get("did");
				String tid  = (String)executionParams.get("tid");
				//paramsString = paramsString.replace("$", "\\$");
				//paramsString = paramsString.replace("\"geometry\"", "\"content.geometry\"");
				
				try {
					paramsString = Name.toEscapedString(ByteBuffer.wrap(paramsString.getBytes("UTF8")));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String name = repoName + "/" + did + "/" + tid + "/QUERY=" + paramsString + "/" + Utils.generateNonce(10);
				queryNaming.add(name);
//				System.out.println("Repo Name: "+name);
			}
		}
		return queryNaming;
	}
	
	
	
}

