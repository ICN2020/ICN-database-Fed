package com.ogb.auc.entity;


import com.ogb.auc.domains.User;


public class LoginResponse {
	User user;
	
	public LoginResponse() {
		
	}
	
	
	public LoginResponse(User user) {
		this.user = user;
	}
	
	
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
}
