package com.edidat.module.ProxyRotator.providers;

import org.apache.logging.log4j.Logger;

import com.edidat.module.ProxyRotator.Protocol;



public abstract class ProxyProvider implements Runnable , Cloneable{

	private static final Integer MIN_PROXIES_TO_LOAD = 4;
	private String providerName;
	private String proxyApiURL;
	private Integer quotaPerTimeUnit;
	private Integer timeUnitInMinutes;
	private Integer remainingQuota;
	private Protocol requiredProtocol;
	
	protected Logger logger;
	
	public ProxyProvider(String providerName, String proxyApiURL, Integer quotaPerTimeUnit,
			Integer timeUnitInMinutes, Protocol protocol) {
		super();
		this.providerName = providerName;
		this.proxyApiURL = proxyApiURL;
		this.quotaPerTimeUnit = quotaPerTimeUnit;
		this.timeUnitInMinutes = timeUnitInMinutes;
		this.remainingQuota = quotaPerTimeUnit;
		this.requiredProtocol = protocol;
	}
	
	@Override
	public void run() {
		try {
			extractAndLoad(this.requiredProtocol);
		} catch (InterruptedException e) {
			logger.error(e);
		}
	}

	public abstract void extractAndLoad(Protocol protocol) throws InterruptedException;
	
	
	
	public void resetQuota() {
		this.remainingQuota = quotaPerTimeUnit;
	}
	
	public Protocol getRequiredProtocol() {
		return requiredProtocol;
	}

	public void setRequiredProtocol(Protocol requiredProtocol) {
		this.requiredProtocol = requiredProtocol;
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

	public void reduceRemainingQuota(int quotaUsage) {
		int remaining = this.remainingQuota - quotaUsage;
		this.remainingQuota =  remaining < 0 ? 0 : remaining;
	}

	public Integer getNOOfProxiesToLoadPerMinute() {
		int minProxyToLoadAtATime = Math.min(MIN_PROXIES_TO_LOAD, remainingQuota);
		return minProxyToLoadAtATime;
	}
	
	public String getProxyApiURL() {
		return proxyApiURL;
	}

	public void setProxyApiURL(String proxyApiURL) {
		this.proxyApiURL = proxyApiURL;
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

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
}
