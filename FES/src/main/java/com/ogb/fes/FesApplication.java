package com.ogb.fes;


import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.ndn.NDNQueryRepoMapManager;
import com.ogb.fes.ndn.query.NDNFetchManager;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.utils.DateTime;





@SpringBootApplication
public class FesApplication {
	
	@Autowired
	public Environment env;
	
		
    public static void main(String[] args) 
    {
        SpringApplication.run(FesApplication.class, args);
    }
    
	@Bean CommandLineRunner init() 
	{
        return (String[] args) -> {
        	NetManager.AUC_URL       = env.getProperty("fes.auc.url");
        	NetManager.FES_IP        = env.getProperty("fes.fes.ip");
        	NetManager.FES_PORT		 = env.getProperty("server-http.port");
        	NDNFetchManager.serverIP = env.getProperty("fes.nfd.ip");
        	
            System.out.println(DateTime.currentTime()+"AUC Server URL: " + NetManager.AUC_URL);
            System.out.println(DateTime.currentTime()+"FES Server URL: " + NetManager.FES_IP +":"+NetManager.FES_PORT);
            System.out.println(DateTime.currentTime()+"NDN Fetch Server IP: " + NDNFetchManager.serverIP);
            
            FileManager.createUploadDir();
            FileManager.createConfigDir();
            //FileManager.createDefaultConfigFile();
            NDNQueryRepoMapManager prefixUpdater = new NDNQueryRepoMapManager();
            prefixUpdater.startNDNPrefixUpdater();
            
            if (FileManager.checkConfigFile() == false) {
            	throw new Exception("Unable to find config file!");
            }
        };
    }
	
	
	
	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
		tomcat.addAdditionalTomcatConnectors(createStandardConnector());
		return tomcat;
	}

	private Connector createStandardConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(Integer.parseInt(env.getProperty("server-http.port")));	
		return connector;
	}
}
 