package com.edidat.module.ProxyRotator.providers;

import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.edidat.module.ProxyRotator.Protocol;
import com.edidat.module.ProxyRotator.ProxyRotator;
import com.edidat.module.ProxyRotator.pojo.NetworkProxy;
import com.edidat.module.ProxyRotator.util.WebExtractor;
import com.edidat.module.ProxyRotator.util.WebExtractor.CellData;

public class ProxyProvidersWebsites extends ProxyProvider {

	protected Logger logger = LogManager.getLogger(ProxyProvidersWebsites.class);
	private static final String ipPortRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+$";
	private String cssTableSelctor;
	private String ipColumnTitle;
	private String portColumnTitle;

	public ProxyProvidersWebsites(String providerName, String url, Integer quotaPerTimeUnit, Integer timeUnitInMinutes,
			Protocol protocol, String cssTableSelctor, String ipColumnTitle, String portColumnTitle) {
		super(providerName, url, quotaPerTimeUnit, timeUnitInMinutes, protocol);
		this.cssTableSelctor = cssTableSelctor;
		this.ipColumnTitle = ipColumnTitle;
		this.portColumnTitle = portColumnTitle;
	}

	public String getCssTableSelctor() {
		return cssTableSelctor;
	}

	public void setCssTableSelctor(String cssTableSelctor) {
		this.cssTableSelctor = cssTableSelctor;
	}

	public String getIpColumnTitle() {
		return ipColumnTitle;
	}

	public void setIpColumnTitle(String ipColumnTitle) {
		this.ipColumnTitle = ipColumnTitle;
	}

	public String getPortColumnTitle() {
		return portColumnTitle;
	}

	public void setPortColumnTitle(String portColumnTitle) {
		this.portColumnTitle = portColumnTitle;
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public void extractAndLoad(Protocol protocol) throws InterruptedException {
		Thread.currentThread().setName(this.getProviderName() + Calendar.getInstance().getTimeInMillis());
		while (true) {
			String proxyServerApi = getProxyApiURL();
			logger.warn("Proxy API loading : {}", proxyServerApi);
			Optional<Map<CellData, Map<CellData, CellData>>> proxies;
			try {
				proxies = WebExtractor.getInstance().extractTableData(getProxyApiURL(), getCssTableSelctor(), false,
						true, false);
				if (proxies.isPresent()) {
					Map<CellData, Map<CellData, CellData>> proxyMap = proxies.get();
					Set<Entry<CellData, Map<CellData, CellData>>> entrySet = proxyMap.entrySet();
					for (Entry<CellData, Map<CellData, CellData>> entry : entrySet) {
						String ipAddress = entry.getValue().get(getIpColumnTitle()).getValue();
						String ipPort = "";
						if (ipAddress.contains(":")) {
							ipPort = ipAddress;
						} else {
							ipPort = ipAddress + ":" + entry.getValue().get(getPortColumnTitle()).getValue();
						}
						if (ipPort.matches(ipPortRegex)) {
							String[] ipPortPair = ipPort.split(":");
							ProxyRotator.getInstance().putToQueue(new NetworkProxy(protocol, ipPortPair[0],
									Integer.parseInt(ipPortPair[1]), this.getProviderName()));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			Thread.sleep(getTimeUnitInMinutes() * 60000);
		}
	}
}
