package com.ogb.fes.utils;


import java.math.BigDecimal;

import java.util.Random;
import java.util.regex.Pattern;

import com.ogb.fes.tesseler.spatial.SpatialNode;
import com.ogb.fes.tesseler.spatial.SpatialPoint;


public class Utils {
	public enum Format {
		LAT_LONG,
		LONG_LAT
	}
	
	//NDN Utilities functions
	public static String[] toStringsForNDNname(double number) {
		String res;
		if (number < 0.0f)
			res = String.format("%07.2f", number);
		else
			res = String.format("%06.2f", number);
		String result[] = new String[3];
		
		String parts[] = res.split(Pattern.quote("."));
		if (parts.length < 2) {
			parts = res.split(",");
			if (parts.length < 2) {
				System.out.println("Error!");
				parts[0] = ""+number;
				parts[1] = "00";	
			}
		}
		
		result[0] = parts[0];
		result[1] = parts[1].toCharArray()[0] +"";
		result[2] = parts[1].toCharArray()[1] +"";

		return result;
	}
	
	public static String generateNonce(int lenght) {
		char[] charset = "1234567890qwertyuiopasdfghjklzxcvbnm".toCharArray();
		
		String res = "";
		for (int i = 0; i < lenght; i++) {
			res += charset[new Random().nextInt(lenght)];
		}
		
		return res;
	}
	
	public static String spatialNodeToNDNName(SpatialNode node, Utils.Format format) {
		SpatialPoint point = node.getSouthWest();
		boolean needSign_lat = false;
		boolean needSign_lng = false;
		
		if (point.latitude < 0.0) {
			point.latitude = node.getNorthEst().latitude;
			needSign_lat = (point.latitude >= 0.0 ? true : false);
		}
		if (point.longitude < 0.0) {
			point.longitude = node.getNorthEst().longitude;
			needSign_lng = (point.longitude >= 0.0 ? true : false);
		}
		
		return Utils.spatialPointToNDNNname(point.latitude, point.longitude, needSign_lat, needSign_lng, node.getLevel(), format);
	}
	
	public static String spatialPointToNDNNname(SpatialPoint point,  int precision, Format format) {
		return spatialPointToNDNNname(point.latitude, point.longitude, false, false, precision, format);
	}
	public static String spatialPointToNDNNname(SpatialPoint point, boolean needSign_lat, boolean needSign_lng,  int precision) {
		return spatialPointToNDNNname(point.latitude, point.longitude, needSign_lat, needSign_lng, precision, Format.LONG_LAT);
	}
	public static String spatialPointToNDNNname(double latitude, double longitude, int precision, Format format) {
		return spatialPointToNDNNname(latitude, longitude, false, false, precision, format);
	}
	public static String spatialPointToNDNNname(double latitude, double longitude, boolean needSign_lat, boolean needSign_lng, int precision, Utils.Format format) {
		String latitudeString[]  = Utils.toStringsForNDNname(latitude);
		String longitudeString[] = Utils.toStringsForNDNname(longitude);
		
		String result = "";
		
		if (format == Format.LAT_LONG)
			result +=  "/" + (needSign_lat ? "-" : "") + latitudeString[0] + "/" + (needSign_lng ? "-" : "") + longitudeString[0];
		else
			result +=  "/" + (needSign_lng ? "-" : "") + longitudeString[0] + "/" + (needSign_lat ? "-" : "") + latitudeString[0];
		
		for (int i = 1; i <= precision; i++) {
			if (format == Format.LAT_LONG)
				result += "/" + latitudeString[i] + longitudeString[i];
			else
				result += "/" + longitudeString[i] + latitudeString[i];
		}
		
		return result;
	}
	

	//--------------Math-utilities-functions----------------//
	static Random rand = new Random(21342141);
	public static double randomBetween(double min, double max) {
		//Check that min is realy the min value
		double temp = 0;
		if (max < min) {
			temp = min;
			min  = max;
			max  = temp;
		}
		
	    double randomNum = rand.nextDouble();
	    randomNum = randomNum*(max - min) + min;
	    
	    return randomNum;
	}
	
	public static double floor10(double number, int decimal) {
		String stringNum = "";
		
		if (number >= 0) {
			number += 0.000001; //For fixing the double precision error 
			stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_FLOOR).toString();
		}
		else {
			number += 0.000001; //For fixing the double precision error 
			stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_FLOOR).toString();
		}
		//System.out.println("Floor To Decimal: " + decimal + "   " + number + " --> " + Double.parseDouble(stringNum));
		return Double.parseDouble(stringNum);
	} 

    public static double ceil10(double number, int decimal) {	
    	String stringNum = "";
		
    	if (number >= 0) {
    		number -= 0.000001; //For fixing the double precision error 
    		stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_CEILING).toString();
    	}
    	else{
    		number -= 0.000001; //For fixing the double precision error 
    		stringNum = new BigDecimal(String.valueOf(number)).setScale(decimal, BigDecimal.ROUND_CEILING).toString();
    	}
    	//System.out.println("Ceil To Decimal: " + decimal + "   " + number + " --> " + Double.parseDouble(stringNum));
		return Double.parseDouble(stringNum);
	} 
    
    
    //Rectangle utilities functions
	public static boolean valueInRange(double value, double min, double max) {
		return (value > min) && (value < max);
	}
	
	
	public static String generateRandomName(int lenght) {
		char[] charset = "1234567890qwertyuiopasdfghjklzxcvbnm".toCharArray();
		
		String res = "";
		for (int i = 0; i < lenght; i++) {
			res += charset[new Random().nextInt(lenght)];
		}
		
		return res;
	}
}

