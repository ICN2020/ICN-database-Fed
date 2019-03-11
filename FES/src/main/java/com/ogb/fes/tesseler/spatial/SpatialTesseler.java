package com.ogb.fes.tesseler.spatial;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.OperatorContains;
import com.esri.core.geometry.OperatorIntersects;
import com.esri.core.geometry.SpatialReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.ogb.fes.ndn.NDNQueryRepoMap;
import com.ogb.fes.utils.Utils;
import com.ogb.fes.utils.Utils.Format;


public class SpatialTesseler {
	
	public static ArrayList<String> tesselateBox(SpatialPoint sw, SpatialPoint ne, int k) {
	
		int height = (int)(Utils.ceil10(ne.latitude, 0) - Utils.floor10(sw.latitude, 0));
		int width  = (int)(Utils.ceil10(ne.longitude, 0) - Utils.floor10(sw.longitude, 0));
		
		if (width*height >= k) {
			return tasselateBox100x100(sw, ne);
		}
	 
		SpatialNode head = new SpatialNode(sw, ne, 0);
		tesselateBox(head, head, sw, ne, 0, k);
		
		ArrayList<String> result = new ArrayList<String>();
		for (SpatialNode node : head.getLeaves()) {
			result.add(Utils.spatialNodeToNDNName(node, Format.LONG_LAT));
		}
		
		return result;
	}
	
	
	 
	public static ArrayList<String> tasselateBox100x100(SpatialPoint sw, SpatialPoint ne) {
		ArrayList<String> result = new ArrayList<String>();
		
		int startLat = (int)Utils.floor10(sw.latitude, 0);
		int startLng = (int)Utils.floor10(sw.longitude, 0);
		int stopLat  = (int)Utils.ceil10(ne.latitude, 0);
		int stopLng  = (int)Utils.ceil10(ne.longitude, 0);
		
		for (int lat = startLat; lat <= stopLat; lat++) {
			for (int lon = startLng; lon <= stopLng; lon++) {
				result.add(Utils.spatialPointToNDNNname(lat, lon, 0, Format.LONG_LAT));
			}
		}
		
		return result;
	}
	
	public static ArrayList<String> tasselateBox10x10(SpatialPoint sw, SpatialPoint ne) {
		ArrayList<String> result = new ArrayList<String>();
		
		double step     = 0.1;
		double startLat = Utils.floor10(sw.latitude, 1);
		double startLng = Utils.floor10(sw.longitude, 1);
		double stopLat  = Utils.ceil10(ne.latitude, 1);
		double stopLng  = Utils.ceil10(ne.longitude, 1);
	
		for (double lat = startLat; lat <= stopLat; lat+=step) {
			for (double lon = startLng; lon <= stopLng; lon+=step) {
				result.add(Utils.spatialPointToNDNNname(lat, lon, 1, Format.LONG_LAT));
			}
		}
                return result;

	}
	
