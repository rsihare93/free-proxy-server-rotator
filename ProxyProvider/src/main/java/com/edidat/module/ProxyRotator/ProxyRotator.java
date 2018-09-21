package com.edidat.module.ProxyRotator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.edidat.module.ProxyRotator.pojo.NetworkProxy;

public class ProxyRotator {

	private static final Logger logger = LogManager.getLogger(ProxyRotator.class);

	private static BlockingQueue<NetworkProxy> proxyQueue = new LinkedBlockingQueue<>();

	private static final String ipPortRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+$";

	private static void putToQueue(NetworkProxy networkProxy) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					try {
						Document document = Jsoup.connect("https://www.google.com/")
								.proxy(networkProxy.getIpAddress(), networkProxy.getPort()).get();
						if (document == null) {
							throw new Exception("Null document returned.");
						}
						proxyQueue.put(networkProxy);
					} catch (Exception e) {
						logger.warn("Proxy {} is not rechable/ not working. : {}", networkProxy, e.getMessage());
					}
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}).start();
	}

	public static NetworkProxy removeInsert() throws InterruptedException, IOException {
		NetworkProxy networkProxy = null;
		networkProxy = proxyQueue.poll();
		if (networkProxy == null) {
			return null;
		}
		putToQueue(networkProxy);
		return networkProxy;
	}

	public static NetworkProxy getNextProxy() {
		NetworkProxy networkProxy = null;
		try {
			networkProxy = removeInsert();
			if (networkProxy == null) {
				return null;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return networkProxy;
	}

	private static void loadQueueFromFile(String filePath) {
		RandomAccessFile read = null;
		try {

			File file = new File(filePath);
			read = new RandomAccessFile(file, "rw");
			String line = "";
			while ((line = read.readLine()) != null) {
				logger.info("Read proxy from file {}", line);
				String[] ipPortPair = line.split(":");
				InetAddress addr = InetAddress.getByName(ipPortPair[0]);
				if (addr.isReachable(2000)) {
					try {
						proxyQueue.add(new NetworkProxy(ipPortPair[0], ipPortPair[1], Integer.parseInt(ipPortPair[1]),
								ipPortPair[2]));
					} catch (IllegalStateException e) {
						logger.warn("Queue is already full");
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			if (read != null) {
				try {
					read.close();
				} catch (IOException e) {

				}
			}
		}
	}
}
