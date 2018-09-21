package com.edidat.module.ProxyRotator.providers;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import com.edidat.module.ProxyRotator.Protocol;
import com.edidat.module.ProxyRotator.ProxyRotator;
import com.edidat.module.ProxyRotator.pojo.NetworkProxy;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProxyProvidersInJsonFormat  extends ProxyProvider{

	private static final Logger logger = LogManager.getLogger(ProxyProvidersInJsonFormat.class);
	private static final String ipPortRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+$";
	private String jsonIPKey;
	private String jsonPORTKey;

	protected ProxyProvidersInJsonFormat(String providerName, String httpsProxyApiURL, String httpProxyApiURL, Integer quotaPerTimeUnit,
			Integer timeUnitInMinutes, Integer remainingQuota, BlockingQueue<NetworkProxy> proxyQueue, String jsonIPKey,
			String jsonPORTKey) {
		super(providerName, httpsProxyApiURL, httpProxyApiURL, quotaPerTimeUnit, timeUnitInMinutes, proxyQueue);
		this.jsonIPKey = jsonIPKey;
		this.jsonPORTKey = jsonPORTKey;
	}

	@Override
	public void extractAndLoad(Protocol protocol) {

		Integer noOfProxiesToLoadPerMinute = getNOOfProxiesToLoadPerMinute();
		String proxyServerApi = protocol.equals(Protocol.HTTP)? getHttpProxyApiURL() : getHttpsProxyApiURL();
		logger.warn("Proxy API loading : {}", proxyServerApi);

		for (int i = 0; i < noOfProxiesToLoadPerMinute; i++) {
			String ipPort = "";
			try {
				ipPort = Jsoup.connect(proxyServerApi).ignoreContentType(true).get().text();
				JsonParser parser = new JsonParser();
				try {
					JsonObject object = parser.parse(ipPort).getAsJsonObject();
					ipPort = object.get(jsonIPKey).getAsString() + ":" + object.get(jsonPORTKey).getAsString();
				} catch (Exception e) {
					logger.error(e.getMessage());				
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			logger.warn("{} :: Proxy API response : {}", proxyServerApi, ipPort);

			if (ipPort.matches(ipPortRegex)) {
				String[] ipPortPair = ipPort.split(":");
				ProxyRotator.getInstance().putToQueue(new NetworkProxy(protocol, ipPortPair[0], Integer.parseInt(ipPortPair[1]), this.getProviderName()));
			} else {
				logger.warn("{} :: Error while retriving proxy server {}. Sleeping thread for 2 hours",
						proxyServerApi, ipPort);
				;
				Thread.sleep(7200000);
			}
			Thread.sleep(2000);
		}
		if (i == size - 1) {
			logger.warn("{} :: Size is reached. Thread is sleeping for 2 minute.", proxyServerApi);
			logger.info("Proxy queue size : {}", proxyQueue.size());
			i = 0;
			Thread.sleep(120000);
		}
	}



}

}
