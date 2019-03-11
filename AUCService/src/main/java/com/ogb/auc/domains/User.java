package com.ogb.auc.domains;


import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.common.hash.Hashing;


@Entity
@Table(name="user")
public class User {

	@Id
	@Column(name="user_id")
	private String userID;
	
	@Column(name="token")
	private String token;
	
	@Column(name="password")
	private String password;
	
	@Column(name="private_key", columnDefinition="LONGBLOB")
	private byte[] privateKey;

	@Column(name="public_key", columnDefinition="LONGBLOB")
	private byte[] publicKey;
	
	@Column(name="key_locator")
	private String keyLocator;
	
	@Column(name="permission_type")
	private String permissionType;
	
	
	public User() {
		super();
	}
	
	
	public static String generateToken(int lenght) {
		String        charset = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
		Random        rand    = new SecureRandom();
		StringBuilder result  = new StringBuilder();
		
		for (int i = 0; i < lenght; i++) {
			result.append(charset.charAt(rand.nextInt(charset.length()-1)));
		}
		
		return Hashing.sha512().hashString(result, StandardCharsets.UTF_8).toString();
	}
	public static String generateToken() {
		return generateToken(256);
	}

	 
	public String getToken() {
		return token;
	}
	public String getUserID() {
		return userID;
	}
	public String getPassword() {
		return password;
	}
	public byte[] getPrivateKey() {
		return privateKey;
	}
	public byte[] getPublicKey() {
		return publicKey;
	}
	public String getKeyLocator() {
		return keyLocator;
	}
	public String getPermissionType() {
		return permissionType;
	}


	public void setToken(String token) {
		this.token = token;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public void setPrivateKey(byte[] privateKey) {
		this.privateKey = privateKey;
	}
	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}
	public void setKeyLocator(String keyLocator) {
		this.keyLocator = keyLocator;
	}
	public void setPermissionType(String permissionType) {
		this.permissionType = permissionType;
	}


}
