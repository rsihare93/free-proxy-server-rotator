package com.edidat.module.ProxyRotator.pojo;

import com.edidat.module.ProxyRotator.Protocol;

public class NetworkProxy {

	Protocol protocol;
	String ipAddress;
	int port;
	String origin;
	
	public NetworkProxy(Protocol protocol, String ipAddress, int port, String origin) {
		super();
		this.protocol = protocol;
		this.ipAddress = ipAddress;
		this.port = port;
		this.origin = origin;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
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

	
	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	@Override
	public String toString() {
		return "NetworkProxy [protocol=" + protocol + ", ipAddress=" + ipAddress + ", port=" + port + "]";
	}

}
