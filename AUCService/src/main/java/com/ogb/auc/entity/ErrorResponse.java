package com.ogb.auc.entity;


public class ErrorResponse {
	public int    code;
	public String message;
	
	
	public ErrorResponse() {
		this.message = "";
		this.code    = 0;
	}
	
	public ErrorResponse(String message) {
		this.message = message;
	}
	
	public ErrorResponse(String message, int code) {
		this.message = message;
		this.code    = code;
	}
	
	
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	
	public String getMessage() {
		return message;
	}
	
	public int getCode() {
		return code;
	}
}
