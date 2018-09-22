package com.edidat.module.ProxyRotator.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.helper.StringUtil;

import com.edidat.module.ProxyRotator.Constants;

/**
 * Hello world!
 *
 */
public class ProxyProviderServer 
{
	public static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
    public static void main( String[] args )
    {
       
    }
    
    public static void start() {
    	
    }

    
    public static void clientConnectionHandler () throws Exception {
    	String port = System.getenv(Constants.PROXY_PROVIDER_SERVER_PORT);
    	if(StringUtil.isBlank(port)) {
    		throw new Exception("Server port is not defined");
    	}
    	ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
    	threadPool.submit(new ClientConnectionHandler(serverSocket.accept()));
    	
    }
    
}
