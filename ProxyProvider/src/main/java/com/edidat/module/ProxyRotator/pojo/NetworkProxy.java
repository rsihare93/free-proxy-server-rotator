package com.edidat.module.ProxyRotator.pojo;

public class NetworkProxy {

	String protocol;
	String ipAddress;
	int port;
	String origin;
	
	public NetworkProxy(String protocol, String ipAddress, int port, String origin) {
		super();
		this.protocol = protocol;
		this.ipAddress = ipAddress;
		this.port = port;
		this.origin = origin;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "NetworkProxy [protocol=" + protocol + ", ipAddress=" + ipAddress + ", port=" + port + "]";
	}

}
