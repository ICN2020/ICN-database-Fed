package com.ogb.auc;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.ogb.auc.ndnsec.NDNSecManager;


@SpringBootApplication
public class AucServiceApplication {
	
	@Autowired
	public Environment env;

	NDNSecManager ndnSecManager = null;
	
	public static void main(String[] args) {
		SpringApplication.run(AucServiceApplication.class, args);
	}
	
	@Bean CommandLineRunner init() 
    {
        return (String[] args) -> {
                       
        	ndnSecManager = NDNSecManager.getInstance();
        	
        	NDNSecManager.NDN_PATH = env.getProperty("auc.ndn.home");
        	NDNSecManager.NDN_MAP_FILE_PATH = NDNSecManager.NDN_PATH+"/.ndn/ndnsec-tpm-file/mapping.txt";
        	NDNSecManager.idPrefix = env.getProperty("auc.ndn.idPrefix");
        };
    }
}
