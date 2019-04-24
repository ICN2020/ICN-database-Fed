## JAVA Application Programming Interface

This is a java library developed for OpenGeoBase frontend.

###Installation guide

To use this library in your project, export OgbJavaLibrary/src/com/icn2020/ogb/clientlib/OgbClientLib.java as runnable jar file and add it to the build path of your project. Then use 

        import com.ogb.clientlib.*;

## API method description

### OpenGeoBase library object constructor

Create a new OgbClientLib with optional settings to manage communication with the OGB FrontEndServer.


**OgbClientLib(String serverURL)**

* **Parameters:**
     
    * String serverURL : URL of the FrontEndServer (ip:port)
    
    
The following code shows an example of object allocation:


    OgbClientLib ogbTestClient = new OgbClientLib(serverURL);


### Login

This method allows the user to log in.

_String_ **login**(String userId, String tenant, String password)

* **Parameters:**

    * _String_ userId : the username of the user that attempts to login
    * _String_ tenant : the tenant responsible for the user
    * _String_ password : the password of the user
    
    
* **Response:** 

    * (_String_) secure token to be used for next operations with the FrontEndServer. This token identify the user. keep it secret..

The following code shows an example of login:

    String uid = "myUserID";
    String tid = "myTenantID";
    String pwd = "myPassword";
    String token = ogbTestClient.login(uid, tid, pwd);
    
### Point insertion

This method allows the user to insert a **Point** GeoJSON object.
    
_String_ **addPoint** (String token, String cid, HashMap < String, String > propertiesMap, final double[ ] location)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _HashMap < String, String >_ propertiesMap : set of geo-json properties, hashmap < string properties, string property_value >
     * _double[ ]_ location : [latitude longitude] double array
     
* **Response:** 

    * (_String_) object identifier (OID) of inserted GeoJSON
    
* **Example**
        
The following code shows an example of **Point** GeoJSON object with additional properties insertion:

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


### MultiPoint insertion

This method allows the user to insert a **MultiPoint** GeoJSON object.

_String_ **addMultiPoint**(String token, String cid, HashMap < String, String > propertiesMap, ArrayList < double[ ] > coordinates)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _HashMap < String, String >_ propertiesMap : set of geo-json properties, hashmap <string properties, string property_value>
     * _ArrayList < double[ ] >_ location : array list of [latitude longitude] double arrays
    

* **Response:** 

    * (_String_) object identifier (OID) of inserted GeoJSON
    
The following code shows an example of **MultiPoint** GeoJSON object with additional properties insertion:

        // multipoint coordinates
        ArrayList<double[]> mcoordinates = new ArrayList<double[]>();
        double lat_point1 = 0.01;	//point 1 latitude
        double lon_point1 = 0.01;	//point 1 longitude
        double lat_point2 = 0.02;	//point 2 latitude
        double lon_point2 = 0.02;	//point 2 longitude
        mcoordinates.add(new double[] { lat_point1, lon_point1 }); //point 1
        mcoordinates.add(new double[]{lat_point2, lon_point2 }); // point 2
        // point properties
        HashMap<String,String> mprop = new HashMap<String,String>();
        mprop.put("prop100", "value100");	
        mprop.put("prop200", "value200");

        // DB insertion, response is the object identifier (oid)
        String moid = ogbTestClient.addMultiPoint(token,cid, mprop, mcoordinates);
    

### Polygon insertion

This method allows the user to insert a **Polygon** GeoJSON object.

_String_ **addPolygon**(String token, String cid, HashMap < String, String > propertiesMap, ArrayList < double[ ] > coordinates)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _HashMap < String, String >_ propertiesMap : set of geo-json properties, hashmap <string properties, string property_value>
     * _ArrayList < double[ ] >_ location : array list of [latitude longitude] double arrays

* **Response:** 

    * (_String_) object identifier (OID) of inserted GeoJSON

