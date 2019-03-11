package com.ogb.fes.ndn;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;

import net.named_data.jndn.Name;
import net.named_data.jndn.Name.Component;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.Common;
import net.named_data.jndn.security.SecurityException;


public class NDNKeychainManager 
{

	public static KeyChain createKeychain(Name keyLocator, byte[] privateKey, byte[] publicKey) throws IOException, SecurityException {
		
		MemoryIdentityStorage   memoryIdentityStorage   = new MemoryIdentityStorage();
		MemoryPrivateKeyStorage memoryPrivateKeyStorage = new MemoryPrivateKeyStorage();
		IdentityManager         identityManager         = new IdentityManager(memoryIdentityStorage, memoryPrivateKeyStorage);		
		KeyChain                keyChain                = new KeyChain(identityManager);

		//System.out.println(DateTime.currentTime()+"NDNKeychainManager - Certificate name: " + keyLocator);
		Name keyName = getKeyNameFromCertificate(keyLocator);
		//System.out.println(DateTime.currentTime()+"NDNKeychainManager - KEY name: " + keyName);
		
		//TODO RETRIEVE PUBLIC KEY FROM fesDB
		ByteBuffer pubKey = toBufferFromByte(Common.base64Decode(new String(publicKey)));
		ByteBuffer priKey = toBufferFromByte(Common.base64Decode(new String(privateKey)));
		
		Name identity = getIdentityNameFromCertificate(keyLocator);
		memoryIdentityStorage.addIdentity(identity);
		memoryIdentityStorage.addKey(keyName, KeyType.RSA, new Blob(pubKey, false));
		memoryPrivateKeyStorage.setKeyPairForKeyName(keyName, KeyType.RSA, pubKey, priKey);
		
		return keyChain;
	}
	
	private static ByteBuffer toBufferFromByte(byte[] array) {
		
		ByteBuffer result = ByteBuffer.allocate(array.length);
		for (int i = 0; i < array.length; ++i)
			result.put((byte) (array[i] & 0xff));
		result.flip();
		
		return result;
	}
	
	public static byte[] keyFileReader(String filePath) {

		StringBuilder contents = new StringBuilder();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(filePath));

			try {
				String line = null;
				while ((line = reader.readLine()) != null)
					contents.append(line);
			} 
			finally {
				reader.close();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return Common.base64Decode(contents.toString());
	}
	
	public static Name getKeyNameFromCertificate(Name certname) {
		
		Name result      = new Name();
		int  keyIndex    = -1;
		int  idCertIndex = -1;
		
		for (int i = 0; i< certname.size(); i++) {
			Component component = certname.get(i);
			if (component.toEscapedString().equals("KEY"))
				keyIndex = i;
			
			if (component.toEscapedString().equals("ID-CERT"))
				idCertIndex = i;
		}
		
		if (keyIndex != -1 && idCertIndex != -1) {		
			result.append(certname.getSubName(0, keyIndex)).append(certname.getSubName(keyIndex+1, idCertIndex-keyIndex-1));
		}
		
		return result;
	}
	
	public static Name getIdentityNameFromCertificate(Name certname) {
		Name result      = new Name();
		int  keyIndex    = -1;
		int  idCertIndex = -1;
		
		for (int i = 0; i< certname.size(); i++) {
			Component component = certname.get(i);
			if (component.toEscapedString().equals("KEY"))
				keyIndex = i;
			
			if (component.toEscapedString().equals("ID-CERT"))
				idCertIndex = i;
		}
		
		if (keyIndex != -1 && idCertIndex != -1) {
			result.append(certname.getSubName(0, keyIndex)).append(certname.getSubName(keyIndex+1, idCertIndex-keyIndex-2));
		}
		
		return result;
	}

}
