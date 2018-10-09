package com.edidat.module.ProxyRotator.providers;

import java.net.SocketTimeoutException;
import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import com.edidat.module.ProxyRotator.Protocol;
import com.edidat.module.ProxyRotator.ProxyRotator;
import com.edidat.module.ProxyRotator.pojo.NetworkProxy;
import com.jayway.jsonpath.JsonPath;

public class ProxyProvidersInJsonFormat extends ProxyProvider {

	protected Logger logger = LogManager.getLogger(ProxyProvidersInJsonFormat.class);
	private static final String ipPortRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+$";
	private String jsonIPKey;
	private String jsonPORTKey;

	public ProxyProvidersInJsonFormat(String providerName, String proxyApiURL, Integer quotaPerTimeUnit,
			Integer timeUnitInMinutes, String jsonIPKey, String jsonPORTKey, Protocol protocol) {
		super(providerName, proxyApiURL, quotaPerTimeUnit, timeUnitInMinutes, protocol);
		this.jsonIPKey = jsonIPKey;
		this.jsonPORTKey = jsonPORTKey;
	}

	@Override
	public void extractAndLoad(Protocol protocol) throws InterruptedException {
		Thread.currentThread().setName(this.getProviderName() + Calendar.getInstance().getTimeInMillis());
		while (true) {
			Integer noOfProxiesToLoadPerMinute = getNOOfProxiesToLoadPerMinute();
			String proxyServerApi = getProxyApiURL();
			logger.warn("Proxy API loading : {}", proxyServerApi);

			inner: for (int i = 0; i < noOfProxiesToLoadPerMinute; i++) {
				String response = "";
				String ipPort = "";
				try {
					response = Jsoup.connect(proxyServerApi).timeout(60000).ignoreHttpErrors(true).ignoreContentType(true).get().text();
					if(response.isEmpty()) {
						i--;
						continue;
					}
					ipPort = JsonPath.parse(response).read(jsonIPKey).toString() + ":"
							+ (String) JsonPath.parse(response).read(jsonPORTKey).toString();
				} catch (SocketTimeoutException e) {
					logger.error(ipPort, e);
					i--;
					continue;
				} catch (Exception e) {
					logger.error(ipPort, e);
				}
				logger.warn("{} :: Proxy API response : {}", proxyServerApi, response);
				
				if (ipPort.matches(ipPortRegex)) {
					String[] ipPortPair = ipPort.split(":");
					ProxyRotator.getInstance().putToQueue(new NetworkProxy(protocol, ipPortPair[0],
							Integer.parseInt(ipPortPair[1]), this.getProviderName()));
					this.reduceRemainingQuota(1);
				} else {
					logger.warn("{} :: Error while retriving proxy server {}. Sleeping thread for {} minutes",
							proxyServerApi, response, this.getTimeUnitInMinutes());
					Thread.sleep(this.getTimeUnitInMinutes() * 60 * 1000);
					this.resetQuota();
					break inner;
				}
				Thread.sleep(15000);
			}
		}
	}

}
