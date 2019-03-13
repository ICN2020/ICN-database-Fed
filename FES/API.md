## HTTP Application Programming Interface 

### Register 

This method allows to register a new user into the OpenGeoBase system.

* **URL:** /OGB/user/register

* **Method:** POST

* **consumes:** application/json
* **produces:** application/json

* **POST body:** a JSON object with the following keys:values :
    
    userName : [the username of the new user]
    
    password : [the password of the new user]
    
    tenantName : [the tenant responsible for the new user]
    
    permission : [the permission type for the new user ("r" or "rw")]. 
    
* **Response:** an empty response with status code 200 in case of success, an error otherwise with following code:
    * 407, "Register Failed"

### Login

This method allows the user to log in.

* **URL:** /OGB/user/login

* **Method:** POST

* **consumes:** application/json
* **produces:** application/json

* **POST body:** a JSON object with the following keys:values :

    userName : [the username of the user that attempts to login]

    password : [the password of the user]
    
    tenantName : [the tenant responsible for the user]

* **Response:** the users's authentication token in case of success, an error otherwise with following code:
    * 407,"Error on login: Wrong credential provided!"

### Object query

This method allows the user to retrive a GeoJSON by its ObjectID. Collection id must be the last path component in the method entrypoint

* **URL:** /OGB/query-service/element/{cid}

* **Method:** POST

* **consumes:** application/json
* **produces:** application/json

* **POST header:** an HashMap<String, String> with the following key:

    Authorization : [the authorization token of the user retrieved by login procedure]

* **POST body:** a JSON object with the following key:value :

    oid : [the object identifier (OID) of the requested GeoJSON]

* **Response:** the requested GeoJSON object as string if it exists into database, an error otherwise with following codes:
    * 420, "Invalid authorization token"
    * 431, "Invalid oid in GeoJSON!"

### Range query

This method allows the user to find GeoJSON objects within a spcified area. Collection id must be the last path component in the method entrypoint

* **URL:** /OGB/query-service/{cid}

* **Method:** POST

* **consumes:** application/json
* **produces:** application/json

* **POST header:** an HashMap<String, String> with the following key:value :
    
    Authorization : [the authorization token of the user]
    
* **POST body:** a JSON object that describes the geospatial query. Currently only "$geoIntersects" geospatial operator and $geometry.type {Box, Polygon} are supported. More information at https://docs.mongodb.com/manual/reference/operator/query/geoIntersects/

The following text shows an example of range query:

    {
        "geometry": {
            "$geoIntersects": {
                "$geometry": {
                    "type": "Box" ,            // supported geometry types are "Box" and "Polygon"
                    "coordinates":[
                        [12.28,41.63],         // bottom left coordinates in format longitude, latitude
                        [12.73,42.02]          // upper right coordinates in format longitude, latitude
                    ]
                }
            }
        }
    }

* **Response:** an array of GeoJSON objects (as string) within the specified area (if no GeoJSON is present in the area the method returns an empty array). In case of failure the following error codes are returned:
    * 420, "Invalid authorization token"
    * 430, "Invalid query params"
    * 440, "Requested area size exceeds the maximum limit"

### Object insertion

This method allows the user to insert a GeoJSON object. Collection id must be the last path component in the method entrypoint

* **URL:** /OGB/content/insert/{cid}

* **Method:** POST

* **consumes:** application/json
* **produces:** application/json

* **POST header:** an HashMap<String, String> with the following key:value :

    Authorization : [the authorization token of the user]
    
* **POST body:** a JSON object that describes the geospatial structure to be inserted in OGB. Supported geometry types are the following: Point, Polygon, MultiPoint. Geometric objects with additional properties are Feature objects.


Following text shows an example of **Point** GeoJSON object with additional properties:

    
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
            "Provider", "Bonvoyage Project",
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

* **Response:** the method returns a string representing the Object Identifier (OID) of the inserted GeoJSON objetc, an error message otherwise with following codes:
    * 420, "Invalid authorization token"
    * 421, "Invalid permission type "
    * 407, "Failed to upload Content!"
    * 407, "Error on upload Content! Empty Content!"


### Object removal

This method allows the user to delete a GeoJSON object

* **URL:** /OGB/content/delete

* **Method:** POST

* **consumes:** application/json
* **produces:** application/json

* **POST header:** an HashMap<String, String> with the following key:value :

    Authorization : [the authorization token of the user]
    
* **POST body:** a JSON object with the following key:value :

    oid : [the object identifier (OID) of the GeoJSON to remove]

* **Response:** the method returns an empty response with status code 200 in case of success, an error otherwise with following codes:
    * 420, "Invalid authorization token"
    * 431, "Invalid oid in GeoJSON!"
    * 403, "User unauthorized!"
    * 421, "Security issues: User grant not retrieved!"
