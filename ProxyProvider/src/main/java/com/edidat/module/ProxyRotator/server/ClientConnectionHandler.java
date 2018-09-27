package com.edidat.module.ProxyRotator.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.edidat.module.ProxyRotator.pojo.ProxyRequest;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class ClientConnectionHandler implements Runnable {

	private static final Logger logger = LogManager.getLogger(ClientConnectionHandler.class);

	private Socket clientSocket;

	public ClientConnectionHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			logger.info("Serving Client {}", clientSocket.getInetAddress());
			clientSocket.setKeepAlive(true);
			clientSocket.setSoTimeout(15000);
			InputStream inputStream = clientSocket.getInputStream();

			boolean shutdown = false;
			while (!shutdown) {
				try {

					JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));
					Gson gson = new Gson();
					ProxyRequest request = gson.fromJson(jsonReader, ProxyRequest.class);

					switch (request.getRequestType()) {
					case GET:
							
						break;
					case HEARTBEAT:

						break;
					case EXIT:

						break;

					default:
						break;
					}

				} catch (Exception e) {
					logger.warn("Error while reading client {}", e.getMessage());
					shutdown = true;
				}
			}

			// TO DO : handle client request and responses.
		} catch (IOException e) {
			logger.warn("Exception occured while serving client {} ", clientSocket.getInetAddress(), e.getMessage());
		}
	}

	private boolean isHeartBeat(String line) {
		return line.trim().equals("msg : {HEARTBEAT}");
	}

}
