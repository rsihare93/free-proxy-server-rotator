package com.edidat.module.ProxyRotator.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientConnectionHandler implements Runnable {

	private static final Logger logger = LogManager.getLogger(ClientConnectionHandler.class);

	private Socket clientSocket;

	public ClientConnectionHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			logger.info("Serving Client {}",clientSocket.getInetAddress());
			clientSocket.setKeepAlive(true);
			clientSocket.getInputStream();
			// TO DO : handle client request and responses.
		} catch (IOException e) {
			logger.warn("Exception occured while serving client {} ", clientSocket.getInetAddress(), e.getMessage());
		}
	}

}