The following code shows an example of **Polygon** GeoJSON object with additional properties insertion:
     
        ArrayList<double[]> pcoordinates = new ArrayList<double[]>();
        double polygon_lat_point1 = 0.00;   //point 1 latitude
        double polygon_lon_point1 = 0.00;   //point 1 longitude
        double polygon_lat_point2 = 0.11;   //point 2 latitude
        double polygon_lon_point2 = 0.11;   //point 2 longitude
        double polygon_lat_point3 = 0.11;   //point 3 latitude
        double polygon_lon_point3 = 0.00;   //point 3 longitude
        pcoordinates.add(new double[] { polygon_lat_point1, polygon_lon_point1 }); 
        pcoordinates.add(new double[] { polygon_lat_point2, polygon_lon_point2 });
        pcoordinates.add(new double[] { polygon_lat_point3, polygon_lon_point3});
        // point properties
        HashMap<String,String> polygonProp = new HashMap<String,String>();
        polygonProp.put("URL", "http:/myurl.it/gtfz.zip");	
        polygonProp.put("Type", "GTFS");
        polygonProp.put("Provider", "ICN2020 Project");

        // db insertion, response is the object identifier (oid)
        String poid = ogbTestClient.addPolygon(token,cid, polygonProp, pcoordinates);

### GeoJSON insertion

This method allows the user to insert a GeoJSON object. Supported GeoJSON shape are Point, MultiPoint and Polygon.

_String_ **addGeoJSON**(String token, String cid, String geoJSON)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _String_ geoJSON : string representing geoJSON object
     
     
The following text shows an example of **Point** GeoJSON object with additional properties:

    
    {
        "geometry": {
            "coordinates": [12.28,41.63],       // coordinates in format longitude, latitude
            "type": "Point"                     // supported geometry types are Point, Polygon, MultiPoint
        },
        "type": "Feature",
        "properties": {                         // additional properties
            "train_speed" : "10"
            "delay" : "0"
        }
    }


Following text shows an example of **MultiPoint** GeoJSON object with additional properties:
    
    {
        "geometry": {
            "coordinates": [            //For type "MultiPoint", the "coordinates" member must be an array of positions.
                [12.536976,41.950308],
                [12.459226,41.937373],
                [12.363410,41.914593],
            ],
            "type": "MultiPoint"
        },
        "type": "Feature",
        "properties": {
            "Provider", "ICN2020 Project",
            "Type", "GTFS",
            "URL" : "http:/myurl.it/gtfs.zip"
        }
    }


Following text shows an example of **Polygon** GeoJSON object with additional properties:

    {
        "geometry" : {
            "type" : "Polygon",
            "coordinates" :
                [
                    [
                        [0.0, 0.0],         //Polygon point 1 coordinate [latitude, longitude]
                        [0.11, 0.11],       //Polygon point 2 coordinate
                        [0.0, 0.11]         //Polygon point 3: if needed, the API method closes Polygon automatically connecting last and first points
                    ]
                ]
            },
        "type":"Feature",
        "properties" : {            // object properties
            "name": "null island"
        }
    }

* **Response:** 

    * (_String_) object identifier (OID) of inserted GeoJSON


### Object query

This method allows the user to retrive a GeoJSON by its ObjectID.

_String_ **queryObject**(String token, String cid, String oid)

* **Parameters:**

     * _String_ token : the authorization token of the user
     * _String_ cid : collection identifier
     * _String_ oid : unique identifier of the geoJSON object
    
    
* **Response:** 
 
    * (_String_) the requested GeoJSON object as string

    
The following code shows an example of object query:

        String oid = "/OGB/000/000/21/00/GPS_id/GEOJSON/icn2020/testCID/test/q37871900e8w1r69";
        System.out.println("\n\n**** Object Query ****");
        String response1 = ogbTestClient.queryObject(token, cid, oid);
        System.out.println("query response: " + response1);
        
        
### Range query

This method allows the user to find GeoJSON objects within a spcified **square** area. This method perform GeoJSON spatial query using square **box** shape and $geoIntersects geospatial operator. For more information visit  https://docs.mongodb.com/manual/reference/operator/query/geoIntersects/

_String_ **rangeQuery**(String token, String cid, double sw_lat, double sw_lon, double boxSize)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _double_ sw_lat : south west latitude
     * _double_ sw_lon : south west longitude
     * _double_ boxSize : box edge size in degree

     
* **Response:** 
 
    * (_String_) a JSON array of GeoJSON objects within the specified area (if no GeoJSON is present in the area the method returns an empty array).

