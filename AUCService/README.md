# AUCService
This is the source code of the Authentication Server developed for the European Project "Bonvoyage"

##HOW TO USE
- Clone the AUCService git

- Copy the FrontEndServer artifact in local maven repository:
  > cp repo/com/ogb/1.0/FrontEndServer-1.0.java ~/.m2/repository/com/ogb/FrontEndServer/1.0/


- Create a DB using mysql named _*aucDB*_

- Edit the file "src/main/resources/application.properties"

  > spring.datasource.username = _your_mysql_username_ 

  > spring.datasource.password = _your_mysql_password_

- Compile the project with:
  > mvn package -DskipTests

- Run the AUCService:
  > java -jar target/AUCService.jar


##API
###/AUC/user/register
This API allow to register a new user in to the AUC Server.  

method: POST
consumes: application/json
produces: application/json

POST parameters: a JSON object with the following keys:
	_username_ : the username of the new user
	_password_ : the password of the new user
	_tenantName_ : the tenant responsible for the new user
	_permission_ : the permission type for the new user ("r" or "rw"). Only the "rw" users can insert data into the system, all users can query data from the system.
	
Result: the API return the user if the registration is successful, an error otherwise.


###/AUC/user/login
This API allow the user to log in.  

method: POST
consumes: application/json
produces: application/json

POST parameters: a JSON object with the following keys:
	_username_ : the username of the user that attempt to login
	_password_ : the password of the user that attempt to login
	_tenantName_ : the tenant responsible for the user that attempt to login

Result: the API return the user, including his authentication token, if the login is successful, an error otherwise.


###/AUC/user/check-token
This API allow to retrive an user from his authenticationn token.  

method: POST
consumes: application/json
produces: application/json

POST parameters: a JSON object with the following keys:
	_token_ : the username of the user that attempt to login
	_password_ : the password of the user that attempt to login
	_tenantName_ : the tenant responsible for the user that attempt to login
 
