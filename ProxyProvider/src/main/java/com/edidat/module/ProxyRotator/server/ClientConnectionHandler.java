package com.edidat.module.ProxyRotator.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.edidat.module.ProxyRotator.RequestType;
import com.edidat.module.ProxyRotator.pojo.ClientRequestMessage;
import com.edidat.module.ProxyRotator.pojo.NetworkProxy;
import com.edidat.module.ProxyRotator.pojo.ServerResponseMessage;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * @author rohit.ihare
 * ClientConnectionHandler handles the client request using separate threads and listens for the client requests.
 * This keeps TCP connection persistent until client disconnects itself. The handler keeps on listening the client unless client sends
 * "Exit" message or closes connection.
 * Handler keeps on sending heartbeat message and waits for ack for 15 seconds.  
 */
public class ClientConnectionHandler implements Runnable {

	private static final Logger logger = LogManager.getLogger(ClientConnectionHandler.class);

	private Socket clientSocket;
	private boolean shutdown = false;

	private static boolean hearBeatAck = false;
	private static Object hearBeatAckLock = new Object();

	public ClientConnectionHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			logger.info("Serving Client {}", clientSocket.getInetAddress());
			clientSocket.setKeepAlive(true);
			clientSocket.setSoTimeout(0);
			new Thread(new HearBeatSender(clientSocket)).start();
			InputStream inputStream = clientSocket.getInputStream();

			while (!shutdown) {
				try {
					JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));
					Gson gson = new Gson();
					ClientRequestMessage request = gson.fromJson(jsonReader, ClientRequestMessage.class);
					if(request == null) {
						shutdown = true;
						continue;
					}
					switch (request.getRequestType()) {
					/* GET request type is considered as the request for the new proxy server*/
					case GET:
						logger.info("Received Proxy request from {}", clientSocket.getInetAddress());
						NetworkProxy nextProxy = new NetworkProxy(request.getProtocol(),"ip", new Random().nextInt(),"ori");//ProxyRotator.getInstance().getNextProxy();
						ServerResponseMessage message = new ServerResponseMessage(RequestType.GET, nextProxy);
						JsonWriter jsonWriter = new JsonWriter( new OutputStreamWriter(clientSocket.getOutputStream()));
						jsonWriter.jsonValue(new Gson().toJson(message));
						jsonWriter.flush();
						break;
					/* HEARTBEAT request type is considered as the ACK to the heartbeat sent by server*/
					case HEARTBEAT_ACK:
						logger.info("Received ack from {}", clientSocket.getInetAddress());
						hearBeatAck = true;
						synchronized (hearBeatAckLock) {
							hearBeatAckLock.notify();
						}
						break;
					/* If server receives HEARTBEAT message from client, server need to ignore it as client may be testing the connectivity.*/
					case HEARTBEAT:
						// Do Nothing.
						break;
					/* Exit request type is considered as the signal from client to terminate the socket connection.*/	
					case EXIT:
						shutdown = true;
						break;

					default:
						break;
					}

				} catch (Exception e) {
					logger.warn("Error while reading client {}", e.getMessage());
					e.printStackTrace();
					shutdown = true;
				}
			}
		} catch (IOException e) {
			logger.warn("Exception occured while serving client {} ", clientSocket.getInetAddress(), e);
			shutdown = true;
		}
	}

	
	/**
	 * @author rohit.ihare
	 * Sends heartbeat to client to check if the client is reachable and waits for client ack.  
	 */
	class HearBeatSender implements Runnable {

		private Socket clientSocket;

		public HearBeatSender(Socket clientSocket) {
			super();
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			Gson gson = new Gson();
			String heartBeatmsg = gson.toJson(new ServerResponseMessage(RequestType.HEARTBEAT, null));
			try {
				boolean shut =false;
				while (!shut ) {
					logger.info("Sending hearbeat to : {}", clientSocket.getLocalAddress());
					hearBeatAck = false;
					OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream());
					outputStreamWriter.write(heartBeatmsg);
					outputStreamWriter.flush();
					synchronized (hearBeatAckLock) {
						hearBeatAckLock.wait(15000);
					}
					if (!hearBeatAck) {
						logger.warn("Did not receive ack from client. closing the handler thread.");
						shut =true;
						if(clientSocket != null && clientSocket.getChannel() != null) {
							clientSocket.getChannel().close();
						}
						break;
					} else {
					Thread.sleep(15000);
					}
				}
			} catch (IOException e) {
				logger.warn("Error ocurred while sending the heartbeat.", e);
			} catch (InterruptedException e) {
				logger.warn("Heartbeat thread got interupted", e);

			}

		}
	}
}
