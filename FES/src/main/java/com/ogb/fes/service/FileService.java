package com.ogb.fes.service;


import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ogb.fes.domain.ErrorResponse;
import com.ogb.fes.domain.User;
import com.ogb.fes.domain.UserRepository;
import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;
import com.ogb.fes.utils.Utils;


@RestController
public class FileService {

	@Autowired
	private UserRepository userRepo;
	
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.POST, value="/OGB/files/upload", produces="application/json")
    public void insertFileContent(HttpServletResponse response, @RequestParam("file") MultipartFile file,  @RequestHeader(value="Authorization", defaultValue="") String authToken, @RequestHeader(value="Accept-Encoding", defaultValue="") String contentHeader) throws IOException {
		
		OutputStream out = response.getOutputStream();
		if (contentHeader != null && contentHeader.length() > 0 && contentHeader.contains("gzip")) {
			response.addHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(response.getOutputStream());
		}
		
		User user = checkAuthToken(authToken);
		if (user == null) {
			response.setStatus(420);
			ErrorResponse error = new ErrorResponse(420, "Invalid authorization token");
			out.write(error.toString().getBytes());
			out.flush();
			out.close();
			return;
		}
		
		String name = authToken;
		if (file.isEmpty() == true) {
			System.out.println(DateTime.currentTime()+"ContentService - Failed to upload file <" + name  + "> because the file was empty!");
			response.setStatus(407);
			ErrorResponse error = new ErrorResponse(407, "Error on upload file! Empty file!");
			out.write(error.toString().getBytes());
			out.flush();
			out.close();
			return;
		}
		
		// Write bytes from the multipart file to disk.
		String fileName = Utils.generateRandomName(20);
		String filePath = FileManager.UPLOAD_DIR + "/" + fileName + ".jpg";
		FileUtils.writeByteArrayToFile(new File(filePath), file.getBytes());	
		
		
		// Create a thumbnail for fast download
		String thumbFilePath = FileManager.UPLOAD_DIR + "/" + "thumb_" + fileName + ".jpg";
	    BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
	    img.createGraphics().drawImage(ImageIO.read(new File(filePath)).getScaledInstance(150, 150, Image.SCALE_SMOOTH),0,0,null);
	    ImageIO.write(img, "jpg", new File(thumbFilePath));
	    
		response.setStatus(200);
		String message = "{\"message\":\"File Upload Success!\", \"uri\" : \"OGB/files/jpg/download/" + fileName + "\", \"thumb_uri\" : \"OGB/files/download/"+ thumbFilePath+"\"}";
		out.write(message.getBytes());
		out.flush();
		out.close();
		return;
    }

	
	@RequestMapping(method = RequestMethod.GET, value = "/OGB/files/list")
	public String getUploadFilesList(Model model) {
		
		File rootFolder = new File(FileManager.UPLOAD_DIR);
		model.addAttribute("files", Arrays.stream(rootFolder.listFiles()).sorted(Comparator.comparingLong(f -> -1 * f.lastModified())).map(f -> f.getName()).collect(Collectors.toList()));

		return model.toString();
	}
	
	@CrossOrigin(origins="*")
	@RequestMapping(method = RequestMethod.GET, value="/OGB/files/{fileExtension}/download/{filename}")
    public void downloadFile(HttpServletResponse response, @PathVariable String fileExtension, @PathVariable String filename) throws IOException {
		
		filename = filename + "." +fileExtension;
		
		String filePath = FileManager.UPLOAD_DIR + "/" + filename;
		System.out.println(DateTime.currentTime()+"ContentService - File Requested: " + filePath);
		
		FileSystemResource fileRes = new FileSystemResource(new File(filePath));
		InputStream        input   = fileRes.getInputStream();
		
		int    bytesRead;
		byte[] buffer = new byte[1024];
		
	    while ((bytesRead = input.read(buffer)) != -1) {
	        response.getOutputStream().write(buffer, 0, bytesRead);
	    }
	    response.flushBuffer();
	    return;
    }
	
	

	
	private User checkAuthToken(String authToken) {
		
		if (authToken.length() <= 0)
			return null;
		
		User user = userRepo.findByToken(authToken);
		if (user == null)
			user = checkTokenOnAUCServer(authToken);
		
		return user;
	}
	
	private User checkTokenOnAUCServer(String token) {
		
		Map<String, Object> postParams = new HashMap<String, Object>();
		postParams.put("token", token);
		
		Map<String, Object> aucUser = new NetManager().sendCheckToken(postParams);
		if (aucUser == null)
			return null;
		
		return new User(aucUser);
	} 
}