	private static void tesselateBox(SpatialNode superNode, SpatialNode head, SpatialPoint worldSW, SpatialPoint worldNE, int level, int k) {
		 
		int step     = (int)Math.pow(10, 2-level);
		int startLat = (int)Utils.floor10(head.getSouthWest().latitude*100, level);
		int startLng = (int)Utils.floor10(head.getSouthWest().longitude*100, level);
		int stopLat  = (int)Utils.ceil10(head.getNorthEst().latitude*100, level);
		int stopLng  = (int)Utils.ceil10(head.getNorthEst().longitude*100, level);
		
		for (int lat = startLat; lat < stopLat; lat+=step) {
			for (int lon = startLng; lon < stopLng; lon+=step) {
				SpatialPoint point = new SpatialPoint(lat/100.0, lon/100.0);
				SpatialNode  node  = new SpatialNode(point, level);
				if (node.computeIntersection(worldSW, worldNE)) {
			
					if (level == 0 || superNode.getLeaves().size() < k)
						head.appendChild(node);
					else {
						head.aggregate();	
						return;
					}
				}
			}
		}
		
		for (SpatialNode node : head.getChildren()) {
			if (level+1 < 3 && node.isFull() == false)
				if (superNode.getLeaves().size() < k)
					tesselateBox(superNode, node, worldSW, worldNE, level+1, k);
		}
	}
	
	
	public static ArrayList<String> tesselatePolygon(HashMap<String, Object> geometryMap, int k) {
		String polygonJSON = "";
		
		try {
			polygonJSON = new ObjectMapper().writeValueAsString(geometryMap);
		}
		catch(Exception e) {
			polygonJSON = "";
		}
		
		Envelope boxEnvelope = new Envelope();
		Geometry geometry   = GeometryEngine.geometryFromGeoJson(polygonJSON, GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
		geometry.queryEnvelope(boxEnvelope);
		
		int startLat = (int)Utils.floor10(boxEnvelope.getXMin(), 0);
		int startLng = (int)Utils.floor10(boxEnvelope.getYMin(), 0);
		int stopLat  = (int)Utils.ceil10(boxEnvelope.getXMax(), 0);
		int stopLng  = (int)Utils.ceil10(boxEnvelope.getYMax(), 0);
		int height   = (int)(Utils.ceil10(stopLat, 0) - Utils.floor10(startLat, 0));
		int width    = (int)(Utils.ceil10(stopLng, 0) - Utils.floor10(startLng, 0));
		
		if (width*height >= k) {
			return tesselatePolygon100x100(geometryMap);
		}
		
		SpatialPoint sw = new SpatialPoint(boxEnvelope.getYMin(), boxEnvelope.getXMin());
		SpatialPoint ne = new SpatialPoint(boxEnvelope.getYMax(), boxEnvelope.getXMax());
		SpatialNode head = new SpatialNode(sw, ne, 0);
		tesselatePolygon(head, head, geometry, boxEnvelope, 0, k);
		
		ArrayList<String> result = new ArrayList<String>();
		for (SpatialNode node : head.getLeaves()) {
			result.add(Utils.spatialNodeToNDNName(node, Format.LONG_LAT));
		}
		
		return result;
	}
		 
	public static ArrayList<String> tesselatePolygon100x100(HashMap<String, Object> geometryMap) {

		String polygonJSON = "";
		
		try {
			polygonJSON = new ObjectMapper().writeValueAsString(geometryMap);
		}
		catch(Exception e) {
			polygonJSON = "";
		}
		
		Envelope boxEnvelope = new Envelope();
		Geometry geometry    = GeometryEngine.geometryFromGeoJson(polygonJSON, GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
		geometry.queryEnvelope(boxEnvelope);
	
		return tesselatePolygon100x100(geometry, boxEnvelope);
	}
	
	private static ArrayList<String> tesselatePolygon100x100(Geometry geometry, Envelope boxEnvelope) {

		ArrayList<String> result = new ArrayList<>();
		SpatialReference  sr     = SpatialReference.create(4326);
		
		int startLng = (int)Utils.floor10(boxEnvelope.getXMin(), 0);
		int startLat = (int)Utils.floor10(boxEnvelope.getYMin(), 0);
		int stopLng  = (int)Utils.ceil10(boxEnvelope.getXMax(), 0);
		int stopLat  = (int)Utils.ceil10(boxEnvelope.getYMax(), 0);
		
		if (startLng == stopLng)
			stopLng++;
		if (startLat == stopLat)
			stopLat++;

		for (int lat = startLat; lat < stopLat; lat++) {
			for (int lon = startLng; lon < stopLng; lon++) {			
				Envelope envelope = new Envelope(lon, lat, lon+1, lat+1);
				
				boolean intersect = OperatorIntersects.local().execute(geometry, envelope, sr, null);
				if (intersect) {	
					result.add(Utils.spatialPointToNDNNname(lat, lon, 0, Format.LONG_LAT));
				}
			}
		}
		
		return result;
	}
	
	public static ArrayList<String> tesselatePolygon10x10(HashMap<String, Object> geometryMap) {

		String polygonJSON = "";
		
		try {
			polygonJSON = new ObjectMapper().writeValueAsString(geometryMap);
		}
		catch(Exception e) {
			polygonJSON = "";
		}
		
		Envelope boxEnvelope = new Envelope();
		Geometry geometry    = GeometryEngine.geometryFromGeoJson(polygonJSON, GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Unknown).getGeometry();
		geometry.queryEnvelope(boxEnvelope);
	
		return tesselatePolygon10x10(geometry, boxEnvelope);
	}
	
	private static ArrayList<String> tesselatePolygon10x10(Geometry geometry, Envelope boxEnvelope) {

		ArrayList<String> result = new ArrayList<>();
		SpatialReference  sr     = SpatialReference.create(4326);
		
		double startLng = Utils.floor10(boxEnvelope.getXMin(), 1);
		double startLat = Utils.floor10(boxEnvelope.getYMin(), 1);
		double stopLng  = Utils.ceil10(boxEnvelope.getXMax(), 1);
		double stopLat  = Utils.ceil10(boxEnvelope.getYMax(), 1);
		
		if (startLng == stopLng)
			stopLng++;
		if (startLat == stopLat)
			stopLat++;

		for (double lat = startLat; lat < stopLat; lat+=0.1) {
			for (double lon = startLng; lon < stopLng; lon+=0.1) {			
				Envelope envelope = new Envelope(lon, lat, lon+0.1, lat+0.1);
				
				boolean intersect = OperatorIntersects.local().execute(geometry, envelope, sr, null);
				if (intersect) {	
					result.add(Utils.spatialPointToNDNNname(lat, lon, 1, Format.LONG_LAT));
				}
			}
		}
		
		Rectangle query = Geometries.rectangleGeographic(startLng, startLat, stopLng, stopLat);	
		HashMap<String, RTree<String, Rectangle>> map = NDNQueryRepoMap.sharedInstance().rtreeMap;
		for (Map.Entry<String, RTree<String, Rectangle>> entry : map.entrySet()) {
			RTree<String, Rectangle> rtree =  entry.getValue();
			List<Entry<String,Rectangle>> entries = rtree.entries().toList().toBlocking().single();
			System.out.println("entries: "+entries.size());
			List<Entry<String,Rectangle>> results = rtree.search(query).toList().toBlocking().single();
			if (results.size()>0) {
				System.out.println("Repo intersect: "+entry.getKey());
			}
		}
		return result;
	}
	 
	private static void tesselatePolygon(SpatialNode superNode, SpatialNode head, Geometry geometry, Envelope boxEnvelope, int level, int k) {
		
		int step     = (int)Math.pow(10, 2-level);
		int startLat = (int)Utils.floor10(head.getSouthWest().latitude*100, level);
		int startLng = (int)Utils.floor10(head.getSouthWest().longitude*100, level);
		int stopLat  = (int)Utils.ceil10(head.getNorthEst().latitude*100, level);
		int stopLng  = (int)Utils.ceil10(head.getNorthEst().longitude*100, level);
		
		SpatialReference sr = SpatialReference.create(4326);
		
		for (int lat = startLat; lat < stopLat; lat+=step) {
			for (int lon = startLng; lon < stopLng; lon+=step) {
				SpatialPoint sw       = new SpatialPoint(lat/100.0, lon/100.0);
				SpatialPoint ne       = new SpatialPoint(sw.latitude+Math.pow(10, 2-level)/100.0, sw.longitude+Math.pow(10, 2-level)/100.0);
				SpatialNode  node     = new SpatialNode(sw, level);
				Envelope     envelope = new Envelope(sw.longitude, sw.latitude, ne.longitude, ne.latitude);
				
				boolean intersect = OperatorIntersects.local().execute(geometry, envelope, sr, null);
				if (intersect) {	
					node.setFull(OperatorContains.local().execute(geometry, envelope, sr, null));
					if (level == 0 || superNode.getLeaves().size() < k)
						head.appendChild(node);
					else {
						head.aggregate();	
						return;
					}
				}		
			}
		}
		
		for (SpatialNode node : head.getChildren()) {
			if (level+1 < 3 && node.isFull() == false)
				if (superNode.getLeaves().size() < k)
					tesselatePolygon(superNode, node, geometry, boxEnvelope, level+1, k);
		}
	}
}
