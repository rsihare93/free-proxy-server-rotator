package com.edidat.module.ProxyRotator;

public enum RequestType {

	GET("GET"), EXIT("EXIT"), HEARTBEAT("HEARTBEAT"), HEARTBEAT_ACK("HEARTBEAT_ACK");
	String name;

	private RequestType(String name) {
		this.name = name;
	}

}
