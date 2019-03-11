package com.ogb.auc.controllers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ogb.auc.domains.User;
import com.ogb.auc.entity.ErrorResponse;
import com.ogb.auc.ndnsec.NDNSecManager;
import com.ogb.auc.repositories.UserRepository;
import com.ogb.auc.sqlite.SqliteManager;


@RestController
public class UserController 
{
	@Autowired
	private UserRepository userRepo;
	
	private NDNSecManager  ndnSecManager = NDNSecManager.getInstance();
	
	
//	@CrossOrigin(origins="*")
//	@RequestMapping(method = RequestMethod.POST, value="/AUC/user/find", consumes="application/json", produces="application/json")
//    public String findUser(@RequestBody Map<String,String> data) {
//		
//		System.out.println("findUser "+data.toString());
//		
//		String username = NDNSecManager.idPrefix + "/" + data.get("tenantName") + "/" + data.get("userName");
//		ArrayList<String> results = SqliteManager.getInstance().findUser(username);
//		System.out.println(results);
//		
//		return results.get(0);
//	}
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/AUC/user/register", consumes="application/json", produces="application/json")
    public Object registerUser(HttpServletResponse response, @RequestBody Map<String,String> data) {
		String userName   = data.get("userName");
		String tenantName = data.get("tenantName");
		String password   = data.get("password");
		String permissionType = data.get("permission");
		
		System.out.println("registerUser " + data);
		
		if ( userName == null)
		{
			System.out.println("Error! username not provided!");
			response.setStatus(410);
			return new ErrorResponse("Error! username not provided!", 410);
		}
		
		if ( tenantName == null)
		{
			System.out.println("Error! tenantID not provided!");
			response.setStatus(411);
			return new ErrorResponse("Error! tenantID not provided!", 411);
		}
		
		if ( password == null)
		{
			System.out.println("Error! password not provided!");
			response.setStatus(412);
			return new ErrorResponse("Error! password not provided!", 412);
		}
			
		if (!(permissionType.equals("r") ||  permissionType.equals("rw") || permissionType.equals("admin")))
		{
			System.out.println("Error! Invalid permission type!");
			response.setStatus(413);
			return new ErrorResponse("Error! Invalid permission type!", 413);
		}
		
		//Find user into local database 
		User user = userRepo.findByUserID(tenantName+"/"+userName);
		System.out.println("userName: "+userName+" , tenantName: "+tenantName+" , pwd: "+password + " , permission: " + permissionType);
		if (user != null) {
			response.setStatus(400);
			return new ErrorResponse("Error! User already exist!", 400);
		}
		
		HashMap<String, byte[]> keyData = ndnSecManager.generateKeyAndCertificate(tenantName, userName, permissionType);
		if (keyData == null) {
			response.setStatus(401);
			return new ErrorResponse("Error! Unable to create key and certificate!", 401);
		}
		
		byte[] privateKey = keyData.get("pri");
		byte[] publicKey = keyData.get("pub");
		if (privateKey == null || publicKey == null) {
			response.setStatus(401);
			return new ErrorResponse("Error! Unable to create key and certificate!", 401);
		}
		
		//Retrieve the keyLocator from sql DB
		String keyLocator = SqliteManager.getInstance().findKeyLocatorOfUser(NDNSecManager.idPrefix + "/" + tenantName + "/" + userName + "/" + permissionType).get(0);
		
		//Save the user registered
		user = new User();
		user.setUserID(tenantName+"/"+userName);
		user.setPassword(password);
		user.setPrivateKey(privateKey);
		user.setPublicKey(publicKey);
		user.setKeyLocator(keyLocator);
		user.setPermissionType(permissionType);
		userRepo.save(user);
		
		response.setStatus(201);
		return user;
	}
	
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/AUC/user/login", consumes="application/json", produces="application/json")
    public Object login(HttpServletResponse response, @RequestBody Map<String, String>params) {
		System.out.println("LOGIN: " + params);
		
		User user = userRepo.findByUserID(params.get("tenantName")+"/"+params.get("userName"));
		if (user == null) {
			response.setStatus(402);
			return new ErrorResponse("Error! User not found!", 404);
		}
		
		String password = params.get("password");
		if (password == null || password.length() <= 0) {
			response.setStatus(403);
			return new ErrorResponse("Error! No password provided!", 403);
		}
		
		if (user.getPassword().compareTo(password) != 0) {
			response.setStatus(400);
			return new ErrorResponse("Error! Wrong user name or password!", 400);
		}
		
		user.setToken(User.generateToken());
		userRepo.save(user);
		
		response.setStatus(200);
		return user;
	}
	
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/AUC/user/check-token", consumes="application/json", produces="application/json")
    public Object checkToken(HttpServletResponse response, @RequestBody Map<String, String>params) {
		
		String token = params.get("token");
		System.out.println("checkToken: " + token );
		
		User user = userRepo.findByToken(token);
		if (user == null)
		{
			response.setStatus(400);
			return "Error on CheckToken!";
		}
		
		response.setStatus(200);
		return user;
	}
	
	
}
