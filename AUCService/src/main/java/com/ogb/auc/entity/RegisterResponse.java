package com.ogb.auc.entity;


import com.ogb.auc.domains.User;


public class RegisterResponse {
	public String userID;
	public String token;
	public String privateKey;
	
	
	public RegisterResponse() {
		super();
		
		userID       = "";
		token        = "";
		privateKey   = "";
	}
	
	public RegisterResponse(User user) {
		this();
		
		this.userID     = user.getUserID();
		this.token      = user.getToken();
		this.privateKey = new String(user.getPrivateKey());
	}

	
	
	public String getUserID() {
		return userID;
	}
	public String getToken() {
		return token;
	}
	public String getPrivateKey() {
		return privateKey;
	}

	
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}
}
