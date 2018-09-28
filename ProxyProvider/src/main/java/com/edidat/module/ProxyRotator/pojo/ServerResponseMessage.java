package com.edidat.module.ProxyRotator.pojo;

import com.edidat.module.ProxyRotator.RequestType;

public class ServerResponseMessage {
	
	private RequestType requestType;
	private NetworkProxy networkProxy;

	
	public RequestType getRequestType() {
		return requestType;
	}

	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	public NetworkProxy getNetworkProxy() {
		return networkProxy;
	}

	public void setNetworkProxy(NetworkProxy networkProxy) {
		this.networkProxy = networkProxy;
	}

	public ServerResponseMessage(RequestType requestType, NetworkProxy networkProxy) {
		super();
		this.requestType = requestType;
		this.networkProxy = networkProxy;
	}
	
	

}
