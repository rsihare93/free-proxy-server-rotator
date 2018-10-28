package com.edidat.module.ProxyRotator.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
	private static final long waitTimeInMilliSeconds = 3000;
	private Socket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private String host;
	private int port;
	private static ProxyFetcherClient singletonObject;
	private static Object lock = new Object();
	private Object proxyRequestLock = new Object();
	private NetworkProxy networkProxy = null;
	private ExecutorService executorService = Executors.newFixedThreadPool(1);
	private Future<?> future;

	private ProxyFetcherClient(String host, int port) {
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

	public synchronized Optional<NetworkProxy> getProxyAddress(Protocol protocol) {
		if (socket == null) {
			try {
				initSocket();
			} catch (ProxyRotatorException e) {
				logger.error("Error occured while initialising the socket", e);
				return Optional.empty();
			}
		}
		return sendProxyRequest(protocol);
	}

	private void initSocket() throws ProxyRotatorException {
		int numberOfRetries = 3;
		while (socket == null) {
			logger.info("Trying to Init socket.");
			try {
				socket = new Socket(host, port);
				if (future != null) {
					future.cancel(true);
					future = null;
				}
				outputStream = socket.getOutputStream();
				inputStream = socket.getInputStream();

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
				logger.error("Error occured while initialiseing the socket.", e);
				numberOfRetries--;
				if (numberOfRetries > 0) {
					continue;
				}
				throw new ProxyRotatorException("Error occured while connecting to server.", e);
			}
		}
	}

	@SuppressWarnings("resource")
	private Optional<NetworkProxy> sendProxyRequest(Protocol protocol) {
		Optional<NetworkProxy> toReturn = Optional.empty();
		boolean done = false;
		int j = 3;
		try {
			while (!done && j > 0) {
				try {
					JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream));
					jsonWriter.jsonValue(new Gson().toJson(new ClientRequestMessage(RequestType.GET, protocol),
							ClientRequestMessage.class));
					jsonWriter.flush();
					done = true;
					synchronized (proxyRequestLock) {
						proxyRequestLock.wait(waitTimeInMilliSeconds);
					}
					return networkProxy != null ? Optional.of(networkProxy) : toReturn;
				} catch (IOException e) {
					logger.warn("Error occured while requesting proxy.. retring...", e);
					try {
						socket = null;
						initSocket();
						j--;
						continue;
					} catch (ProxyRotatorException e1) {
						return toReturn;
					}
				} catch (InterruptedException e) {
					logger.warn("Error occured while requesting proxy", e);
					return networkProxy != null ? Optional.of(networkProxy) : toReturn;
				}
			}
		} catch (Exception e) {
			logger.warn("Error occured while requesting proxy", e);
			return networkProxy != null ? Optional.of(networkProxy) : toReturn;

		}
		return networkProxy != null ? Optional.of(networkProxy) : toReturn;
	}

	protected void handleServerResponse(Socket serverSocket) {
		try {
			if (serverSocket == null) {
				return;
			}
			logger.info("Reading Server {}", serverSocket.getInetAddress());
			serverSocket.setKeepAlive(true);
			serverSocket.setSoTimeout(0);
			Gson gson = new Gson();
			boolean shutdown = false;
			while (!shutdown && !Thread.currentThread().isInterrupted()) {
				try {
					logger.info("Trying to Read meassage");
					ServerResponseMessage response = gson.fromJson(new JsonReader(new InputStreamReader(inputStream)),
							ServerResponseMessage.class);
					logger.info("Meassage Reading completed");
					if (response == null) {
						logger.info("Recived EOS Response, closing thread.");
						break;
					}
					switch (response.getRequestType()) {
					/* GET request type is considered as the request for the new proxy server */
					case GET:
						networkProxy = response.getNetworkProxy();
						synchronized (proxyRequestLock) {
							proxyRequestLock.notifyAll();
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
					logger.info("Completed reading.");
				} catch (IOException e) {
					logger.warn("Error while reading client", e);
					shutdown = true;
				} catch (Exception e) {
					logger.warn("Error while reading client", e);
				}
			}
		} catch (IOException e) {
			logger.warn("Exception occured while reading server{} ", serverSocket.getInetAddress(), e);
		} finally {
			logger.warn("Finally closing the socket");
			try {
				inputStream.close();
				outputStream.close();
				serverSocket.close();
			} catch (Exception e) {
				logger.error("Error while closing server socket", e);
			}
		}
	}

	public Boolean isSocketConnected() {
		if (socket == null) {
			return false;
		}
		try {
			@SuppressWarnings("resource")
			JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream));
			jsonWriter.jsonValue(new Gson().toJson(new ClientRequestMessage(RequestType.HEARTBEAT, null),
					ClientRequestMessage.class));
			jsonWriter.flush();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/*public static void main(String[] args) throws InterruptedException {
		@SuppressWarnings({ "resource", "unused" })
		ApplicationContext context = new ClassPathXmlApplicationContext("config/spring.xml");
		ProxyFetcherClient instance = ProxyFetcherClient.getInstance("localhost", 5656);
		System.out.println(instance.getProxyAddress(Protocol.HTTP));
		System.out.println(instance.getProxyAddress(Protocol.HTTPS));
	}*/
}
