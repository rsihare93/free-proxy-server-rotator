package com.edidat.module.ProxyRotator.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.edidat.module.ProxyRotator.Protocol;
import com.edidat.module.ProxyRotator.ProxyRotatorException;
import com.edidat.module.ProxyRotator.RequestType;
import com.edidat.module.ProxyRotator.pojo.ClientRequestMessage;
import com.edidat.module.ProxyRotator.pojo.NetworkProxy;
import com.edidat.module.ProxyRotator.pojo.ServerResponseMessage;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ProxyFetcherClient {

	private static final Logger logger = LogManager.getLogger(ProxyFetcherClient.class);
	private static final long waitTimeInMilliSeconds = 300000;
	private Socket socket;
	private String host;
	private int port;
	private static ProxyFetcherClient singletonObject;
	private static Object lock = new Object();
	private Object proxyRequestLock = new Object();
	private NetworkProxy networkProxy = null;
	private ExecutorService executorService = Executors.newFixedThreadPool(1);
	private Future<?> future;

	private ProxyFetcherClient(	String host, int port) {
		this.host = host;
		this.port = port;
	}

	public static ProxyFetcherClient getInstance(String host, int port) {
		ProxyFetcherClient r = singletonObject;
		if (r == null) {
			synchronized (lock) {
				r = singletonObject;
				if (r == null) {
					r = new ProxyFetcherClient(host, port);
					singletonObject = r;
				}
			}
		}
		return r;
	}

	private void initSocket() throws ProxyRotatorException {
		int numberOfRetries = 3;
		while (socket == null) {
			try {
				socket = new Socket(host, port);

				if (future != null) {
					future.cancel(true);
				}

				future = executorService.submit(new Runnable() {
					public void run() {
						handleServerResponse(socket);
					}
				});

			} catch (IllegalArgumentException e) {
				throw new ProxyRotatorException("Please configure proper host and port details.", e);
			} catch (SecurityException e) {
				throw new ProxyRotatorException("You are not allowed to perform this opration.", e);
			} catch (UnknownHostException e) {
				throw new ProxyRotatorException("Please configure proper host and port details.", e);
			} catch (IOException e) {
				logger.error("Error occured while initialiseing the socket. {}", e.getMessage());
				numberOfRetries--;
				if (numberOfRetries != 0) {
					continue;
				}
				throw new ProxyRotatorException("Error occured while connecting to server.", e);
			}
		}
	}

	protected void handleServerResponse(Socket serverSocket) {
		try {
			if (serverSocket == null) {
				return;
			}
			logger.info("Reading Server {}", serverSocket.getInetAddress());
			serverSocket.setKeepAlive(true);
			serverSocket.setSoTimeout(0);
			InputStream inputStream = serverSocket.getInputStream();
			JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream));
			Gson gson = new Gson();
			boolean shutdown = false;
			while (!shutdown && !Thread.currentThread().isInterrupted()) {
				try {
					logger.info("Trying to Read meassage");
					ServerResponseMessage response = gson.fromJson(jsonReader, ServerResponseMessage.class);
					logger.info("Meassage Reading completed");
					if(response == null) {
						break;
					}
					switch (response.getRequestType()) {
					/* GET request type is considered as the request for the new proxy server */
					case GET:
						networkProxy = response.getNetworkProxy();
						synchronized (proxyRequestLock) {
							proxyRequestLock.notify();
						}
						break;
						/*
						 * If client receives HEARTBEAT message from Server, client need to response as
						 * Ack.
						 */
					case HEARTBEAT:
						logger.info("Received Hearbeat");
						ServerResponseMessage message = new ServerResponseMessage(RequestType.HEARTBEAT_ACK, null);
						JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
						jsonWriter.jsonValue(new Gson().toJson(message));
						jsonWriter.flush();
						logger.info("Sent Hearbeat Ack");
						break;
					default:
						break;
					}

				} catch (IOException e) {
					logger.warn("Error while reading client {}", e.getMessage());
					shutdown = true;
				}
			}
		} catch (IOException e) {
			logger.warn("Exception occured while reading server{} ", serverSocket.getInetAddress(), e.getMessage());
		} finally {
			try {
				serverSocket.close();
			} catch (Exception e) {

			}
		}
	}

	public synchronized Optional<NetworkProxy> getProxyAddress(Protocol protocol) {
		if (socket == null) {
			try {
				initSocket();
			} catch (ProxyRotatorException e) {
				logger.error("Error occured while initialising the socket {}", e.getMessage());
				return Optional.empty();
			}
		}
		return sendProxyRequest(protocol);
	}

	private Optional<NetworkProxy> sendProxyRequest(Protocol protocol) {
		Optional<NetworkProxy> toReturn = Optional.empty();
		JsonWriter jsonWriter = null;
		boolean done = false;
		int j = 3;
		try {
			while (!done && j > 0) {
				try {
					jsonWriter = new JsonWriter(new OutputStreamWriter(socket.getOutputStream()));
					jsonWriter.jsonValue(new Gson().toJson(new ClientRequestMessage(RequestType.GET, protocol),
							ClientRequestMessage.class));
					jsonWriter.flush();
					done = true;
					synchronized (proxyRequestLock) {
						proxyRequestLock.wait(waitTimeInMilliSeconds);
					}
					return networkProxy != null ? Optional.of(networkProxy) : toReturn;
				} catch (IOException e) {
					logger.warn("Error occured while requesting proxy retiring... {}", e.getMessage());
					try {
						socket = null;
						initSocket();
						j--;
						continue;
					} catch (ProxyRotatorException e1) {
						return toReturn;
					}
				} catch (InterruptedException e) {
					logger.warn("Error occured while requesting proxy {}", e.getMessage());
					return networkProxy != null ? Optional.of(networkProxy) : toReturn;
				}
			}
		} catch (Exception e){
			logger.warn("Error occured while requesting proxy {}", e.getMessage());
			e.printStackTrace();
			return networkProxy != null ? Optional.of(networkProxy) : toReturn;
			
		}finally {
			if (jsonWriter != null) {
				try {
					jsonWriter.close();
				} catch (IOException e) {
				}
			}
		}
		return networkProxy != null ? Optional.of(networkProxy) : toReturn;
	}

	public Boolean isSocketConnected() {
		if (socket == null) {
			return false;
		}
		JsonWriter jsonWriter = null;
		try {
			jsonWriter = new JsonWriter(new OutputStreamWriter(socket.getOutputStream()));
			jsonWriter.jsonValue(new Gson().toJson(new ClientRequestMessage(RequestType.HEARTBEAT, null),
					ClientRequestMessage.class));
			jsonWriter.flush();
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (jsonWriter != null) {
				try {
					jsonWriter.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		ProxyFetcherClient instance = ProxyFetcherClient.getInstance("localhost", 5656);
		while (true) {
			System.out.println(instance.getProxyAddress(Protocol.HTTP));
			System.out.println(instance.getProxyAddress(Protocol.HTTP));
			System.out.println(instance.getProxyAddress(Protocol.HTTP));
			System.out.println(instance.getProxyAddress(Protocol.HTTPS));
			Thread.sleep(2000);
		}
	}
}
