package com.edidat.module.ProxyRotator.providers;

public abstract class ProxyProvider {

	private String httpsProxyApiURL;
	private String quotaPerTimeUnit;
	private String timeUnitInMinutes;
	
	
	public abstract void extractAndLoad();


	public String getHttpsProxyApiURL() {
		return httpsProxyApiURL;
	}


	public void setHttpsProxyApiURL(String httpsProxyApiURL) {
		this.httpsProxyApiURL = httpsProxyApiURL;
	}


	public String getQuotaPerTimeUnit() {
		return quotaPerTimeUnit;
	}


	public void setQuotaPerTimeUnit(String quotaPerTimeUnit) {
		this.quotaPerTimeUnit = quotaPerTimeUnit;
	}


	public String getTimeUnitInMinutes() {
		return timeUnitInMinutes;
	}


	public void setTimeUnitInMinutes(String timeUnitInMinutes) {
		this.timeUnitInMinutes = timeUnitInMinutes;
	}
	
}
