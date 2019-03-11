package com.ogb.fes.filesystem;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.policy.ConfigPolicyManager;


public class FileManager {
	
	public static String UPLOAD_DIR = "upload-dir";
	public static String CONFIG_DIR = "config-dir";
	
	public static KeyChain keyChain = null;
	
	
	/*** 
	 * Upload file helper functions
	***/
	
    public static File[] getUploadedFileList() {  	
		
    	return new File(UPLOAD_DIR).listFiles(new FileFilter() {	
			
			@Override
			public boolean accept(File pathName) {
				return true;
			}
		});
	}
    
    public static void saveFileToUploadDir(String fileName, MultipartFile file) throws IOException {
    	
    	BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(UPLOAD_DIR + "/" + fileName)));
		FileCopyUtils.copy(file.getInputStream(), stream);
		stream.flush();
		stream.close();
    }
    
    public static String getUploadFileContent(String fileName)  {
		
		byte bytes[];
		
		if (fileName.contains(UPLOAD_DIR) == true)
			bytes = new FileReader(fileName).getAllBytes();
		else
			bytes = new FileReader(UPLOAD_DIR+"/"+fileName).getAllBytes();
			
		return new String(bytes);
	}
    
	public static JSONArray getUploadFileContentJSONArray(String fileName)  {
			
		return new JSONArray(getUploadFileContent(fileName));
	}
	
	public static JSONObject getUploadFileContentJSONObjects(String fileName)  {
		
		return new JSONObject(getUploadFileContent(fileName));
	}
	
	public static void createUploadDir() {
		
		if (new File(UPLOAD_DIR).exists() == false)
			new File(UPLOAD_DIR).mkdir();
	}
	
	
	
	/*** 
	 * Config file helper functions
	***/
	
	public static File[] getConfigFileList() { 
		final HashSet<String> extensions = new HashSet<String>(Arrays.asList("config", "cfg", "conf"));
		
		return new File(CONFIG_DIR).listFiles(new FileFilter() {	
			@Override
			public boolean accept(File pathname) {
				String fileExt = FilenameUtils.getExtension(pathname.getName());
				return extensions.contains(fileExt); 
			}
		});
	}
	   
	public static KeyChain getKeyChainFromConfigFileName()  {
		
		if (keyChain != null)
			return keyChain;
		
		String fileName = CONFIG_DIR+"/validator-config.conf";

		try {
			ConfigPolicyManager policyManager   = new ConfigPolicyManager(fileName);
			IdentityManager     identityManager = new IdentityManager();
			keyChain = new KeyChain(identityManager, policyManager);
			return keyChain;
		} 
		catch (IOException | SecurityException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void createConfigDir() {
		
		if (new File(CONFIG_DIR).exists() == false)
			new File(CONFIG_DIR).mkdir();
	}
	
	public static void createDefaultConfigFile() {
		String fileName = CONFIG_DIR + "/validator-config.conf.sample";
		
		try {
			Resource resource = new ClassPathResource("validator-config.conf.sample");
	    	BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			FileCopyUtils.copy(resource.getInputStream(), stream);
			stream.flush();
			stream.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean checkConfigFile() {
		return new File(CONFIG_DIR + "/validator-config.conf").exists();
	}
	
	public static String waitForResFile(String fileName) throws InterruptedException {
		
		while (new File(FileManager.UPLOAD_DIR+"/"+fileName+".res").exists() == false)
			Thread.sleep(5);
		
		String result = new FileReader(FileManager.UPLOAD_DIR+"/"+fileName+".res").getAllLinesConcat();
		
		new File(FileManager.UPLOAD_DIR+"/"+fileName+".res").delete();
		return result;
	}
	

	
	/*** 
	 * Generic file helper functions
	***/
	
    public static byte[] getFileContent(String fileName) {
    	return new FileReader(fileName).getAllBytes();
    }
}