The following code shows an example of query:

        double sw_lat = 0.0; // south west latitude in degree 
        double sw_lon = 0.0; // south west longitude in degree 
        double boxSize = 0.5; // box size in degree
        String response = ogbTestClient.rangeQuery(token, cid, sw_lat, sw_lon, boxSize);
        System.out.println("query response: " + response);

### Range query Box

This method allows the user to find GeoJSON objects within a spcified area. This method perform GeoJSON spatial query using **box** rectangular shape and $geoIntersects geospatial operator. For more information visit  https://docs.mongodb.com/manual/reference/operator/query/geoIntersects/

_String_ **rangeQueryBox**(String token, String cid, double sw_lat, double sw_lon, double ne_lat, double ne_lon)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _double_ sw_lat : south west latitude
     * _double_ sw_lon : south west longitude
     * _double_ ne_lat : north east latitude
     * _double_ ne_lon : north east longitude

     
* **Response:** 
 
    * (_String_) a JSON array of GeoJSON objects within the specified area (if no GeoJSON is present in the area the method returns an empty array).

The following code shows an example of box query:

        double sw_lat = 0.0; // south west latitude in degree 
        double sw_lon = 0.0; // south west longitude in degree
        double ne_lat = 0.5; // north east latitude in degree 
        double ne_lon = 0.5; // north east longitude in degree
        String response = ogbTestClient.rangeQueryBox(token, cid, sw_lat, sw_lon, ne_lat, ne_lon);
        System.out.println("query response: " + response);

### Range query Polygon

This method allows the user to find GeoJSON objects within a spcified area. This method perform GeoJSON spatial query using **Polygon** shape and $geoIntersects geospatial operator. For more information visit  https://docs.mongodb.com/manual/reference/operator/query/geoIntersects/

_String_ **rangeQueryPolygon**(String token, String cid, ArrayList <  ArrayList < double [ ] > > coordinates)

* **Parameters:**

     * _String_ token : the authorization token of the user  
     * _String_ cid : collection identifier
     * _ArrayList < ArrayList < double [ ] > >_ coordinates : ArrayList of ArrayList<[latitude longitude]> double arrays, at the moment supporting only polygon without holes (for more information visit http://geojson.org/geojson-spec.html#id4)

     
* **Response:** 
 
    * (_String_) a JSON array of GeoJSON objects within the specified area (if no GeoJSON is present in the area the method returns an empty array).


The following code shows an example of Polygon query:
    
        ArrayList<ArrayList<double[]>> polygon = new ArrayList<>();
        ArrayList<double[]> subPolygon = new ArrayList<>(); 
        double queryPol_lat_point1 = 0.00;	//point 1 latitude in degree
        double queryPol_lon_point1 = 0.00;  //point 1 longitude in degree
        double queryPol_lat_point2 = 0.15;	//point 2 latitude in degree
        double queryPol_lon_point2 = 0.15;	//point 2 longitude in degree
        double queryPol_lat_point3 = 0.15;	//point 3 latitude in degree
        double queryPol_lon_point3 = 0.00;	//point 3 longitude in degree
        subPolygon.add(new double[] {queryPol_lat_point1,queryPol_lon_point1});
        subPolygon.add(new double[] {queryPol_lat_point2,queryPol_lon_point2});
        subPolygon.add(new double[] {queryPol_lat_point3,queryPol_lon_point3});
        polygon.add(subPolygon);
        String response = ogbTestClient.rangeQueryPolygon(token, cid, polygon);
        System.out.println("query response: " + response);

### Object removal

This method allows the user to delete a GeoJSON object
    
_boolean_ **deleteObject**(String token, String oid)

* **Parameters:**

     * _String_ token : the authorization token of the user
     * _String_ cid : collection identifier
     * _String_ oid : the object identifier (OID) of the GeoJSON to remove
    

* **Response:**
    * (_Boolean_) true/false if success/failure


The following code shows an example of object removal:

        String oid = "/OGB/000/000/21/00/GPS_id/GEOJSON/icn2020/testCID/test/q37871900e8w1r69";
        boolean removalStatus = ogbTestClient.deleteObject(token, oid);
