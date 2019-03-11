package com.ogb.auc.ndnsec;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import com.ogb.fes.filesystem.FileReader;

import net.named_data.jndn.util.Common;


public class NDNSecManager {
	
	public static String NDN_PATH;
	public static String NDN_MAP_FILE_PATH;
	public static String idPrefix; 

	private HashMap<String, String> mappingFileTable; 
	
	private static NDNSecManager sharedInstance = null;
	
	
	private NDNSecManager()
	{
		
	}
	 
	public static NDNSecManager getInstance()
	{
		if (sharedInstance == null)
			sharedInstance = new NDNSecManager();
		
		return sharedInstance;
	}
	

	
	private HashMap<String, String> getMappingFile(boolean reload) {
		
		if (reload == false && mappingFileTable != null)
			return mappingFileTable;
			
		FileReader fileReader = new FileReader(NDNSecManager.NDN_MAP_FILE_PATH);
		String[]   mapping    = fileReader.getAllLines();
		
		mappingFileTable = new HashMap<String, String>();
		
		for (int i = 0; i < mapping.length; i++) {
			String[] line = mapping[i].split(" ");
			mappingFileTable.put(line[0], line[1]);
		}
		
		return mappingFileTable;
	}
	
	private String findInMappingFile(String prefix) {
		
		//if (mappingFileTable == null)
			getMappingFile(true);
		
		for (String key : mappingFileTable.keySet()) {
			if(key.startsWith(prefix)) {
				String value = mappingFileTable.get(key);
				value = value.replaceAll("\n", "");
				value = value.replaceAll("\r", "");
				value = value.replaceAll(" ", "");
				if(new File(value + ".pri").exists() == true)
					return value;
				
			}
		}
		
		return null;
	}
	
	public byte[] getPrivateKeyFromUsername(String tenant, String username, String permissionType) {
		
		return getPrivateKeyFromUserIdentifier(idPrefix+"/"+ tenant + "/" + username + "/" + permissionType);
	}
	public byte[] getPrivateKeyFromUserIdentifier(String userIdentifier) {
		
		return getKeyFromUserIdentifier(userIdentifier, "pri");
	}
	public byte[] getPublicKeyFromUsername(String tenant, String username, String permissionType) {
		
		return getPublicKeyFromUserIentifier(idPrefix + "/" + tenant + "/" + username + "/" + permissionType);
	}
	public byte[] getPublicKeyFromUserIentifier(String userIdentifier) {
		
		return getKeyFromUserIdentifier(userIdentifier, "pub");
	}	
	public byte[] getKeyFromUserIdentifier(String userIdentifier, String keyType) {
		
		String        filePath = findInMappingFile(userIdentifier)+"."+keyType;
		StringBuilder contents = new StringBuilder();
		
		try {
			BufferedReader reader = new BufferedReader(new java.io.FileReader(filePath));

			try {
				String line = null;
				while ((line = reader.readLine()) != null)
					contents.append(line);
			} 
			finally {
				reader.close();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return Common.base64Decode(contents.toString());
	}
	
	public HashMap<String,byte[]> generateKeyAndCertificate(String tenantName, String userName, String permissionType) {
		
		boolean success        = false;
		boolean generation = generatePrivateKey(userName, tenantName, permissionType);
		if (generation == true)
		{
			byte[]  privateKeyData = getPrivateKeyFromUsername(tenantName, userName, permissionType);
			byte[]  publicKeyData = getPublicKeyFromUsername(tenantName, userName, permissionType);
			
		
			if (privateKeyData != null) 
			{
				String certFileName = tenantName + "_" + userName + "_" + permissionType + ".req";
				success = generateCertificate(tenantName, userName, certFileName, tenantName, permissionType);
	
				
				success &= installCertificate(tenantName + "_" + userName + "_" + permissionType + ".cert");
			}
			
			if (success == true)
			{
				HashMap<String , byte[]> result = new HashMap<>();
				result.put("pri", privateKeyData);
				result.put("pub", publicKeyData);
				return result;
			}
			//TODO must implements rollback system 
		}
		return null;
	}
	
	private boolean generatePrivateKey(String userName, String tenantName, String permissionType) {

		String   homePath = System.getProperty("user.home") + "/cert/";
		if (new File(homePath).exists() == false)
			new File(homePath).mkdir();
		
		String   command  = "ndnsec-keygen -n "+idPrefix+"/"+tenantName+"/"+userName+"/"+permissionType+
											  " > "+homePath+tenantName+"_"+userName+"_"+permissionType+".req";
		System.out.println(command);
		String[] args     = new String[] {"/bin/bash", "-c", command};
		
		try {
			Process proc    = new ProcessBuilder(args).start();
			boolean success = waitProcessUntilDone(proc, true);
			
			System.out.println("generatePrivateKey success: "+success);
			
			return success;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean generateCertificate(String tenantName, String userName, String certFileName, String signer, String permissionType) {
		String   homePath = System.getProperty("user.home") + "/cert/";
		if (new File(homePath).exists() == false)
			new File(homePath).mkdir();
		
		String command = "ndnsec-certgen -N " + idPrefix + "/" + tenantName + "/" + userName + "/" + permissionType +
									   " -s " + idPrefix + "/" + signer + " " +
									   homePath + certFileName + " > " + homePath + tenantName + "_" + userName + "_" + permissionType + ".cert";
		System.out.println(command);
		String[] args = new String[] { "/bin/bash", "-c", command };
		
		try {
			Process proc = new ProcessBuilder(args).start();
			return waitProcessUntilDone(proc, true);
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean installCertificate(String certFileName) {
		
		String   homePath = System.getProperty("user.home") + "/cert/";
		if (new File(homePath).exists() == false)
			new File(homePath).mkdir();
		
		String   command = "cat " + homePath + certFileName + " | ndnsec-cert-install -NI -";
		System.out.println(command);
		String[] args    = new String[] { "/bin/bash", "-c", command };
		
		try {
			Process proc = new ProcessBuilder(args).start();
			return waitProcessUntilDone(proc, true);
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean removeUser(String tenantName, String userName){

		String   command = "ndnsec-delete -n "+idPrefix+"/"+tenantName+"/"+userName;
		String[] args    = new String[] {"/bin/bash", "-c", command};
		
		try {
			Process proc = new ProcessBuilder(args).start();
			return waitProcessUntilDone(proc, true);

		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

		
	private boolean waitProcessUntilDone(Process proc, boolean printResult) {
				
		try {
			
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			StringBuffer   start    = new StringBuffer();
			
			// read the output from the command
			String line = null;
			while ((line = stdInput.readLine()) != null) {
				start.append(line);
				if (printResult)
					System.out.println(line.toString());
			}
			stdInput.close();
			
			// read any errors from the attempted command
			while ((line = stdError.readLine()) != null) {
				start.append(line);
				if (printResult)
					System.err.println(line);
			}
			stdError.close();
			
			int exitValue = proc.waitFor();
			System.out.println("\n\nExit Value " + exitValue);
			return (exitValue >= 0);
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
}
