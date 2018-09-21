package com.edidat.module.ProxyRotator.providers;

import java.util.concurrent.BlockingQueue;

import com.edidat.module.ProxyRotator.Protocol;
import com.edidat.module.ProxyRotator.pojo.NetworkProxy;



public abstract class ProxyProvider {

	private static final Integer MIN_PROXIES_TO_LOAD = 4;
	private String providerName;
	private String httpsProxyApiURL;
	private String httpProxyApiURL;
	private Integer quotaPerTimeUnit;
	private Integer timeUnitInMinutes;
	private Integer remainingQuota;
	private BlockingQueue<NetworkProxy> proxyQueue;
	
	public ProxyProvider(String providerName, String httpsProxyApiURL, String httpProxyApiURL, Integer quotaPerTimeUnit,
			Integer timeUnitInMinutes, BlockingQueue<NetworkProxy> proxyQueue) {
		super();
		this.providerName = providerName;
		this.httpsProxyApiURL = httpsProxyApiURL;
		this.httpProxyApiURL = httpProxyApiURL;
		this.quotaPerTimeUnit = quotaPerTimeUnit;
		this.timeUnitInMinutes = timeUnitInMinutes;
		this.remainingQuota = quotaPerTimeUnit;
		this.proxyQueue = proxyQueue;
	}

	public abstract void extractAndLoad(Protocol protocol);
	
	public BlockingQueue<NetworkProxy> getProxyQueue() {
		return proxyQueue;
	}

	public void setProxyQueue(BlockingQueue<NetworkProxy> proxyQueue) {
		this.proxyQueue = proxyQueue;
	}

	
	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public Integer getRemainingQuota() {
		return remainingQuota;
	}

	public void setRemainingQuota(Integer remainingQuota) {
		this.remainingQuota = remainingQuota;
	}

	public Integer getNOOfProxiesToLoadPerMinute() {
		int minProxyToLoadAtATime = Math.min(MIN_PROXIES_TO_LOAD, remainingQuota);
	}
	
	public String getHttpProxyApiURL() {
		return httpProxyApiURL;
	}

	public void setHttpProxyApiURL(String httpProxyApiURL) {
		this.httpProxyApiURL = httpProxyApiURL;
	}

	public String getHttpsProxyApiURL() {
		return httpsProxyApiURL;
	}


	public void setHttpsProxyApiURL(String httpsProxyApiURL) {
		this.httpsProxyApiURL = httpsProxyApiURL;
	}


	public Integer getQuotaPerTimeUnit() {
		return quotaPerTimeUnit;
	}


	public void setQuotaPerTimeUnit(Integer quotaPerTimeUnit) {
		this.quotaPerTimeUnit = quotaPerTimeUnit;
	}


	public Integer getTimeUnitInMinutes() {
		return timeUnitInMinutes;
	}


	public void setTimeUnitInMinutes(Integer timeUnitInMinutes) {
		this.timeUnitInMinutes = timeUnitInMinutes;
	}
	
}
