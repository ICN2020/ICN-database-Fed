package com.ogb.fes.tesseler;


import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import com.ogb.fes.tesseler.spatial.SpatialPoint;


public class RangeQueryParams 
{
	public static int MAX_TILES  = 80;
	public static int MIN_TILES  = 10;
	public static int STEP_TILES = 10;

	public String            tid, cid, uid;
	public ArrayList<String> requestedProperties;
	public String            queryFunction;
	public String            queryType;
	
	public ArrayList<ArrayList<SpatialPoint>> coordinatesPolygonPoint; //Used for handle queryType == Polygon
	public ArrayList<SpatialPoint>            coordinatesBoxPoint;     //Used for handle queryType == Box

	public DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	public String     timeOperator;
	public Date       startDate;
	public Date       stopDate;
	
	public RangeQueryParams() {
		super();
		
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));

	}

	@Override
	public String toString() {
		return "RangeQuery [Function=" + queryFunction + ", Type=" + queryType + ", Coordinates=" + coordinatesBoxPoint+"]";
	}
}
