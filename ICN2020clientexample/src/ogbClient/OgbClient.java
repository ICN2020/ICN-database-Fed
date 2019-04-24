package ogbClient;

import java.util.ArrayList;
import java.util.HashMap;
import com.ogb.clientlib.*;

public class OgbClient {
	public static void main( String args[] ) {

		String uid = "admin";	// user id (not change)
		String pwd = "icn2020";	// password (not change)
		
		String tid = "icn2020";	// tenant id (not change)
		String cid = "testCID";	// collection id 
		
		String serverURL = "https://52.233.130.10:443"; // server connection to the test OGB platform
				
		String token;
		OgbClientLib ogbTestClient = new OgbClientLib(serverURL);

		// LOGIN
		token=ogbTestClient.login(uid, tid, pwd);	// get token for HTTP next interactions
		if (token!= null) {
			System.out.println("Authentication ok, token: "+token);
		} else {
			System.out.println("Authentication failure");
			return;
		}


		// INSERTION OF POINT OBJECT 
		System.out.println("\n\n**** Point geoJSON Insert ****");
		// point coordinates
		double lat = 0.1;
		double lon = 0.2;
		double [] coordinates =  {lat, lon};
		// point properties
		HashMap<String,String> prop = new HashMap<String,String>();
		prop.put("train-name", "ice-374");	
		prop.put("train-speed", "170 km/h");
		// db insertion, response is the object identifier (oid)
		String oid = ogbTestClient.addPoint(token,cid, prop, coordinates);
		if (oid!=null) {
			System.out.println("Insertion ok, oid : "+oid);
		} 
		else {
			return;
		}

		// Sleep necessary to allow backend to finish insert procedure and propagate tile update, otherwise range query might not return the inserted item
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// RANGE QUERY, response is a JSON Array
		System.out.println("\n\n**** Range Query ****");
		double sw_lat = 0.0; // south west latitude in degree 
		double sw_lon = 0.0; // south west longitude in degree 
		double boxSize = 0.5; // box size in degree
		String response = ogbTestClient.rangeQuery(token, cid, sw_lat, sw_lon, boxSize);
		System.out.println("query response: " + response);

		// DELETE OBJECT
		if(oid!=null)
		{
			System.out.println("\n\n**** Delete object ****");
			if (ogbTestClient.deleteObject(token, oid)) {
				System.out.println("Delete of " + oid+" OK");
			};
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		

		// RANGE QUERY, response is a JSON Array
		System.out.println("\n\n**** Range Query ****");
		double sw_lat2 = 0.0; // south west latitude in degree 
		double sw_lon2 = 0.0; // south west longitude in degree 
		double boxSize2 = 0.5; // box size in degree
		String response2 = ogbTestClient.rangeQuery(token, cid, sw_lat2, sw_lon2, boxSize2);
		System.out.println("query response: " + response2);
		

		// INSERTION OF MULTI-POINT OBJECT 
		System.out.println("\n\n**** MultiPoint geoJSON Insert ****\n");

		// multipoint coordinates
		ArrayList<double[]> mcoordinates = new ArrayList<double[]>();
		double lat_point1 = 0.01;
		double lon_point1 = 0.01;
		double lat_point2 = 0.02;
		double lon_point2 = 0.02;
		mcoordinates.add(new double[] { lat_point1, lon_point1 }); //point 1
		mcoordinates.add(new double[]{lat_point2, lon_point2 }); // point 2
		// point properties
		HashMap<String,String> mprop = new HashMap<String,String>();
		mprop.put("prop100", "value100");	
		mprop.put("prop200", "value200");

		// DB insertion, response is the object identifier (oid)
		String moid = ogbTestClient.addMultiPoint(token,cid, mprop, mcoordinates);
		if (moid!=null) {
			System.out.println("Insertion ok, oid : "+moid);
		} else {
			return;
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		// RANGE QUERY, response is a JSON Array
		System.out.println("\n\n**** Range Query ****\n");
		double sw_lat3 = 0.0; // south west latitude in degree 
		double sw_lon3 = 0.0; // south west longitude in degree 
		double boxSize3 = 0.5; // box size in degree
		String response3 = ogbTestClient.rangeQuery(token, cid, sw_lat3, sw_lon3, boxSize3);
		System.out.println("query response: " + response3);

		// DELETE OBJECT
		System.out.println("\n\n**** Delete object ****\n");
		if (ogbTestClient.deleteObject(token, moid)) {
			System.out.println("Delete of " + moid+" OK");
		};

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// RANGE QUERY, response is a JSON Array
		System.out.println("\n\n**** Range Query ****\n");
		double sw_lat4 = 0.0; // south west latitude in degree 
		double sw_lon4 = 0.0; // south west longitude in degree 
		double boxSize4 = 0.5; // box size in degree
		String response4 = ogbTestClient.rangeQuery(token, cid, sw_lat4, sw_lon4, boxSize4);
		System.out.println("query response: " + response4);

	}
}
