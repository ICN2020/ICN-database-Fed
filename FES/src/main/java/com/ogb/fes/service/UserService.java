package com.ogb.fes.service;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ogb.fes.domain.User;
import com.ogb.fes.domain.UserRepository;
import com.ogb.fes.domain.ErrorResponse;

import com.ogb.fes.net.NetManager;


@RestController
public class UserService {

	@Autowired
	private UserRepository userRepo;
	

	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/user/register", consumes="application/json", produces="application/json")
    public Object registerUser(HttpServletResponse response, @RequestBody Map<String, Object>params) {
		
		System.out.println("RegisterUser with params: " + params);
		
		Map<String, Object> result = registerOnAUCServer(params);
		
		System.out.println("RegisterUser AUC result: " + result);
		
		if (result != null) {
			response.setStatus(200);
			return result;
		}
		
		response.setStatus(407);
		return new ErrorResponse(407, "Register Failed");
	}
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/user/login", consumes="application/json", produces="application/json")
    public Object login(HttpServletResponse response, @RequestBody Map<String, Object>params) {
		System.out.println("LOGIN: " + params );
		
		User aucUser = loginOnAUCServer(params);
		if (aucUser == null) {
			response.setStatus(407);
			return new ErrorResponse(407, "Error on login: Wrong credential provided!");
		}
		
		userRepo.save(aucUser);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("token", aucUser.getToken());
		return result;
	}
	
	
	private User loginOnAUCServer(Map<String, Object> params) {
		Map<String, Object> aucUser = new NetManager().sendLogin(params);
		
		if (aucUser == null)
			return null;
		
		return new User(aucUser);
	}
	
	private  Map<String, Object> registerOnAUCServer(Map<String, Object> params) {
		return new NetManager().sendRegister(params);
	}
}
