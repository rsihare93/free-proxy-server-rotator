package com.edidat.module.ProxyRotator.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.helper.StringUtil;

import com.edidat.module.ProxyRotator.Constants;
import com.edidat.module.ProxyRotator.Protocol;
import com.edidat.module.ProxyRotator.ProxyRotator;
import com.edidat.module.ProxyRotator.ProxyRotatorException;

/**
 * @author rohit.ihare {@link ProxyProviderServer} is the server application
 *         which provides the free proxies available across Internet with the
 *         help of free-APIs and web scrapers. It servers the client request
 *         over tcp connection and communicates using json.
 */
public class ProxyProviderServer {
	public static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
	private static final Logger logger = LogManager.getLogger(ProxyProviderServer.class);

	public static void main(String[] args) {
		try {
			// starts the server.
			start();
		} catch (ProxyRotatorException e) {
			logger.fatal(e.getMessage());
			System.exit(-1);
		}
	}

	/**
	 * Init the proxy queue and start filling it with extracted proxies Also starts
	 * the tcp server to serve client requests.
	 * 
	 * @throws Exception
	 */
	public static void start() throws ProxyRotatorException {
		logger.info("Starting server ...");
		ProxyRotator.getInstance().init(new HashSet<Protocol>(Arrays.asList(new Protocol [] {Protocol.HTTPS})));
		clientRequestServer();
	}

	/**
	 * It starts the tcp server and accepts proxy requests from client in json
	 * format.
	 * 
	 * @throws ProxyRotatorException
	 */
	public static void clientRequestServer() throws ProxyRotatorException {
		String port = System.getenv(Constants.PROXY_PROVIDER_SERVER_PORT);
		if (StringUtil.isBlank(port)) {
			throw new ProxyRotatorException("Server port is not defined");
		}
		ServerSocket serverSocket = null;
		boolean shutdown = false;
		try {
			/* TCP Server starts at specified port and starts accepting clients */
			serverSocket = new ServerSocket(Integer.parseInt(port));
		} catch (NumberFormatException | IOException e1) {
			logger.error(e1.getMessage());
			throw new ProxyRotatorException(e1.getMessage(), e1);
		}
		logger.info("Server has been started @ port {}!!!", port);
		try {
			while (!shutdown) {
				try {
					/*
					 * TCP Server accepts clients and create separate thread for handling clients
					 * requests
					 */
					Socket clientSocket = serverSocket.accept();
					logger.info("Client connection is accepted ..");
					threadPool.submit(new ClientConnectionHandler(clientSocket));
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new ProxyRotatorException(e.getMessage(), e);
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				throw new ProxyRotatorException(e.getMessage(), e);
			}
		}
	}

}
